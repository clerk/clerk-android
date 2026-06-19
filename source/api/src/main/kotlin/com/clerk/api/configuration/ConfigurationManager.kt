package com.clerk.api.configuration

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.Constants.Config.API_TIMEOUT_SECONDS
import com.clerk.api.Constants.Config.BACKOFF_BASE_DELAY_SECONDS
import com.clerk.api.Constants.Config.EXPONENTIAL_BACKOFF_SHIFT
import com.clerk.api.Constants.Config.MAX_INITIALIZATION_RETRIES
import com.clerk.api.Constants.Config.REFRESH_TOKEN_INTERVAL
import com.clerk.api.Constants.Config.TIMEOUT_MULTIPLIER
import com.clerk.api.configuration.connectivity.NetworkConnectivityMonitor
import com.clerk.api.configuration.lifecycle.AppLifecycleListener
import com.clerk.api.locale.LocaleProvider
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.fold
import com.clerk.api.session.GetTokenOptions
import com.clerk.api.session.fetchToken
import com.clerk.api.sso.SSOService
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import java.lang.ref.WeakReference
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 */
internal class ConfigurationManager {
  private companion object {
    const val LIFECYCLE_REFRESH_DEFER_STEP_MS = 100L
    const val LIFECYCLE_REFRESH_MAX_DEFER_MS = 5_000L
  }

  /**
   * Coroutine scope with SupervisorJob for parallel API requests.
   *
   * Uses SupervisorJob to ensure that if one coroutine fails, others continue running. This is
   * essential for handling concurrent client and environment requests independently.
   */
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val refreshMutex = Mutex()

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
  @Volatile private var hasConfigured = false

  /** Stored configuration options for use in retry attempts. */
  private var storedOptions: ClerkConfigurationOptions? = null

  /**
   * Internal job reference for ongoing refresh token operations.
   *
   * Used to cancel ongoing refresh operations if needed.
   */
  private var refreshJob: Job? = null

  /** Internal job reference for ongoing initialization operations. */
  private var initializationJob: Job? = null

  /** Monotonic token used to ignore stale refreshes from an older configuration. */
  @Volatile private var configurationVersion = 0

  private enum class RefreshMode {
    INITIALIZATION,
    DEVICE_TOKEN_UPDATE,
  }

  private data class RefreshAttempt(
    val options: ClerkConfigurationOptions?,
    val retryCount: Int,
    val expectedConfigurationVersion: Int,
  ) {
    fun nextRetry(): RefreshAttempt = copy(retryCount = retryCount + 1)
  }

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
   * Storage initialization and device ID generation are moved to background to optimize startup
   * time and avoid blocking the main thread.
   *
   * @param context The application context used for storage and API configuration.
   * @param publishableKey The publishable key from Clerk Dashboard for API authentication.
   * @param options Additional configuration options.
   * @throws IllegalStateException if called multiple times.
   * @throws IllegalArgumentException if publishableKey format is invalid.
   */
  @Synchronized
  fun configure(
    context: Context,
    publishableKey: String,
    options: ClerkConfigurationOptions?,
  ): Boolean {
    if (hasConfigured) {
      ClerkLog.w(
        "ConfigurationManager.configure() called multiple times. Ignoring subsequent calls."
      )
      return false
    }

    try {
      val configuredVersion = configureSdkState(context, publishableKey, options)
      initializationJob = launchInitialization(options, configuredVersion)

      ClerkLog.d("ConfigurationManager configured successfully - background initialization started")
      return true
    } catch (e: Exception) {
      hasConfigured = false
      ClerkLog.e("Failed to configure ConfigurationManager: ${e.message}")
      throw e
    }
  }

  private fun configureSdkState(
    context: Context,
    publishableKey: String,
    options: ClerkConfigurationOptions?,
  ): Int {
    configurationVersion += 1
    val configuredVersion = configurationVersion
    this.context = WeakReference(context.applicationContext)
    this.storedOptions = options
    this.publishableKey = publishableKey
    Clerk.publishableKey = publishableKey
    LocaleProvider.initialize()

    val baseUrl = options?.proxyUrl ?: PublishableKeyHelper().extractApiUrl(publishableKey)
    Clerk.baseUrl = baseUrl
    Clerk.applicationId = context.applicationContext.packageName

    ensureStorageInitialized()
    ClerkApi.configure(Clerk.baseUrl, context.applicationContext)
    hasConfigured = true
    configureConnectivityMonitor(context.applicationContext)
    return configuredVersion
  }

  private fun launchInitialization(
    options: ClerkConfigurationOptions?,
    configuredVersion: Int,
  ): Job = scope.launch {
    val attempt =
      RefreshAttempt(
        options = options,
        retryCount = 0,
        expectedConfigurationVersion = configuredVersion,
      )
    val deviceIdInitJob = async { DeviceIdGenerator.initialize() }
    val dataRefreshJob = async {
      refreshClientAndEnvironment(attempt, RefreshMode.INITIALIZATION)
    }

    deviceIdInitJob.await()
    AppLifecycleListener.configure {
      if (hasConfigured) {
        scope.launch {
          deferForegroundRefreshDuringPendingSso()
          refreshClientAndEnvironment(attempt, RefreshMode.INITIALIZATION)
          startTokenRefresh()
        }
      }
    }
    dataRefreshJob.await()
  }

  fun isConfigured(): Boolean = hasConfigured

  @Synchronized
  fun reset() {
    configurationVersion += 1
    scope.coroutineContext.cancelChildren()
    initializationJob?.cancel()
    refreshJob?.cancel()
    initializationJob = null
    refreshJob = null
    context = null
    storageInitialized = false
    hasConfigured = false
    storedOptions = null
    publishableKey = ""
    _isInitialized.value = false
    _initializationError.value = null
    NetworkConnectivityMonitor.stop()
    AppLifecycleListener.stop()
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
    refreshJob = scope.launch {
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

  suspend fun updateDeviceToken(deviceToken: String): ClerkResult<Unit, ClerkErrorResponse> {
    val validationError = validateDeviceTokenUpdate(deviceToken)
    if (validationError != null) return validationError

    ensureStorageInitialized()
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, deviceToken)

    return refreshClientAndEnvironment(
      attempt = currentRefreshAttempt(),
      mode = RefreshMode.DEVICE_TOKEN_UPDATE,
      skipClientId = true,
    )
  }

  suspend fun clearDeviceToken(): ClerkResult<Unit, ClerkErrorResponse> {
    val validationError = validateDeviceTokenClear()
    if (validationError != null) return validationError

    ensureStorageInitialized()
    StorageHelper.deleteValue(StorageKey.DEVICE_TOKEN)
    Clerk.updateClient(Client())
    Clerk.clearSessionAndUserState()

    return refreshClientAndEnvironment(
      attempt = currentRefreshAttempt(),
      mode = RefreshMode.DEVICE_TOKEN_UPDATE,
      skipClientId = true,
    )
  }

  /**
   * Refreshes client and environment data by making concurrent API requests.
   *
   * This method:
   * - Performs client and environment requests in parallel for better performance
   * - Handles errors gracefully with detailed logging
   * - Updates Clerk state when both requests succeed
   * - Sets initialization status based on operation success
   * - Starts token refresh as soon as client data is available
   * - Retries with exponential backoff on failure (up to MAX_INITIALIZATION_RETRIES)
   *
   * The method is safe to call multiple times and will not interfere with ongoing requests.
   *
   * @param options Configuration options for the SDK.
   * @param retryCount Current retry attempt number (0 for initial attempt).
   */
  private fun currentRefreshAttempt(): RefreshAttempt =
    RefreshAttempt(
      options = storedOptions,
      retryCount = 0,
      expectedConfigurationVersion = configurationVersion,
    )

  // Launches in a new coroutine so retries triggered inside the mutex do not deadlock.
  private fun queueClientAndEnvironmentRefresh(attempt: RefreshAttempt = currentRefreshAttempt()) {
    scope.launch { refreshClientAndEnvironment(attempt, RefreshMode.INITIALIZATION) }
  }

  private fun validateDeviceTokenUpdate(
    deviceToken: String
  ): ClerkResult<Unit, ClerkErrorResponse>? {
    return when {
      deviceToken.isBlank() ->
        ClerkResult.unknownFailure(IllegalArgumentException("Device token must not be blank"))
      !hasConfigured ->
        ClerkResult.unknownFailure(
          IllegalStateException("Clerk must be initialized before updating the device token")
        )
      else -> null
    }
  }

  private fun validateDeviceTokenClear(): ClerkResult<Unit, ClerkErrorResponse>? {
    return when {
      !hasConfigured ->
        ClerkResult.unknownFailure(
          IllegalStateException("Clerk must be initialized before clearing the device token")
        )
      else -> null
    }
  }

  private suspend fun deferForegroundRefreshDuringPendingSso() {
    var waitedMs = 0L
    while (hasPendingSsoFlow() && waitedMs < LIFECYCLE_REFRESH_MAX_DEFER_MS) {
      if (waitedMs == 0L) {
        ClerkLog.d("Deferring lifecycle refresh while SSO completion is in progress")
      }
      delay(LIFECYCLE_REFRESH_DEFER_STEP_MS)
      waitedMs += LIFECYCLE_REFRESH_DEFER_STEP_MS
    }
  }

  internal fun hasPendingSsoFlow(): Boolean {
    return SSOService.hasPendingAuthentication() || SSOService.hasPendingExternalAccountConnection()
  }

  private suspend fun refreshClientAndEnvironment(
    attempt: RefreshAttempt,
    mode: RefreshMode,
    skipClientId: Boolean = false,
  ): ClerkResult<Unit, ClerkErrorResponse> {
    val failure = validateRefreshPreconditions(mode, attempt.expectedConfigurationVersion)
    if (failure != null) return failure

    return refreshMutex.withLock {
      val lockedFailure = validateRefreshPreconditions(mode, attempt.expectedConfigurationVersion)
      if (lockedFailure != null) return@withLock lockedFailure

      try {
        if (Clerk.debugMode) {
          ClerkLog.d("Starting client and environment refresh (attempt ${attempt.retryCount + 1})")
        }

        if (attempt.retryCount == 0 && mode == RefreshMode.INITIALIZATION) {
          _initializationError.value = null
        }

        executeRefresh(attempt = attempt, mode = mode, skipClientId = skipClientId)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        ClerkLog.e("Exception during client and environment refresh: ${e.message}")
        if (mode == RefreshMode.INITIALIZATION) {
          handleInitializationFailure(error = e, attempt = attempt)
        }
        ClerkResult.unknownFailure(e)
      }
    }
  }

  private fun validateRefreshPreconditions(
    mode: RefreshMode,
    expectedConfigurationVersion: Int,
  ): ClerkResult<Unit, ClerkErrorResponse>? =
    when {
      expectedConfigurationVersion != configurationVersion -> staleConfigurationFailure()
      !hasConfigured -> {
        ClerkLog.w("Attempted to refresh before configuration. Skipping.")
        ClerkResult.unknownFailure(
          IllegalStateException("Clerk must be initialized before refreshing")
        )
      }
      context?.get() == null -> {
        ClerkLog.w(
          "Application context no longer available. Cannot refresh client and environment."
        )
        val error = IllegalStateException("Application context no longer available")
        if (mode == RefreshMode.INITIALIZATION) {
          _initializationError.value = error
        }
        ClerkResult.unknownFailure(error)
      }
      else -> null
    }

  private suspend fun executeRefresh(
    attempt: RefreshAttempt,
    mode: RefreshMode,
    skipClientId: Boolean,
  ): ClerkResult<Unit, ClerkErrorResponse> {
    return withTimeout((API_TIMEOUT_SECONDS * TIMEOUT_MULTIPLIER)) {
      val (clientResult, environmentResult) = fetchRefreshData(skipClientId)

      if (attempt.expectedConfigurationVersion != configurationVersion || !hasConfigured) {
        return@withTimeout staleConfigurationFailure()
      }

      handleClientResult(clientResult)
      handleEnvironmentResult(environmentResult)

      when {
        clientResult is ClerkResult.Success && environmentResult is ClerkResult.Success ->
          handleSuccessfulRefresh(
            client = clientResult.value,
            environment = environmentResult.value,
          )
        else ->
          handleRefreshFailure(
            clientResult = clientResult,
            environmentResult = environmentResult,
            mode = mode,
            attempt = attempt,
          )
      }
    }
  }

  private fun staleConfigurationFailure(): ClerkResult.Failure<ClerkErrorResponse> {
    return ClerkResult.unknownFailure(
      IllegalStateException("Clerk configuration changed during refresh")
    )
  }

  private suspend fun fetchRefreshData(
    skipClientId: Boolean
  ): Pair<ClerkResult<Client, ClerkErrorResponse>, ClerkResult<Environment, ClerkErrorResponse>> =
    coroutineScope {
      val clientDeferred = async {
        if (skipClientId) Client.getSkippingClientId() else Client.get()
      }
      val environmentDeferred = async { Environment.get() }
      clientDeferred.await() to environmentDeferred.await()
    }

  private fun handleSuccessfulRefresh(
    client: Client,
    environment: Environment,
  ): ClerkResult<Unit, ClerkErrorResponse> {
    updateClerkState(client, environment)
    _isInitialized.value = true
    _initializationError.value = null

    launchPostRefreshTasks()

    if (Clerk.debugMode) {
      ClerkLog.d("Client and environment refresh completed successfully")
    }

    return ClerkResult.success(Unit)
  }

  private fun launchPostRefreshTasks() {
    scope.launch {
      if (Clerk.session != null) {
        startTokenRefresh()
      }
    }
  }

  private fun handleRefreshFailure(
    clientResult: ClerkResult<Client, ClerkErrorResponse>,
    environmentResult: ClerkResult<Environment, ClerkErrorResponse>,
    mode: RefreshMode,
    attempt: RefreshAttempt,
  ): ClerkResult.Failure<ClerkErrorResponse> {
    val errorMessage =
      "Failed to refresh client and environment -" +
        " client: ${clientResult.javaClass.simpleName}," +
        " environment: ${environmentResult.javaClass.simpleName}"
    ClerkLog.e(errorMessage)

    val failure =
      selectRefreshFailure(
        clientResult = clientResult,
        environmentResult = environmentResult,
        fallbackMessage = errorMessage,
      )

    if (mode == RefreshMode.INITIALIZATION) {
      handleInitializationFailure(
        error = failure.throwable ?: IllegalStateException(errorMessage),
        attempt = attempt,
      )
    }

    return failure
  }

  private fun selectRefreshFailure(
    clientResult: ClerkResult<Client, ClerkErrorResponse>,
    environmentResult: ClerkResult<Environment, ClerkErrorResponse>,
    fallbackMessage: String,
  ): ClerkResult.Failure<ClerkErrorResponse> {
    return when {
      clientResult is ClerkResult.Failure -> clientResult
      environmentResult is ClerkResult.Failure -> environmentResult
      else ->
        ClerkResult.Failure(
          error = null,
          throwable = IllegalStateException(fallbackMessage),
          errorType = ClerkResult.Failure.ErrorType.UNKNOWN,
        )
    }
  }

  /**
   * Handles initialization failure by setting error state and scheduling retry if within limits.
   *
   * @param error The exception that caused the failure.
   * @param options Configuration options for retry.
   * @param retryCount Current retry attempt number.
   */
  private fun handleInitializationFailure(error: Throwable, attempt: RefreshAttempt) {
    if (!hasConfigured || attempt.expectedConfigurationVersion != configurationVersion) {
      return
    }

    _isInitialized.value = false
    _initializationError.value = error

    if (attempt.retryCount < MAX_INITIALIZATION_RETRIES) {
      scope.launch { retryInitialization(attempt.nextRetry()) }
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
  private suspend fun retryInitialization(attempt: RefreshAttempt) {
    // Exponential backoff: 5s, 10s, 20s
    val delaySeconds =
      BACKOFF_BASE_DELAY_SECONDS * (EXPONENTIAL_BACKOFF_SHIFT shl (attempt.retryCount - 1))
    ClerkLog.d("Retrying initialization in ${delaySeconds}s (attempt ${attempt.retryCount})")

    delay(delaySeconds.seconds)

    if (attempt.expectedConfigurationVersion != configurationVersion) {
      return
    }

    // Retry the initialization
    queueClientAndEnvironmentRefresh(attempt)
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
    queueClientAndEnvironmentRefresh()
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
          refreshClientAndEnvironment(currentRefreshAttempt(), RefreshMode.INITIALIZATION)
        }
      } else if (_isInitialized.value) {
        // Already initialized, but connectivity was restored - refresh data
        if (Clerk.debugMode) {
          ClerkLog.d("Connectivity restored - SDK already initialized, refreshing data")
        }
        scope.launch {
          refreshClientAndEnvironment(currentRefreshAttempt(), RefreshMode.INITIALIZATION)
        }
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
