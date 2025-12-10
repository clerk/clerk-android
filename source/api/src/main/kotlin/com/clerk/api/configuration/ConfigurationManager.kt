package com.clerk.api.configuration

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.Constants.Config.API_TIMEOUT_SECONDS
import com.clerk.api.Constants.Config.BACKOFF_BASE_DELAY_SECONDS
import com.clerk.api.Constants.Config.EXPONENTIAL_BACKOFF_SHIFT
import com.clerk.api.Constants.Config.MAX_ATTESTATION_RETRIES
import com.clerk.api.Constants.Config.MAX_INITIALIZATION_RETRIES
import com.clerk.api.Constants.Config.REFRESH_TOKEN_INTERVAL
import com.clerk.api.Constants.Config.TIMEOUT_MULTIPLIER
import com.clerk.api.attestation.DeviceAttestationHelper
import com.clerk.api.configuration.connectivity.NetworkConnectivityMonitor
import com.clerk.api.configuration.lifecycle.AppLifecycleListener
import com.clerk.api.locale.LocaleProvider
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.FraudSettings
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.fold
import com.clerk.api.network.serialization.successOrElse
import com.clerk.api.session.GetTokenOptions
import com.clerk.api.session.fetchToken
import com.clerk.api.storage.StorageHelper
import java.lang.ref.WeakReference
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Internal configuration manager responsible for Clerk SDK initialization and lifecycle management.
 *
 * This class handles:
 * - API client configuration and base URL extraction
 * - Storage initialization for session persistence
 * - Concurrent client and environment data fetching
 * - Application lifecycle monitoring for state refresh
 * - Context memory leak prevention through weak references
 * - Background device attestation to optimize startup performance
 */
internal class ConfigurationManager {

  /**
   * Coroutine scope with SupervisorJob for parallel API requests.
   *
   * Uses SupervisorJob to ensure that if one coroutine fails, others continue running. This is
   * essential for handling concurrent client and environment requests independently.
   */
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  /** Weak reference to application context to prevent memory leaks. */
  internal var context: WeakReference<Context>? = null
    set(value) {
      field = value
      // Don't initialize storage immediately - do it lazily when needed
    }

  /** Track if storage has been initialized to avoid duplicate initialization */
  private var storageInitialized = false

  /** Internal mutable state flow for initialization status. */
  private val _isInitialized = MutableStateFlow(false)

  /**
   * Public read-only state flow indicating SDK initialization completion.
   *
   * Emits true when both client and environment data have been successfully loaded.
   */
  val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

  /** Internal mutable state flow for device attestation status. */
  private val _isDeviceAttested = MutableStateFlow(false)

  /** Internal mutable state flow for initialization errors. */
  private val _initializationError = MutableStateFlow<Throwable?>(null)

  /**
   * Public read-only state flow for initialization errors.
   *
   * Emits the last error that occurred during initialization, or null if no error. This allows apps
   * to detect and handle initialization failures gracefully.
   */
  val initializationError: StateFlow<Throwable?> = _initializationError.asStateFlow()

  /**
   * The publishable key from Clerk Dashboard used for API authentication.
   *
   * This key determines the API base URL and connects the app to the correct Clerk instance.
   */
  internal lateinit var publishableKey: String

  /** Flag to track if configuration has been started to prevent duplicate initialization. */
  private var hasConfigured = false

  /** Stored configuration options for use in retry attempts. */
  private var storedOptions: ClerkConfigurationOptions? = null

  /**
   * Internal job reference for ongoing refresh token operations.
   *
   * Used to cancel ongoing refresh operations if needed.
   */
  private var refreshJob: Job? = null

  /** Internal job reference for ongoing device attestation operations. */
  private var attestationJob: Job? = null

  /** Internal job reference for ongoing initialization operations. */
  private var initializationJob: Job? = null

  /** Ensures storage is initialized when needed. */
  private fun ensureStorageInitialized() {
    if (!storageInitialized) {
      context?.get()?.let { context ->
        StorageHelper.initialize(context)
        storageInitialized = true
        ClerkLog.d("Storage initialized")
      }
    }
  }

  /**
   * Configures the Clerk SDK with the provided application context and publishable key.
   *
   * This method performs the following initialization steps:
   * 1. Stores application context safely using WeakReference
   * 2. Extracts API base URL from publishable key (synchronous - fast)
   * 3. Configures the Clerk API client (synchronous - fast)
   * 4. Initiates background client and environment data refresh (async)
   * 5. Sets up application lifecycle monitoring (async)
   *
   * Storage initialization, device ID generation, and device attestation are moved to background to
   * optimize startup time and avoid blocking the main thread.
   *
   * @param context The application context used for storage and API configuration.
   * @param publishableKey The publishable key from Clerk Dashboard for API authentication.
   * @param options Additional configuration options, such as device attestation settings.
   * @throws IllegalStateException if called multiple times.
   * @throws IllegalArgumentException if publishableKey format is invalid.
   */
  fun configure(context: Context, publishableKey: String, options: ClerkConfigurationOptions?) {
    if (hasConfigured) {
      ClerkLog.w(
        "ConfigurationManager.configure() called multiple times. Ignoring subsequent calls."
      )
      return
    }

    try {
      this.context = WeakReference(context.applicationContext)
      this.publishableKey = publishableKey
      this.storedOptions = options
      LocaleProvider.initialize()

      val baseUrl =
        if (options?.proxyUrl != null) {
          options.proxyUrl
        } else {
          PublishableKeyHelper().extractApiUrl(publishableKey)
        }
      Clerk.baseUrl = baseUrl
      Clerk.applicationId = context.applicationContext.packageName

      // Initialize storage synchronously before configuring ClerkApi to ensure interceptors
      // can safely access StorageHelper without race conditions
      ensureStorageInitialized()

      ClerkApi.configure(Clerk.baseUrl, context.applicationContext)

      // Mark as configured before starting async operations
      hasConfigured = true

      // Set up network connectivity monitoring for automatic retry on reconnection
      configureConnectivityMonitor(context.applicationContext)

      // Start all background initialization concurrently
      initializationJob =
        scope.launch {
          // Initialize device ID after storage is ready (storage is already initialized above)
          val deviceIdInitJob = async { DeviceIdGenerator.initialize() }

          // Launch data refresh independently (doesn't depend on storage)
          val dataRefreshJob = async { refreshClientAndEnvironment(options, retryCount = 0) }

          // Wait for device ID init before setting up lifecycle monitoring
          deviceIdInitJob.await()

          // Set up lifecycle monitoring for automatic refresh
          AppLifecycleListener.configure {
            if (hasConfigured) {
              scope.launch {
                refreshClientAndEnvironment(options, retryCount = 0)
                startTokenRefresh()
              }
            }
          }

          // Data refresh can continue independently
          dataRefreshJob.await()
        }

      ClerkLog.d("ConfigurationManager configured successfully - background initialization started")
    } catch (e: Exception) {
      hasConfigured = false
      ClerkLog.e("Failed to configure ConfigurationManager: ${e.message}")
      throw e
    }
  }

  private fun startTokenRefresh() {
    ClerkLog.d(
      "startTokenRefresh() called - debugMode: ${Clerk.debugMode}, hasConfigured: $hasConfigured"
    )

    if (!hasConfigured) {
      ClerkLog.w("Cannot start token refresh - not configured")
      return
    }

    // Cancel any ongoing jobs
    refreshJob?.cancel()
    refreshJob =
      scope.launch {
        while (isActive) {
          try {
            val session = Clerk.session
            if (session != null) {
              if (Clerk.debugMode) {
                ClerkLog.d("Refreshing token for session: ${session.id}")
              }
              // Use async to avoid blocking the refresh loop
              async { session.fetchToken(GetTokenOptions(skipCache = false)) }
            } else {
              if (Clerk.debugMode) {
                ClerkLog.d("No session available for token refresh")
              }
            }
          } catch (e: Exception) {
            ClerkLog.w("Token refresh failed: ${e.message}")
          }

          delay(REFRESH_TOKEN_INTERVAL.seconds)
        }
      }
  }

  /**
   * Refreshes client and environment data by making concurrent API requests.
   *
   * This method:
   * - Performs client and environment requests in parallel for better performance
   * - Handles errors gracefully with detailed logging
   * - Updates Clerk state when both requests succeed
   * - Sets initialization status based on operation success
   * - Launches device attestation independently to avoid blocking
   * - Starts token refresh as soon as client data is available
   * - Retries with exponential backoff on failure (up to MAX_INITIALIZATION_RETRIES)
   *
   * The method is safe to call multiple times and will not interfere with ongoing requests.
   *
   * @param options Configuration options for the SDK.
   * @param retryCount Current retry attempt number (0 for initial attempt).
   */
  private fun refreshClientAndEnvironment(options: ClerkConfigurationOptions?, retryCount: Int) {
    if (!hasConfigured) {
      ClerkLog.w("Attempted to refresh before configuration. Skipping.")
      return
    }

    // Check if context is still available
    val context = context?.get()
    if (context == null) {
      ClerkLog.w("Application context no longer available. Cannot refresh client and environment.")
      _initializationError.value = IllegalStateException("Application context no longer available")
      return
    }

    if (Clerk.debugMode) {
      ClerkLog.d("Starting client and environment refresh (attempt ${retryCount + 1})")
    }

    // Clear error state on new attempt
    if (retryCount == 0) {
      _initializationError.value = null
    }

    scope.launch {
      try {
        // Add timeout to prevent hanging
        withTimeout((API_TIMEOUT_SECONDS * TIMEOUT_MULTIPLIER)) {
          // Launch concurrent requests for better performance
          val clientDeferred = async { Client.get() }
          val environmentDeferred = async { Environment.get() }

          // Process results as they become available
          val clientResult = clientDeferred.await()
          val environmentResult = environmentDeferred.await()

          // Handle results independently
          handleClientResult(clientResult)
          handleEnvironmentResult(environmentResult)

          // Update Clerk state if both operations succeeded
          if (clientResult is ClerkResult.Success && environmentResult is ClerkResult.Success) {
            // Update state immediately
            updateClerkState(clientResult.value, environmentResult.value)
            _isInitialized.value = true
            _initializationError.value = null

            // Launch dependent operations concurrently
            launch {
              // Start token refresh as soon as client is available
              if (Clerk.session != null) {
                startTokenRefresh()
              }
            }

            // Launch device attestation independently - don't block anything
            launch {
              launchDeviceAttestationInBackground(
                applicationContext = context.applicationContext,
                cloudProjectNumber = options?.deviceAttestationOptions?.cloudProjectNumber,
                applicationId = options?.deviceAttestationOptions?.applicationId,
                clientId = clientResult.value.id!!,
                environment = environmentResult.value,
              )
            }

            if (Clerk.debugMode) {
              ClerkLog.d("Client and environment refresh completed successfully")
            }
          } else {
            val errorMessage =
              "Failed to refresh client and environment -" +
                " client: ${clientResult.javaClass.simpleName}," +
                " environment: ${environmentResult.javaClass.simpleName}"
            ClerkLog.e(errorMessage)
            handleInitializationFailure(
              error = IllegalStateException(errorMessage),
              options = options,
              retryCount = retryCount,
            )
          }
        }
      } catch (e: Exception) {
        ClerkLog.e("Exception during client and environment refresh: ${e.message}")
        handleInitializationFailure(error = e, options = options, retryCount = retryCount)
      }
    }
  }

  /**
   * Handles initialization failure by setting error state and scheduling retry if within limits.
   *
   * @param error The exception that caused the failure.
   * @param options Configuration options for retry.
   * @param retryCount Current retry attempt number.
   */
  private fun handleInitializationFailure(
    error: Throwable,
    options: ClerkConfigurationOptions?,
    retryCount: Int,
  ) {
    _isInitialized.value = false
    _initializationError.value = error

    if (retryCount < MAX_INITIALIZATION_RETRIES) {
      scope.launch { retryInitialization(options, retryCount + 1) }
    } else {
      ClerkLog.e(
        "Max initialization retries ($MAX_INITIALIZATION_RETRIES) reached. " +
          "Initialization failed permanently. Call Clerk.reinitialize() to retry manually."
      )
    }
  }

  /**
   * Retries initialization after a delay with exponential backoff.
   *
   * @param options Configuration options for the SDK.
   * @param retryCount Current retry attempt number.
   */
  private suspend fun retryInitialization(options: ClerkConfigurationOptions?, retryCount: Int) {
    // Exponential backoff: 5s, 10s, 20s
    val delaySeconds = BACKOFF_BASE_DELAY_SECONDS * (EXPONENTIAL_BACKOFF_SHIFT shl (retryCount - 1))
    ClerkLog.d("Retrying initialization in ${delaySeconds}s (attempt $retryCount)")

    delay(delaySeconds.seconds)

    // Retry the initialization
    refreshClientAndEnvironment(options, retryCount)
  }

  /**
   * Manually triggers a reinitialization attempt.
   *
   * This method can be called by developers when initialization has failed and they want to retry
   * manually, for example after network connectivity is restored.
   *
   * @return true if reinitialization was started, false if SDK is not configured or already
   *   initialized.
   */
  fun reinitialize(): Boolean {
    if (!hasConfigured) {
      ClerkLog.w("Cannot reinitialize - SDK not configured. Call Clerk.initialize() first.")
      return false
    }

    if (_isInitialized.value) {
      ClerkLog.d("SDK already initialized. Skipping reinitialization.")
      return false
    }

    ClerkLog.d("Manual reinitialization requested")
    _initializationError.value = null
    refreshClientAndEnvironment(storedOptions, retryCount = 0)
    return true
  }

  /**
   * Configures the network connectivity monitor to automatically retry initialization when
   * connectivity is restored.
   *
   * This enables proactive initialization without requiring a background/foreground app cycle. When
   * the device regains internet access after being offline, the SDK will automatically attempt to
   * initialize if it hasn't completed initialization yet.
   *
   * @param context The application context used for ConnectivityManager access.
   */
  private fun configureConnectivityMonitor(context: Context) {
    NetworkConnectivityMonitor.configure(context) {
      // Callback invoked when connectivity is restored
      if (!_isInitialized.value && hasConfigured) {
        ClerkLog.d("Connectivity restored - attempting automatic reinitialization")
        scope.launch {
          _initializationError.value = null
          refreshClientAndEnvironment(storedOptions, retryCount = 0)
        }
      } else if (_isInitialized.value) {
        // Already initialized, but connectivity was restored - refresh data
        if (Clerk.debugMode) {
          ClerkLog.d("Connectivity restored - SDK already initialized, refreshing data")
        }
        scope.launch {
          refreshClientAndEnvironment(storedOptions, retryCount = 0)
        }
      }
    }
  }

  /**
   * Launches device attestation in the background to avoid blocking initialization. This allows the
   * SDK to become ready for use while attestation completes separately.
   *
   * This method returns immediately and doesn't block the caller.
   */
  private fun launchDeviceAttestationInBackground(
    applicationId: String?,
    clientId: String,
    environment: Environment,
    cloudProjectNumber: Long?,
    applicationContext: Context,
  ) {
    // Cancel any ongoing attestation
    attestationJob?.cancel()

    attestationJob =
      scope.launch {
        try {
          ClerkLog.d("Starting background device attestation")

          // Run attestation in a separate async block for better isolation
          val attestationResult = async {
            attestDeviceIfNeeded(
              applicationContext = applicationContext,
              cloudProjectNumber = cloudProjectNumber,
              applicationId = applicationId,
              clientId = clientId,
              environment = environment,
            )
          }

          attestationResult.await()
          _isDeviceAttested.value = true
          ClerkLog.d("Background device attestation completed successfully")
        } catch (e: Exception) {
          ClerkLog.w("Background device attestation failed: ${e.message}")
          _isDeviceAttested.value = false

          // Launch retry in a separate coroutine to avoid blocking
          launch {
            retryDeviceAttestation(
              applicationContext,
              cloudProjectNumber,
              applicationId,
              clientId,
              environment,
            )
          }
        }
      }
  }

  /**
   * Retries device attestation after a delay if the initial attempt fails. Uses exponential backoff
   * and runs entirely in background.
   */
  private suspend fun retryDeviceAttestation(
    applicationContext: Context,
    cloudProjectNumber: Long?,
    applicationId: String?,
    clientId: String,
    environment: Environment,
    retryCount: Int = 0,
  ) {
    if (retryCount >= MAX_ATTESTATION_RETRIES) {
      ClerkLog.w("Max device attestation retries reached")
      return
    }

    try {
      // Exponential backoff: 5s, 10s, 20s
      val delaySeconds = BACKOFF_BASE_DELAY_SECONDS * (EXPONENTIAL_BACKOFF_SHIFT shl retryCount)
      ClerkLog.d("Retrying device attestation in ${delaySeconds}s (attempt ${retryCount + 1})")

      delay(delaySeconds.seconds)

      // Run retry in async for better isolation
      coroutineScope {
        val retryResult = async {
          attestDeviceIfNeeded(
            applicationContext = applicationContext,
            cloudProjectNumber = cloudProjectNumber,
            applicationId = applicationId,
            clientId = clientId,
            environment = environment,
          )
        }

        retryResult.await()
      }

      _isDeviceAttested.value = true
      ClerkLog.d("Device attestation retry successful")
    } catch (e: Exception) {
      ClerkLog.w("Device attestation retry ${retryCount + 1} failed: ${e.message}")

      // Recursively retry with incremented count
      retryDeviceAttestation(
        applicationContext,
        cloudProjectNumber,
        applicationId,
        clientId,
        environment,
        retryCount + 1,
      )
    }
  }

  /**
   * Handles the client API result with appropriate logging.
   *
   * Note: updating clerk state happens in [updateClerkState]
   */
  private fun handleClientResult(result: ClerkResult<Client, *>) {
    result.fold(
      onSuccess = { client ->
        if (Clerk.debugMode) {
          ClerkLog.d("Client loaded successfully: ${client.id}")
        }
      },
      onFailure = { failure ->
        ClerkLog.e("Failed to load client: ${failure.error}")
        logApiError("Client", failure.errorType, failure.error.toString())
      },
    )
  }

  /**
   * Handles the environment API result with appropriate logging.
   *
   * Note: updating clerk state happens in [updateClerkState]
   */
  private fun handleEnvironmentResult(result: ClerkResult<Environment, *>) {
    result.fold(
      onSuccess = { environment ->
        if (Clerk.debugMode) {
          ClerkLog.d("Environment loaded successfully: ${environment.authConfig}")
        }
      },
      onFailure = { failure ->
        ClerkLog.e("Failed to load environment: ${failure.error}")
        logApiError("Environment", failure.errorType, failure.error.toString())
      },
    )
  }

  private suspend fun attestDeviceIfNeeded(
    applicationId: String?,
    clientId: String,
    environment: Environment,
    cloudProjectNumber: Long?,
    applicationContext: Context,
  ) {
    val deviceAttestationMode = environment.fraudSettings.native.deviceAttestationMode
    if (
      (deviceAttestationMode == FraudSettings.DeviceAttestationMode.ONBOARDING) ||
        deviceAttestationMode == FraudSettings.DeviceAttestationMode.ENFORCED
    ) {
      DeviceAttestationHelper.prepareIntegrityTokenProvider(
        applicationContext,
        cloudProjectNumber = cloudProjectNumber,
      )
      val token =
        DeviceAttestationHelper.attestDevice(clientId).successOrElse {
          error("Device attestation failed: $it")
        }

      DeviceAttestationHelper.performAssertion(token, applicationId).successOrElse {
        error("Device attestation failed: $it")
      }
    }
  }

  /** Logs API errors with appropriate detail based on error type. */
  private fun logApiError(
    operation: String,
    errorType: ClerkResult.Failure.ErrorType,
    error: String,
  ) {
    when (errorType) {
      ClerkResult.Failure.ErrorType.API -> ClerkLog.e("$operation API error: $error")
      ClerkResult.Failure.ErrorType.HTTP -> ClerkLog.e("$operation HTTP error: $error")
      ClerkResult.Failure.ErrorType.UNKNOWN -> ClerkLog.e("$operation unknown error: $error")
    }
  }

  /**
   * Updates the Clerk singleton state with successfully loaded data.
   *
   * This method is called only when both client and environment data have been loaded successfully.
   */
  private fun updateClerkState(client: Client, environment: Environment) {
    Clerk.updateClient(client)
    Clerk.updateEnvironment(environment)

    if (Clerk.debugMode) {
      ClerkLog.d("Clerk state updated - Client ID: ${client.id}, Sessions: ${client.sessions.size}")
    }
  }
}
