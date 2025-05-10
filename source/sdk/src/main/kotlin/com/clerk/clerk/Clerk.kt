@file:Suppress("TooGenericExceptionCaught", "RethrowCaughtException")

package com.clerk.clerk

import android.os.Environment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.clerk.model.client.Client
import com.clerk.model.environment.InstanceEnvironmentType
import com.clerk.model.session.Session
import com.clerk.model.user.User
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TOKEN_PREFIX = "pk_live_"

/**
 * This is the main entrypoint class for the clerk package. It contains a number of methods and
 * properties for interacting with the Clerk API.
 */
class Clerk private constructor() : DefaultLifecycleObserver {

  internal val clerkService = ClerkService()

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
  var client: Client? = null
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

  /** A dictionary of a user's active sessions on all devices. */
  var sessionsByUserId: MutableMap<String, List<Session>> = ConcurrentHashMap()
    internal set

  /** The publishable key from your Clerk Dashboard, used to connect to Clerk. */
  var publishableKey: String = ""
    private set

  /** Enable for additional debugging signals. */
  var debugMode: Boolean = false
    private set

  /** The Clerk environment for the instance. */
  var environment = Environment()
    private set

  init {
    // Register lifecycle observer
    ProcessLifecycleOwner.get().lifecycle.addObserver(this)
  }

  /**
   * Configures the shared clerk instance.
   *
   * @param publishableKey The publishable key from your Clerk Dashboard, used to connect to Clerk.
   * @param debugMode Enable for additional debugging signals.
   * @param keychainConfig Options that Clerk will use when accessing the keychain.
   */
  fun configure(publishableKey: String, debugMode: Boolean = false) {
    this.publishableKey = publishableKey
    this.debugMode = debugMode
  }
}
