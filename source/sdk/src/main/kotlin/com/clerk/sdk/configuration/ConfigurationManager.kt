package com.clerk.sdk.configuration

import android.content.Context
import android.util.Base64
import com.clerk.sdk.error.ClerkClientError
import com.clerk.sdk.log.ClerkLog
import com.clerk.sdk.model.client.Client
import com.clerk.sdk.model.environment.Environment
import com.clerk.sdk.network.ClerkApi
import com.clerk.sdk.storage.StorageHelper
import com.clerk.sdk.util.TokenConstants.TOKEN_PREFIX_LIVE
import com.clerk.sdk.util.TokenConstants.TOKEN_PREFIX_TEST
import com.slack.eithernet.ApiResult
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

private const val URL_SSL_PREFIX = "https://"

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
    val baseUrl = extractApiUrl(publishableKey)
    ClerkApi.configure(baseUrl)

    // Check if the API is reachable
    scope.launch(Dispatchers.IO) {
      val clientDeferred = async { Client.get() }
      val environmentDeferred = async { Environment.get() }

      val clientResult = clientDeferred.await()
      val environmentResult = environmentDeferred.await()

      when {
        clientResult is ApiResult.Success && environmentResult is ApiResult.Success -> {
          callback(ClerkConfigurationState.Configured(environmentResult.value, clientResult.value))
        }
        else -> {
          ClerkLog.e("Failed to configure Clerk: $clientResult")
        }
      }
    }
  }

  /**
   * Helper function that extracts the API URL from the publishable key. This is used to initialize
   * the Clerk API and check if the API is reachable. This is done by making a request to the client
   * and environment endpoints.
   */
  private fun extractApiUrl(publishableKey: String): String {
    val prefixRemoved =
      publishableKey
        .removePrefix(TOKEN_PREFIX_TEST)
        .removePrefix(TOKEN_PREFIX_LIVE) // Handles both test and live

    val decodedBytes = Base64.decode(prefixRemoved, Base64.DEFAULT)
    val decodedString = String(decodedBytes)

    return if (decodedString.isNotEmpty()) {
      "$URL_SSL_PREFIX${decodedString.dropLast(1)}"
    } else {
      throw ClerkClientError("Invalid publishable key")
    }
  }
}

sealed interface ClerkConfigurationState {

  data class Configured(val environment: Environment, val client: Client) : ClerkConfigurationState

  object Error : ClerkConfigurationState
}
