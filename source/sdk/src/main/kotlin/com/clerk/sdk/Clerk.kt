@file:Suppress("TooGenericExceptionCaught", "RethrowCaughtException")

package com.clerk.sdk

import android.content.Context
import android.util.Base64
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.clerk.sdk.error.ClerkClientError
import com.clerk.sdk.model.client.Client
import com.clerk.sdk.model.environment.InstanceEnvironmentType
import com.clerk.sdk.model.session.Session
import com.clerk.sdk.model.user.User
import com.clerk.sdk.storage.StorageHelper
import java.lang.ref.WeakReference

private const val TOKEN_PREFIX_LIVE = "pk_live_"
private const val TOKEN_PREFIX_TEST = "pk_test_"
private const val URL_SSL_PREFIX = "https://"

/**
 * This is the main entrypoint class for the clerk package. It contains a number of methods and
 * properties for interacting with the Clerk API.
 */
object Clerk : DefaultLifecycleObserver {

  // region Configuration Properties

  /** The publishable key from your Clerk Dashboard, used to connect to Clerk. */
  var publishableKey: String = ""
    private set(value) {
      field = value
      if (value.isEmpty()) {
        throw ClerkClientError(
          "Clerk loaded without a publishable key. Please call initialize() with a valid publishable key first."
        )
      }
      extractApiUrl()
    }

  /** Enable for additional debugging signals. */
  var debugMode: Boolean = false
    private set

  /** Stores the frontend API URL extracted from the publishable key */
  var frontendApiUrl: String = ""
    private set(value) {
      field = value

      // We throw above if the publishable key is empty, so this should never be empty.
      if (value.isEmpty()) {
        throw ClerkClientError(
          "Clerk loaded without a publishable key. Please call initialize() with a valid publishable key first."
        )
      }
      ClerkService.initializeApi(value)
      value
    }

  /** The application context. Used to initialize the StorageHelper. */
  var context: WeakReference<Context>? = null
    private set(value) {
      field = value
      value?.get()?.let { context -> StorageHelper.initialize(context) }
    }

  // endregion

  // region State Properties

  /** The Client object for the current device. */
  var client: Client? = null
    internal set(value) {
      field = value
      value?.id?.let { clientId ->
        try {
          // clerkInitializationHelper.saveClientIdToKeychain(clientId)
        } catch (e: Exception) {
          if (debugMode) {
            e.printStackTrace()
          }
        }
      }
    }

  // endregion

  // region Computed Properties

  /**
   * The currently active Session, which is guaranteed to be one of the sessions in Client.sessions.
   * If there is no active session, this field will be nil.
   */
  val session: Session?
    get() = client?.let { c -> c.sessions.firstOrNull { it.id == c.lastActiveSessionId } }

  /**
   * A shortcut to Session.user which holds the currently active User object. If the session is nil,
   * the user field will match.
   */
  val user: User?
    get() = session?.user

  /** Determines the environment type based on the publishable key. */
  val instanceType: InstanceEnvironmentType
    get() =
      if (publishableKey.startsWith(TOKEN_PREFIX_LIVE)) {
        InstanceEnvironmentType.PRODUCTION
      } else {
        InstanceEnvironmentType.DEVELOPMENT
      }

  // endregion

  // region Public Methods

  /**
   * Configures the shared clerk instance.
   *
   * @param context The application context.
   * @param publishableKey The publishable key from your Clerk Dashboard, used to connect to Clerk.
   * @param debugMode Enable for additional debugging signals.
   */
  fun initialize(context: Context, publishableKey: String, debugMode: Boolean = false) {
    this.context = WeakReference(context)
    this.publishableKey = publishableKey
    this.debugMode = debugMode
    ProcessLifecycleOwner.get().lifecycle.addObserver(this)
  }

  // region Lifecycle observer

  /**
   * Called when the lifecycle owner is started, on every app foreground the Clerk sdk:
   * - Refresh the client object
   * - Refresh the Environment object (internal API)
   * - Starts polling for short-lived session token refresh
   */
  override fun onStart(owner: LifecycleOwner) {
    super.onStart(owner)
  }

  /**
   * Called when the lifecycle owner is stopped, on every app background the Clerk sdk:
   * - Stops polling for short-lived session token refresh
   */
  override fun onStop(owner: LifecycleOwner) {
    super.onStop(owner)
  }

  // endregion

  // region Private Methods

  /** Extracts and sets the frontend API URL from the publishable key. */
  private fun extractApiUrl() {
    val liveRegex = "$TOKEN_PREFIX_LIVE(.+)".toRegex()
    val testRegex = "$TOKEN_PREFIX_TEST(.+)".toRegex()

    val match =
      liveRegex.find(publishableKey)?.groupValues?.get(1)
        ?: testRegex.find(publishableKey)?.groupValues?.get(1)

    match?.let {
      val apiUrl = Base64.decode(it.toByteArray(), Base64.DEFAULT)
      frontendApiUrl = "$URL_SSL_PREFIX${apiUrl.dropLast(1)}"
    }
  }

  // endregion
}
