package com.clerk.sdk

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.clerk.sdk.configuration.ClerkConfigurationState
import com.clerk.sdk.configuration.ConfigurationManager
import com.clerk.sdk.log.ClerkLog
import com.clerk.sdk.model.client.Client
import com.clerk.sdk.model.environment.Environment
import com.clerk.sdk.model.session.Session
import com.clerk.sdk.model.user.User

/**
 * This is the main entrypoint class for the clerk package. It contains a number of methods and
 * properties for interacting with the Clerk API.
 */
object Clerk : DefaultLifecycleObserver {

  internal val configurationManager = ConfigurationManager()

  // region Configuration Properties

  /** Enable for additional debugging signals. */
  var debugMode: Boolean = false
    private set

  // endregion

  // region State Properties

  /** The Client object for the current device. */
  internal lateinit var client: Client

  /** The Environment object for the current client */
  internal lateinit var environment: Environment

  // endregion

  // region Computed Properties

  /**
   * The currently active Session, which is guaranteed to be one of the sessions in Client.sessions.
   * If there is no active session, this field will be nil.
   */
  val session: Session?
    get() = client.let { c -> c.sessions.firstOrNull { it.id == c.lastActiveSessionId } }

  /**
   * A shortcut to Session.user which holds the currently active User object. If the session is nil,
   * the user field will match.
   */
  val user: User?
    get() = session?.user

  // endregion

  // region Public Methods

  /**
   * Configures the shared clerk instance.
   *
   * @param context The application context.
   * @param publishableKey The publishable key from your Clerk Dashboard, used to connect to Clerk.
   * @param debugMode Enable for additional logging.
   * @param onInitialized A callback that is called when the configuration is complete. It returns
   *   true if the configuration was successful, and false if there was an error.
   */
  internal fun initialize(
    context: Context,
    publishableKey: String,
    debugMode: Boolean = false,
    onInitialized: (Boolean) -> Unit = {},
  ) {
    this.debugMode = debugMode
    configurationManager.configure(context, publishableKey) { state ->
      when (state) {
        is ClerkConfigurationState.Configured -> {
          this.client = state.client
          this.environment = state.environment
          onInitialized(true)
        }
        ClerkConfigurationState.Error -> {
          ClerkLog.e("Failed to configure Clerk.")
          onInitialized(false)
        }
      }
    }
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

  // endregion
}
