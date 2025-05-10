@file:Suppress("TooGenericExceptionCaught", "RethrowCaughtException")

package com.clerk.model.clerk

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.clerk.model.client.Client
import com.clerk.model.environment.InstanceEnvironmentType
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TOKEN_PREFIX = "pk_live_"

private const val FIFTY_SECONDS = 50000

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

  /** The event emitter for auth events. */
  val authEventEmitter = EventEmitter<AuthEvent>()

  /** Enable for additional debugging signals. */
  var debugMode: Boolean = false
    private set

  /** The Clerk environment for the instance. */
  var environment = Environment()
    private set

  // MARK: - Private Properties

  /** Frontend API URL. */
  private var frontendApiUrl: String = ""
    set(value) {
      field = value
      apiClient =
        APIClient(
          baseURL = value,
          delegate = ClerkAPIClientDelegate(),
          apiVersion = "2024-10-01",
          sdkVersion = version,
        )
    }

  private var keychainConfig = KeychainConfig()
    set(value) {
      field = value
      keychain =
        SimpleKeychain(
          service = value.service,
          accessGroup = value.accessGroup,
          accessibility = KeychainAccessibility.AFTER_FIRST_UNLOCK_THIS_DEVICE_ONLY,
        )
    }

  /** Holds a reference to the coroutine scope for lifecycle operations. */
  private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  /** Holds a reference to the session polling job. */
  private var sessionPollingJob: Job? = null

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
  fun configure(
    publishableKey: String,
    debugMode: Boolean = false,
    keychainConfig: KeychainConfig = KeychainConfig(),
  ) {
    this.publishableKey = publishableKey
    this.debugMode = debugMode
    this.keychainConfig = keychainConfig

    // Parse frontendApiUrl from publishableKey
    val liveRegex = Regex("pk_live_(.*)")
    val testRegex = Regex("pk_test_(.*)")

    val match =
      liveRegex.find(publishableKey)?.groupValues?.get(1)
        ?: testRegex.find(publishableKey)?.groupValues?.get(1)

    match?.let {
      val decoded = String(Base64.getDecoder().decode(it))
      frontendApiUrl = "https://${decoded.dropLast(1)}"
    }
  }

  /**
   * Loads all necessary environment configuration and instance settings from the Frontend API. It
   * is absolutely necessary to call this method before using the Clerk object in your code.
   */
  suspend fun load() {
    if (publishableKey.trim().isEmpty()) {
      throw ClerkClientError(
        "Clerk loaded without a publishable key. " +
          "Please call configure() with a valid publishable key first."
      )
    }

    startSessionTokenPolling()

    try {
      val clientResult = withContext(Dispatchers.IO) { Client.get() }
      val environmentResult = withContext(Dispatchers.IO) { Environment.get() }

      client = clientResult
      environment = environmentResult

      attestDeviceIfNeeded(environment)

      _isLoaded.value = true
    } catch (e: Exception) {
      throw e
    }
  }

  /**
   * Signs out the active user.
   * - In a **multi-session** application: Signs out the active user from all sessions.
   * - In a **single-session** context: Signs out the active user from the current session.
   * - You can specify a specific session to sign out by passing the `sessionId` parameter.
   *
   * @param sessionId An optional session ID to specify a particular session to sign out. Useful for
   *   multi-session applications.
   * @throws Exception if the sign-out process fails.
   *
   * Example:
   * ```kotlin
   * clerk.signOut()
   * ```
   */
  suspend fun signOut(sessionId: String? = null) {
    clerkService.signOut(sessionId)
  }

  /**
   * A method used to set the active session.
   *
   * Useful for multi-session applications.
   *
   * @param sessionId The session ID to be set as active.
   * @param organizationId The organization ID to be set as active in the current session. If null,
   *   the currently active organization is removed as active.
   */
  suspend fun setActive(sessionId: String, organizationId: String? = null) {
    clerkService.setActive(sessionId, organizationId)
  }

  // Lifecycle methods
  override fun onStart(owner: LifecycleOwner) {
    super.onStart(owner)
    startSessionTokenPolling()

    // Start both functions concurrently
    coroutineScope.launch {
      try {
        Client.get()
      } catch (e: Exception) {
        if (debugMode) {
          e.printStackTrace()
        }
      }
    }

    coroutineScope.launch {
      try {
        environment = Environment.get()
      } catch (e: Exception) {
        if (debugMode) {
          e.printStackTrace()
        }
      }
    }
  }

  override fun onStop(owner: LifecycleOwner) {
    super.onStop(owner)
    stopSessionTokenPolling()
  }

  private fun startSessionTokenPolling() {
    sessionPollingJob?.cancel()
    sessionPollingJob =
      coroutineScope.launch(Dispatchers.IO) {
        while (true) {
          session?.let { currentSession ->
            try {
              currentSession.getToken(TokenOptions(skipCache = true))
            } catch (e: Exception) {
              if (debugMode) {
                e.printStackTrace()
              }
            }
          }
          delay(FIFTY_SECONDS) // 50 seconds
        }
      }
  }

  private fun stopSessionTokenPolling() {
    sessionPollingJob?.cancel()
    sessionPollingJob = null
  }

  private fun attestDeviceIfNeeded(environment: Environment) {
    if (
      !AppAttestHelper.hasKeyId &&
        listOf(DeviceAttestationMode.ONBOARDING, DeviceAttestationMode.ENFORCED)
          .contains(environment.fraudSettings?.native?.deviceAttestationMode)
    ) {
      CoroutineScope(Dispatchers.IO).launch {
        try {
          AppAttestHelper.performDeviceAttestation()
        } catch (e: Exception) {
          if (debugMode) {
            e.printStackTrace()
          }
        }
      }
    }
  }

  companion object {
    /** The shared Clerk instance. */
    val shared: Clerk by lazy { Clerk() }

    /** Current SDK version. */
    const val version = "1.0.0" // Replace with actual version
  }
}

enum class DeviceAttestationMode {
  ONBOARDING,
  ENFORCED,
  DISABLED,
}

class TokenOptions(val skipCache: Boolean = false)

enum class KeychainAccessibility {
  AFTER_FIRST_UNLOCK_THIS_DEVICE_ONLY
}

data class KeychainConfig(
  val service: String = "com.clerk.secure",
  val accessGroup: String? = null,
)

class ClerkClientError(message: String) : Exception(message)

/** Auth events emitted by the Clerk instance */
sealed class AuthEvent {
  /** Emitted when a user signs in */
  data class SignedIn(val userId: String, val sessionId: String) : AuthEvent()

  /** Emitted when a user signs out */
  data class SignedOut(val userId: String?, val sessionId: String?) : AuthEvent()

  /** Emitted when a user's session is created */
  data class SessionCreated(val userId: String, val sessionId: String) : AuthEvent()

  /** Emitted when a user's session is removed */
  data class SessionRemoved(val userId: String?, val sessionId: String) : AuthEvent()
}
