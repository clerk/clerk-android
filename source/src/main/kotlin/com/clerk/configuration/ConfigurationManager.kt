package com.clerk.configuration

import android.content.Context
import com.clerk.Clerk
import com.clerk.lifecycle.AppLifecycleListener
import com.clerk.log.ClerkLog
import com.clerk.model.client.Client
import com.clerk.model.environment.Environment
import com.clerk.network.ClerkApi
import com.clerk.network.serialization.ClerkResult
import com.clerk.network.serialization.fold
import com.clerk.storage.StorageHelper
import com.clerk.util.PublishableKeyHelper
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
   * @throws IllegalStateException if called multiple times.
   * @throws IllegalArgumentException if publishableKey format is invalid.
   */
  fun configure(context: Context, publishableKey: String) {
    if (hasConfigured) {
      ClerkLog.w(
        "ConfigurationManager.configure() called multiple times. Ignoring subsequent calls."
      )
      return
    }

    try {
      this.context = WeakReference(context.applicationContext)
      this.publishableKey = publishableKey

      // Initialize storage helper explicitly to ensure it's ready
      StorageHelper.initialize(context.applicationContext)

      // Extract base URL and configure API client
      val baseUrl = PublishableKeyHelper().extractApiUrl(publishableKey)
      ClerkApi.configure(baseUrl, context.applicationContext)

      // Mark as configured before starting async operations
      hasConfigured = true

      // Start initial data refresh
      refreshClientAndEnvironment()

      // Set up lifecycle monitoring for automatic refresh
      AppLifecycleListener.configure {
        if (hasConfigured) {
          refreshClientAndEnvironment()
        }
      }

      ClerkLog.d("ConfigurationManager configured successfully")
    } catch (e: Exception) {
      hasConfigured = false
      ClerkLog.e("Failed to configure ConfigurationManager: ${e.message}")
      throw e
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
  private fun refreshClientAndEnvironment() {
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

    if (Clerk.debugMode) {
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
          updateClerkState(clientResult.value, environmentResult.value)
          _isInitialized.value = true

          if (Clerk.debugMode) {
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

  /** Handles the client API result with appropriate logging. */
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

  /** Handles the environment API result with appropriate logging. */
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
    Clerk.client = client
    Clerk.updateEnvironment(environment)

    if (Clerk.debugMode) {
      ClerkLog.d("Clerk state updated - Client ID: ${client.id}, Sessions: ${client.sessions.size}")
    }
  }

  /**
   * Cleans up resources and resets state.
   *
   * This method should be called when the SDK is being shut down to prevent memory leaks and ensure
   * proper cleanup of background operations.
   */
  internal fun cleanup() {
    context?.clear()
    context = null
    hasConfigured = false
    _isInitialized.value = false

    ClerkLog.d("ConfigurationManager cleaned up")
  }
}
