package com.clerk.configuration

import android.content.Context
import com.clerk.Clerk
import com.clerk.Clerk.debugMode
import com.clerk.ClerkConfigurationOptions
import com.clerk.attestation.DeviceAttestationHelper
import com.clerk.configuration.lifecycle.AppLifecycleListener
import com.clerk.log.ClerkLog
import com.clerk.network.ClerkApi
import com.clerk.network.model.client.Client
import com.clerk.network.model.environment.Environment
import com.clerk.network.model.environment.FraudSettings
import com.clerk.network.serialization.ClerkResult
import com.clerk.network.serialization.fold
import com.clerk.session.SessionGetTokenOptions
import com.clerk.session.fetchToken
import com.clerk.storage.StorageHelper
import java.lang.ref.WeakReference
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val REFRESH_TOKEN_INTERVAL = 50

/**
 * Internal configuration manager responsible for Clerk SDK initialization and lifecycle management.
 *
 * This class handles:
 * - API client configuration and base URL extraction
 * - Storage initialization for session persistence
 * - Concurrent client and environment data fetching
 * - Application lifecycle monitoring for state refresh
 * - Context memory leak prevention through weak references
 */
internal class ConfigurationManager {

  /**
   * Coroutine scope with SupervisorJob for parallel API requests.
   *
   * Uses SupervisorJob to ensure that if one coroutine fails, others continue running. This is
   * essential for handling concurrent client and environment requests independently.
   */
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  /**
   * Weak reference to application context to prevent memory leaks.
   *
   * The setter automatically initializes StorageHelper when a valid context is provided, ensuring
   * storage is ready before any session operations.
   */
  internal var context: WeakReference<Context>? = null
    set(value) {
      field = value
      value?.get()?.let { context -> StorageHelper.initialize(context) }
    }

  /** Internal mutable state flow for initialization status. */
  private val _isInitialized = MutableStateFlow(false)

  /**
   * Public read-only state flow indicating SDK initialization completion.
   *
   * Emits true when both client and environment data have been successfully loaded.
   */
  val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

  /**
   * The publishable key from Clerk Dashboard used for API authentication.
   *
   * This key determines the API base URL and connects the app to the correct Clerk instance.
   */
  internal lateinit var publishableKey: String

  /** Flag to track if configuration has been started to prevent duplicate initialization. */
  private var hasConfigured = false

  /**
   * Internal job reference for ongoing refresh token operations.
   *
   * Used to cancel ongoing refresh operations if needed.
   */
  private var refreshJob: Job? = null

  /**
   * Configures the Clerk SDK with the provided application context and publishable key.
   *
   * This method performs the following initialization steps:
   * 1. Stores application context safely using WeakReference
   * 2. Initializes local storage for session persistence
   * 3. Extracts API base URL from publishable key
   * 4. Configures the Clerk API client
   * 5. Initiates client and environment data refresh
   * 6. Sets up application lifecycle monitoring
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
      DeviceAttestationHelper.prepareIntegrityTokenProvider(
        context.applicationContext,
        cloudProjectNumber = options?.deviceAttestationOptions?.cloudProjectNumber,
      )

      // Initialize storage helper explicitly to ensure it's ready
      StorageHelper.initialize(context.applicationContext)
      DeviceIdGenerator.initialize()

      // Extract base URL and configure API client
      val baseUrl = PublishableKeyHelper().extractApiUrl(publishableKey)
      Clerk.baseUrl = baseUrl
      ClerkApi.configure(Clerk.baseUrl, context.applicationContext)

      // Mark as configured before starting async operations
      hasConfigured = true

      // Start initial data refresh
      refreshClientAndEnvironment(options?.deviceAttestationOptions?.applicationId)

      // Set up lifecycle monitoring for automatic refresh
      AppLifecycleListener.configure {
        if (hasConfigured) {
          refreshClientAndEnvironment(options?.deviceAttestationOptions?.applicationId)
          startTokenRefresh()
        }
      }

      ClerkLog.d("ConfigurationManager configured successfully")
    } catch (e: Exception) {
      hasConfigured = false
      ClerkLog.e("Failed to configure ConfigurationManager: ${e.message}")
      throw e
    }
  }

  private fun startTokenRefresh() {
    ClerkLog.d("startTokenRefresh() called - debugMode: $debugMode, hasConfigured: $hasConfigured")

    if (!hasConfigured) {
      ClerkLog.w("Cannot start token refresh - not configured")
      return
    }

    val currentSession = Clerk.session
    if (currentSession == null) {
      ClerkLog.w("Cannot start token refresh - no active session")
      return
    }

    ClerkLog.d("Starting token refresh for session: ${currentSession.id}")

    // Cancel any ongoing jobs
    refreshJob?.cancel()
    refreshJob =
      scope.launch {
        var refreshCount = 0
        while (isActive) {
          ClerkLog.d("Token refresh cycle ${++refreshCount} - waiting ${REFRESH_TOKEN_INTERVAL}s")
          delay(REFRESH_TOKEN_INTERVAL.seconds)

          try {
            val session = Clerk.session
            if (session != null) {
              ClerkLog.d("Refreshing token for session: ${session.id}")
              session.fetchToken(SessionGetTokenOptions(skipCache = true))
              ClerkLog.d("Token refresh successful")
            } else {
              ClerkLog.w("No session available for token refresh")
            }
          } catch (e: Exception) {
            ClerkLog.w("Token refresh failed: ${e.message}")
          }
        }
        ClerkLog.d("Token refresh loop ended")
      }
  }

  /**
   * Refreshes client and environment data by making concurrent API requests.
   *
   * This method:
   * - Performs client and environment requests in parallel for better performance
   * - Handles errors gracefully with detailed logging
   * - Updates Clerk singleton state only when both requests succeed
   * - Sets initialization status based on operation success
   *
   * The method is safe to call multiple times and will not interfere with ongoing requests.
   */
  private fun refreshClientAndEnvironment(applicationId: String?) {
    if (!hasConfigured) {
      ClerkLog.w("Attempted to refresh before configuration. Skipping.")
      return
    }

    // Check if context is still available
    val context = context?.get()
    if (context == null) {
      ClerkLog.w("Application context no longer available. Cannot refresh client and environment.")
      return
    }

    if (debugMode) {
      ClerkLog.d("Starting client and environment refresh")
    }

    scope.launch {
      try {
        // Launch concurrent requests for better performance
        val clientDeferred = async { Client.get() }
        val environmentDeferred = async { Environment.get() }

        // Await both results
        val clientResult = clientDeferred.await()
        val environmentResult = environmentDeferred.await()

        // Handle client result
        handleClientResult(clientResult)

        // Handle environment result
        handleEnvironmentResult(environmentResult)

        // Update Clerk state if both operations succeeded
        if (clientResult is ClerkResult.Success && environmentResult is ClerkResult.Success) {
          attestDeviceIfNeeded(
            applicationId = applicationId,
            clientId = clientResult.value.id!!,
            environment = environmentResult.value,
          )
          updateClerkState(clientResult.value, environmentResult.value)
          _isInitialized.value = true

          if (Clerk.session != null) {
            startTokenRefresh()
          }

          if (debugMode) {
            ClerkLog.d("Client and environment refresh completed successfully")
          }
        } else {
          ClerkLog.e(
            "Failed to refresh client and environment -" +
              " client: ${clientResult.javaClass.simpleName}," +
              " environment: ${environmentResult.javaClass.simpleName}"
          )
          _isInitialized.value = false
        }
      } catch (e: Exception) {
        ClerkLog.e("Exception during client and environment refresh: ${e.message}")
        _isInitialized.value = false
      }
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
        if (debugMode) {
          ClerkLog.d("Environment loaded successfully: ${environment.authConfig}")
        }
      },
      onFailure = { failure ->
        ClerkLog.e("Failed to load environment: ${failure.error}")
        logApiError("Environment", failure.errorType, failure.error.toString())
      },
    )
  }

  private fun attestDeviceIfNeeded(
    applicationId: String?,
    clientId: String,
    environment: Environment,
  ) {
    val deviceAttestationMode = environment.fraudSettings.native.deviceAttestationMode
    if (
      (deviceAttestationMode == FraudSettings.DeviceAttestationMode.ONBOARDING) ||
        deviceAttestationMode == FraudSettings.DeviceAttestationMode.ENFORCED
    ) {
      DeviceAttestationHelper.getAndVerifyIntegrityToken(
        applicationId = applicationId,
        clientId = clientId,
      )
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
    Clerk.client = client
    Clerk.updateEnvironment(environment)

    if (Clerk.debugMode) {
      ClerkLog.d("Clerk state updated - Client ID: ${client.id}, Sessions: ${client.sessions.size}")
    }
  }
}
