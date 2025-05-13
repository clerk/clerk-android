package com.clerk.sdk.configuration

import android.content.Context
import com.clerk.sdk.log.ClerkLog
import com.clerk.sdk.model.client.Client
import com.clerk.sdk.model.environment.Environment
import com.clerk.sdk.network.ClerkApi
import com.clerk.sdk.storage.StorageHelper
import com.clerk.sdk.util.PublishableKeyHelper
import com.slack.eithernet.ApiResult
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
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
    ClerkApi.configure(baseUrl)

    // Check if the API is reachable
    scope.launch(Dispatchers.IO) {
      val clientDeferred = async { Client.get() }
      val environmentDeferred = async { Environment.get() }

      val clientResult = clientDeferred.await()
      val environmentResult = environmentDeferred.await()

      ClerkLog.d("Client result: $clientResult")
      ClerkLog.d("Environment result: $environmentResult")
      when {
        clientResult is ApiResult.Success && environmentResult is ApiResult.Success -> {
          clientResult.value.client?.let { client ->
            callback(
              ClerkConfigurationState.Configured(
                environment = environmentResult.value,
                client = client,
              )
            )
          }
        }
        else -> {
          ClerkLog.e("Failed to configure Clerk: $environmentResult")
        }
      }
    }
  }
}

sealed interface ClerkConfigurationState {

  data class Configured(val environment: Environment, val client: Client) : ClerkConfigurationState

  object Error : ClerkConfigurationState
}
