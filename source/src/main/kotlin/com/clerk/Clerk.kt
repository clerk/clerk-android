package com.clerk

import android.content.Context
import com.clerk.configuration.ConfigurationManager
import com.clerk.network.model.client.Client
import com.clerk.network.model.environment.Environment
import com.clerk.network.model.environment.UserSettings
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.serialization.ClerkResult
import com.clerk.session.Session
import com.clerk.signin.SignIn
import com.clerk.signout.SignOutService
import com.clerk.signup.SignUp
import com.clerk.user.User
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.StateFlow

/**
 * Main entrypoint class for the Clerk SDK.
 *
 * Provides access to authentication state, user information, and core functionality for managing
 * user sessions and sign-in flows.
 */
object Clerk {

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
   * The Client object representing the current device and its authentication state.
   *
   * Contains information about active sessions, sign-in attempts, and device-specific data.
   */
  lateinit var client: Client

  /** Internal environment configuration containing display settings and authentication options. */
  internal lateinit var environment: Environment

  /**
   * The base URL for the Clerk API.
   *
   * This is the publishable key from your Clerk Dashboard that connects your app to Clerk, Base64
   * decoded.
   */
  internal lateinit var baseUrl: String

  internal var applicationContext: WeakReference<Context>? = null

  // region Computed Properties

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

  /**
   * Reactive state indicating whether the Clerk SDK has completed initialization.
   *
   * Observe this StateFlow to know when the SDK is ready for authentication operations. The SDK
   * must be initialized before calling authentication methods.
   */
  val isInitialized: StateFlow<Boolean> = configurationManager.isInitialized

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

  /**
   * The currently active user session.
   *
   * Represents an authenticated session and is guaranteed to be one of the sessions in
   * [Client.sessions]. Returns null when no session is active.
   */
  val session: Session?
    get() =
      if (::client.isInitialized) {
        client.sessions.firstOrNull { it.id == client.lastActiveSessionId }
      } else null

  /**
   * The currently authenticated user.
   *
   * Provides access to user profile information, email addresses, phone numbers, and other user
   * data. Returns null when no user is signed in.
   */
  val user: User?
    get() = session?.user

  /**
   * Indicates whether a user is currently signed in.
   *
   * @return true if there is an active session with a user, false otherwise.
   */
  val isSignedIn: Boolean
    get() = session != null

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
   * @param debugMode Enable additional logging and debugging information (default: false).
   * @throws IllegalArgumentException if the publishable key format is invalid.
   */
  fun initialize(context: Context, publishableKey: String, debugMode: Boolean = false) {
    this.debugMode = debugMode
    configurationManager.configure(context, publishableKey)
    this.applicationContext = WeakReference(context)
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

  // endregion
}
