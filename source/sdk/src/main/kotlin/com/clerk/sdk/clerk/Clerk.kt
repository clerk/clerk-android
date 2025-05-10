@file:Suppress("TooGenericExceptionCaught", "RethrowCaughtException")

package com.clerk.sdk.clerk

import android.os.Environment
import com.clerk.sdk.model.environment.InstanceEnvironmentType
import com.clerk.sdk.model.session.Session
import com.clerk.sdk.model.user.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TOKEN_PREFIX = "pk_live_"

/**
 * This is the main entrypoint class for the clerk package. It contains a number of methods and
 * properties for interacting with the Clerk API.
 */
object Clerk {

  internal val clerkService = ClerkService()

  /** The publishable key from your Clerk Dashboard, used to connect to Clerk. */
  var publishableKey: String = ""
    private set

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

  /** Enable for additional debugging signals. */
  var debugMode: Boolean = false
    private set

  /** The Clerk environment for the instance. */
  var environment = Environment()
    private set

  init {
    // Register lifecycle observer

  }

  /** A getter to see if the Clerk object is ready for use or not. */
  private val _isLoaded = MutableStateFlow(false)
  val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

  /** A getter to see if a Clerk instance is running in production or development mode. */
  val instanceType: InstanceEnvironmentType
    get() =
      if (publishableKey.startsWith(TOKEN_PREFIX)) {
        InstanceEnvironmentType.PRODUCTION
      } else {
        InstanceEnvironmentType.DEVELOPMENT
      }

  /** The Client object for the current device. */
  var client: com.clerk.sdk.model.client.Client? = null
    internal set(value) {
      field = value
      value?.id?.let { clientId ->
        try {
          //          clerkInitializationHelper.saveClientIdToKeychain(clientId)
        } catch (e: Exception) {
          if (debugMode) {
            e.printStackTrace()
          }
        }
      }
    }

  /**
   * Configures the shared clerk instance.
   *
   * @param publishableKey The publishable key from your Clerk Dashboard, used to connect to Clerk.
   * @param debugMode Enable for additional debugging signals.
   */
  fun configure(publishableKey: String, debugMode: Boolean = false) {
    Clerk.publishableKey = publishableKey
    Clerk.debugMode = debugMode
  }
}
