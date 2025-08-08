package com.clerk.api

import android.content.Context
import com.clerk.api.configuration.ConfigurationManager
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.UserSettings
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.signin.SignIn
import com.clerk.api.signout.SignOutService
import com.clerk.api.signup.SignUp
import com.clerk.api.user.User
import com.clerk.ui.ClerkThemeConfig
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Main entrypoint class for the Clerk SDK.
 *
 * Provides access to authentication state, user information, and core functionality for managing
 * user sessions and sign-in flows.
 */
object Clerk {

  // region Configuration & Initialization

  /** Internal configuration manager responsible for SDK initialization and API client setup. */
  private val configurationManager = ConfigurationManager()

  /**
   * Enable for additional debugging signals and logging.
   *
   * When enabled, provides verbose logging for SDK operations and API calls.
   */
  var debugMode: Boolean = false
    private set

  /**
   * The base URL for the Clerk API.
   *
   * This is the publishable key from your Clerk Dashboard that connects your app to Clerk, Base64
   * decoded.
   */
  internal lateinit var baseUrl: String

  /** Application context used for setting up deep links and SSO Receivers */
  internal var applicationContext: WeakReference<Context>? = null

  internal var applicationId: String? = null

  /** Internal environment configuration containing display settings and authentication options. */
  internal lateinit var environment: Environment

  /**
   * The Client object representing the current device and its authentication state.
   *
   * Contains information about active sessions, sign-in attempts, and device-specific data.
   */
  lateinit var client: Client
    private set

  /**
   * Reactive state indicating whether the Clerk SDK has completed initialization.
   *
   * Observe this StateFlow to know when the SDK is ready for authentication operations. The SDK
   * must be initialized before calling authentication methods.
   */
  val isInitialized: StateFlow<Boolean> = configurationManager.isInitialized

  val applicationName: String?
    get() = if (::environment.isInitialized) environment.displayConfig.applicationName else null

  /**
   * The image URL for the application logo used in authentication UI components.
   *
   * This logo appears in sign-in screens, sign-up flows, and other authentication interfaces. The
   * URL is configured in your Clerk Dashboard under branding settings.
   */
  val logoUrl: String?
    get() = if (::environment.isInitialized) environment.displayConfig.logoImageUrl else null

  // endregion

  // region Session Management

  /** Internal mutable state flow for session changes. */
  private val _session = MutableStateFlow<Session?>(null)

  /**
   * Reactive state for the currently active user session.
   *
   * Observe this StateFlow to react to session changes such as sign-in, sign-out, or session
   * refresh. Emits null when no session is active.
   */
  val sessionFlow: StateFlow<Session?> = _session.asStateFlow()

  /**
   * The currently active user session.
   *
   * Represents an authenticated session and is guaranteed to be one of the sessions in
   * [Client.sessions]. Returns null when no session is active.
   */
  val session: Session?
    get() =
      if (::client.isInitialized) {
        client.activeSessions().firstOrNull { it.id == client.lastActiveSessionId }
      } else null

  /**
   * Indicates whether a user is currently signed in.
   *
   * @return true if there is an active session with a user, false otherwise.
   */
  val isSignedIn: Boolean
    get() = session != null

  // endregion

  // region User Management

  /** Internal mutable state flow for user changes. */
  private val _userFlow = MutableStateFlow<User?>(null)

  /**
   * Reactive state for the currently authenticated user.
   *
   * Observe this StateFlow to react to user changes such as sign-in, sign-out, or profile updates.
   * Emits null when no user is signed in.
   */
  val userFlow: StateFlow<User?> = _userFlow.asStateFlow()

  /** The current user for the active session. */
  val user: User? = session?.user

  // endregion

  // region Authentication Features & Settings

  /**
   * Map of available social authentication providers configured for this application.
   *
   * Each entry contains the provider's strategy identifier (e.g., "oauth_google", "oauth_facebook")
   * and its configuration details. Use these strategy identifiers when initiating OAuth sign-in
   * flows.
   *
   * @return Map where keys are strategy identifiers and values contain provider configuration.
   * @see [SignIn.create] for usage with OAuth authentication.
   */
  val socialProviders: Map<String, UserSettings.SocialConfig>
    get() = if (::environment.isInitialized) environment.userSettings.social else emptyMap()

  /** Indicates whether passkey authentication is enabled for this application. */
  val passkeyIsEnabled: Boolean
    get() = if (::environment.isInitialized) environment.passkeyIsEnabled else false

  /** Indicates whether multi-factor authentication (MFA) is enabled for this application. */
  val mfaIsEnabled: Boolean
    get() = if (::environment.isInitialized) environment.mfaIsEnabled else false

  /** Indicates whether authenticator app MFA is enabled for this application. */
  val mfaAuthenticatorAppIsEnabled: Boolean
    get() = if (::environment.isInitialized) environment.mfaAuthenticatorAppIsEnabled else false

  /** Indicates whether password authentication is enabled for this application. */
  val passwordIsEnabled: Boolean
    get() = if (::environment.isInitialized) environment.passwordIsEnabled else false

  /** Indicates whether username authentication is enabled for this application. */
  val usernameIsEnabled: Boolean
    get() = if (::environment.isInitialized) environment.usernameIsEnabled else false

  /** Indicates whether first name is enabled in user settings for this application. */
  val firstNameIsEnabled: Boolean
    get() = if (::environment.isInitialized) environment.firstNameIsEnabled else false

  /** Indicates whether last name is enabled in user settings for this application. */
  val lastNameIsEnabled: Boolean
    get() = if (::environment.isInitialized) environment.lastNameIsEnabled else false

  // endregion

  // region Theme settings
  /** Clerk theme configuration for customizing the appearance of authentication UI components. */
  var clerkThemeConfig: ClerkThemeConfig? = null

  // endregion

  // region Sign In/Sign Up

  /**
   * The current sign-in attempt, if one is in progress.
   *
   * This represents an ongoing authentication flow and provides access to verification steps and
   * authentication state. Returns null when no sign-in is active.
   */
  val signIn: SignIn?
    get() = if (::client.isInitialized) client.signIn else null

  /**
   * The current sign-up attempt, if one is in progress.
   *
   * This represents an ongoing user registration flow and provides access to verification steps and
   * registration state. Returns null when no sign-up is active.
   */
  val signUp: SignUp?
    get() = if (::client.isInitialized) client.signUp else null

  // endregion

  // region Public Methods
  /**
   * Initializes the Clerk SDK with the provided configuration.
   *
   * This method must be called before using any other Clerk functionality. It configures the API
   * client, initializes local storage, and begins the authentication state setup.
   *
   * @param context The application context used for initialization and storage setup.
   * @param publishableKey The publishable key from your Clerk Dashboard that connects your app to
   *   Clerk.
   * @param options Enable additional options for the Clerk SDK. See [ClerkConfigurationOptions] for
   *   details.
   * @param themeConfig Customize the appearance of Clerk UI components. See [ClerkThemeConfig] for
   * @throws IllegalArgumentException if the publishable key format is invalid.
   */
  fun initialize(
    context: Context,
    publishableKey: String,
    options: ClerkConfigurationOptions? = null,
    themeConfig: ClerkThemeConfig? = null,
  ) {
    this.debugMode = options?.enableDebugMode == true
    configurationManager.configure(
      context = context,
      publishableKey = publishableKey,
      options = options,
    )
    this.applicationContext = WeakReference(context)
    this.applicationId = options?.deviceAttestationOptions?.applicationId
    this.clerkThemeConfig = themeConfig
  }

  /**
   * Signs out the currently authenticated user.
   *
   * This operation removes the active session from both the server and local storage, clearing all
   * cached user data and authentication state.
   *
   * @return A [ClerkResult] indicating success or failure of the sign-out operation.
   */
  suspend fun signOut(): ClerkResult<Unit, ClerkErrorResponse> = SignOutService.signOut()

  // endregion

  // region Internal Methods

  /**
   * Internal method to update the environment configuration.
   *
   * Called by [ConfigurationManager] when environment data is refreshed from the server.
   *
   * @param environment The updated environment configuration.
   */
  internal fun updateEnvironment(environment: Environment) {
    this.environment = environment
  }

  /**
   * Internal method to update the client and trigger state updates.
   *
   * Called by [ConfigurationManager] when client data is refreshed from the server.
   *
   * @param client The updated client configuration.
   */
  internal fun updateClient(client: Client) {
    this.client = client
    // Only update state if flows are initialized (not during static initialization)
    try {
      updateSessionAndUserState()
    } catch (e: Exception) {
      ClerkLog.e("${e.message}")
    }
  }

  /**
   * Internal method to update session and user state flows.
   *
   * Should be called whenever the client state changes that might affect the current session or
   * user.
   */
  internal fun updateSessionAndUserState() {
    val currentSession = if (::client.isInitialized) session else null
    val currentUser = currentSession?.user

    _session.value = currentSession
    _userFlow.value = currentUser
  }

  // endregion
}

/** Data class for enabling extra functionality on the Clerk SDK. */
data class ClerkConfigurationOptions(
  /** Enables verbose logging */
  val enableDebugMode: Boolean = false,
  /** Used only for device attestation. Should be something like: `com.example.app` */
  val deviceAttestationOptions: DeviceAttestationOptions? = null,
)

data class DeviceAttestationOptions(val applicationId: String, val cloudProjectNumber: Long)
