package com.clerk.sdk.configuration

import android.content.Context
import com.clerk.sdk.Clerk
import com.clerk.sdk.lifecycle.AppLifecycleListener
import com.clerk.sdk.log.ClerkLog
import com.clerk.sdk.model.client.Client
import com.clerk.sdk.model.environment.Environment
import com.clerk.sdk.network.ClerkApi
import com.clerk.sdk.network.serialization.ClerkApiResult
import com.clerk.sdk.network.serialization.fold
import com.clerk.sdk.storage.StorageHelper
import com.clerk.sdk.util.PublishableKeyHelper
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Responsible for managing the configuration of Clerk. This includes initializing the Clerk API,
 * checking if the API is reachable, and storing session data across app launches.
 */
class ConfigurationManager {

  /**
   * The coroutine scope used for launching coroutines. This is a SupervisorJob, which means that if
   * one coroutine fails, the others will continue to run. This is useful for handling multiple
   * requests at the same time without cancelling the entire scope.
   */
  private val scope = CoroutineScope(SupervisorJob())

  /** The application context. Used to initialize the StorageHelper. */
  internal var context: WeakReference<Context>? = null
    set(value) {
      field = value
      value?.get()?.let { context -> StorageHelper.initialize(context) }
    }

  internal var isInitialized: MutableStateFlow<Boolean> = MutableStateFlow(false)

  /** The publishable key from your Clerk Dashboard, used to connect to Clerk. */
  internal lateinit var publishableKey: String

  /**
   * This configures Clerk for authenticating requests and initializes the StorageHelper for storing
   * session data across app launches
   *
   * @param context The application context.
   * @param publishableKey The publishable key from your Clerk Dashboard, used to connect to Clerk.
   * @param callback A callback that returns true if the configuration was successful, false
   *   otherwise.
   */
  fun configure(
    context: Context,
    publishableKey: String,
    callback: ((ClerkConfigurationState) -> Unit),
  ) {
    this.context = WeakReference(context)
    this.publishableKey = publishableKey
    StorageHelper.initialize(context)

    // Initialize the clerk api
    val baseUrl = PublishableKeyHelper().extractApiUrl(publishableKey)
    ClerkApi.configure(baseUrl, context)

    // Check if the API is reachable
    refreshClientAndEnvironment(callback)
    AppLifecycleListener.configure { refreshClientAndEnvironment(callback) }
  }

  /**
   * Refreshes the client and environment by making asynchronous requests to the Clerk API. This
   * method uses coroutines to make the requests in parallel and waits for both to complete before
   * calling the callback with the results.
   */
  private fun refreshClientAndEnvironment(callback: (ClerkConfigurationState) -> Unit) {
    if (Clerk.debugMode) {
      ClerkLog.d("Refreshing client and environment.")
    }
    scope.launch(Dispatchers.IO) {
      val clientDeferred = async { Client.get() }
      val environmentDeferred = async { Environment.get() }

      val clientResult = clientDeferred.await()
      val environmentResult = environmentDeferred.await()

      clientResult.fold(
        onSuccess = { ClerkLog.d("Client result: $it") },
        onFailure = {
          ClerkLog.e("Error getting client: ${it.error}")
          when (it.errorType) {
            ClerkApiResult.Failure.ErrorType.API -> ClerkLog.e("API error: ${it.error}")
            ClerkApiResult.Failure.ErrorType.HTTP -> ClerkLog.e("HTTP error: ${it.error}")
            ClerkApiResult.Failure.ErrorType.UNKNOWN -> ClerkLog.e("Unknown error: ${it.error}")
          }
        },
      )
      environmentResult.fold(
        onSuccess = { ClerkLog.d("Environment result: $it") },
        onFailure = {
          ClerkLog.e("Error getting environment: $it")
          when (it.errorType) {
            ClerkApiResult.Failure.ErrorType.API -> ClerkLog.e("API error: ${it.error}")
            ClerkApiResult.Failure.ErrorType.HTTP -> ClerkLog.e("HTTP error: ${it.error}")
            ClerkApiResult.Failure.ErrorType.UNKNOWN -> ClerkLog.e("Unknown error: ${it.error}")
          }
        },
      )

      if (clientResult is ClerkApiResult.Success && environmentResult is ClerkApiResult.Success) {
        ClerkLog.d(
          "Client and environment refreshed successfully. client: ${clientResult.value}," +
            " environment: ${environmentResult.value}"
        )
        callback(
          ClerkConfigurationState.Success(
            client = clientResult.value.response,
            environment = environmentResult.value,
          )
        )
        isInitialized.value = true
      } else {

        ClerkLog.e(
          "Error refreshing client and environment. client: $clientResult, environment: $environmentResult}"
        )
      }
    }
  }
}

sealed interface ClerkConfigurationState {

  data class Success(val environment: Environment, val client: Client?) : ClerkConfigurationState

  object Error : ClerkConfigurationState
}
