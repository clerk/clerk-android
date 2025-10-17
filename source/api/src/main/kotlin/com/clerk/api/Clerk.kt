package com.clerk.api

import android.content.Context
import com.clerk.api.Clerk.initialize
import com.clerk.api.Clerk.isInitialized
import com.clerk.api.Clerk.sessionFlow
import com.clerk.api.Clerk.userFlow
import com.clerk.api.configuration.ConfigurationManager
import com.clerk.api.locale.LocaleProvider
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.UserSettings
import com.clerk.api.network.model.environment.enabledFirstFactorAttributes
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.signin.SignIn
import com.clerk.api.signout.SignOutService
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.ui.ClerkTheme
import com.clerk.api.user.User
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
   *
   * Set this via [ClerkConfigurationOptions] in the [initialize] method. When enabled, provides
   * verbose logging for SDK operations and API calls. Defaults to `false`.
   */
  var debugMode: Boolean = false
    private set

  /**
   * Optional proxy URL for network requests.
   *
   * Your Clerk app's proxy URL. Required for applications that run behind a reverse proxy. Must be
   * a full URL (for example, https://proxy.example.com/__clerk. Set this via
   * [ClerkConfigurationOptions] in the [initialize] method.
   */
  var proxyUrl: String? = null
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
   * Contains information about active sessions, sign-in attempts, and device-specific data. This is
   * initialized after the SDK's `initialize` method has been successfully called.
   */
  lateinit var client: Client
    private set

  /**
   * Reactive state indicating whether the Clerk SDK has completed initialization.
   *
   * Observe this StateFlow to know when the SDK is ready for authentication operations. The SDK
   * must be initialized by calling [initialize] before most other methods can be used. Emits `true`
   * once initialization is complete, `false` otherwise.
   */
  val isInitialized: StateFlow<Boolean> = configurationManager.isInitialized

  /**
   * The name of the application, as configured in the Clerk Dashboard.
   *
   * Used for display purposes in authentication UI and other contexts. Returns `null` if the SDK is
   * not yet initialized or the application name is not set.
   */
  val applicationName: String?
    get() = if (::environment.isInitialized) environment.displayConfig.applicationName else null

  /**
   * A list of enabled first factor attributes, sorted by priority.
   *
   * These attributes represent the primary identification methods available for users during
   * sign-in or sign-up. Examples include email address, phone number, or username. The order in
   * this list reflects the preferred order for presenting these options in the UI.
   *
   * @return A list of strings, each representing an enabled first factor attribute. Returns an
   *   empty list if the SDK is not initialized or if no first factor attributes are enabled.
   */
  val enabledFirstFactorAttributes: List<String>
    get() =
      if (::environment.isInitialized) environment.enabledFirstFactorAttributes() else emptyList()

  /**
   * Indicates whether the 'First Name' field is enabled for user profiles.
   *
   * This setting is configured in your Clerk Dashboard under User & Authentication settings.
   *
   * @return `true` if the 'First Name' attribute is enabled, `false` otherwise. Returns `false` if
   *   the SDK is not yet initialized.
   */
  val isFirstNameEnabled: Boolean
    get() =
      if (::environment.isInitialized) environment.userSettings.attributes.contains("first_name")
      else false

  /**
   * Indicates whether the last name field is enabled for user profiles.
   *
   * This setting is configured in your Clerk Dashboard under user attributes. If enabled, the SDK
   * may prompt for or display the last name in user profiles and authentication flows.
   *
   * @return `true` if the last name attribute is enabled, `false` otherwise. Returns `false` if the
   *   SDK is not yet initialized.
   */
  val isLastNameEnabled: Boolean
    get() =
      if (::environment.isInitialized) environment.userSettings.attributes.contains("last_name")
      else false

  /**
   * The image URL for the application logo used in authentication UI components.
   *
   * This logo appears in sign-in screens, sign-up flows, and other authentication interfaces. The
   * URL is configured in your Clerk Dashboard under branding settings. Returns `null` if the SDK is
   * not yet initialized or no logo URL is configured.
   */
  val organizationLogoUrl: String?
    get() = if (::environment.isInitialized) environment.displayConfig.logoImageUrl else null

  /**
   * Indicates whether Google One Tap sign-in is enabled for the application.
   *
   * Google One Tap provides a streamlined sign-in experience for users with existing Google
   * accounts. This property returns `true` if Google One Tap is configured with a client ID in the
   * Clerk Dashboard, and `false` otherwise.
   *
   * @return `true` if Google One Tap is enabled, `false` otherwise. Returns `false` if the SDK is
   *   not yet initialized.
   */
  val isGoogleOneTapEnabled: Boolean
    get() =
      if (::environment.isInitialized) environment.displayConfig.googleOneTapClientId != null
      else false

  /**
   * Indicates whether MFA (Multi-Factor Authentication) via phone code is enabled.
   *
   * This setting is configured in your Clerk Dashboard. If enabled, users can add a phone number as
   * a second authentication factor.
   *
   * @return `true` if MFA with phone code is enabled, `false` otherwise. Returns `false` if the SDK
   *   is not yet initialized.
   */
  val mfaPhoneCodeIsEnabled: Boolean
    get() = if (::environment.isInitialized) environment.mfaPhoneCodeIsEnabled else false

  /**
   * Indicates whether MFA (Multi-Factor Authentication) via backup codes is enabled.
   *
   * This setting is configured in your Clerk Dashboard. If enabled, users can generate and use
   * single-use backup codes as a second authentication factor.
   *
   * @return `true` if MFA with backup codes is enabled, `false` otherwise. Returns `false` if the
   *   SDK is not yet initialized.
   */
  val mfaBackupCodeIsEnabled: Boolean
    get() = if (::environment.isInitialized) environment.mfaBackupCodeIsEnabled else false

  // endregion

  // region Session Management

  /** Internal mutable state flow for session changes. */
  private val _session = MutableStateFlow<Session?>(null)

  /**
   * Reactive state for the currently active user session.
   *
   * Observe this StateFlow to react to session changes such as sign-in, sign-out, or session
   * refresh. Emits `null` when no session is active.
   */
  val sessionFlow: StateFlow<Session?> = _session.asStateFlow()

  /**
   * The currently active user session.
   *
   * Represents an authenticated session and is guaranteed to be one of the sessions in
   * [Client.sessions]. Returns `null` when no session is active or if the SDK is not initialized.
   */
  val session: Session?
    get() =
      if (::client.isInitialized) {
        client.activeSessions().firstOrNull { it.id == client.lastActiveSessionId }
      } else null

  /**
   * The active locale for the current session.
   *
   * This is used to determine the language of the UI components and the emails sent to the user.
   * The value is a IETF BCP 47 language tag, e.g., "en-US".
   */
  val locale: StateFlow<String?> = LocaleProvider.locale

  /**
   * Indicates whether a user is currently signed in.
   *
   * @return `true` if there is an active session with a user, `false` otherwise.
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
   * Emits `null` when no user is signed in.
   */
  val userFlow: StateFlow<User?> = _userFlow.asStateFlow()

  /**
   * The current user for the active session.
   *
   * Returns `null` if no session is active or if the SDK is not initialized.
   */
  val user: User?
    get() = session?.user

  // endregion

  // region Authentication Features & Settings

  /**
   * Map of available social authentication providers configured for this application.
   *
   * Each entry contains the provider's strategy identifier (e.g., "oauth_google", "oauth_facebook")
   * and its configuration details. Use these strategy identifiers when initiating OAuth sign-in
   * flows.
   *
   * @return Map where keys are strategy identifiers (e.g., `oauth_google`) and values contain
   *   provider configuration. Returns an empty map if the SDK is not initialized or no social
   *   providers are configured.
   * @see [SignIn.create] for usage with OAuth authentication.
   */
  val socialProviders: Map<String, UserSettings.SocialConfig>
    get() = if (::environment.isInitialized) environment.userSettings.social else emptyMap()

  // endregion

  // region Theme settings
  /**
   * Clerk theme configuration for customizing the appearance of authentication UI components.
   *
   * Set this property during SDK initialization or at any time to apply a custom theme. See
   * [ClerkTheme] for details on available customizations. If `null`, default theming will be
   * applied.
   */
  var customTheme: ClerkTheme? = null

  // endregion

  /**
   * Sets the active session and optionally the active organization for that session.
   *
   * This is useful for applications that support multiple user sessions or organizations. Calling
   * this method will attempt to update the active session on the Clerk backend.
   *
   * @param sessionId The ID of the session to be set as active.
   * @param organizationId The ID of the organization to be set as active for the current session.
   *   If `null`, the currently active organization (if any) is removed as active.
   * @return A [ClerkResult] which is a [ClerkResult.Success] containing the updated [Session] on
   *   success, or a [ClerkResult.Failure] containing a [ClerkErrorResponse] on failure.
   */
  suspend fun setActive(
    sessionId: String,
    organizationId: String? = null,
  ): ClerkResult<Session, ClerkErrorResponse> {
    return ClerkApi.client.setActive(sessionId, organizationId)
  }

  // region Sign In/Sign Up

  /**
   * The current sign-in attempt, if one is in progress.
   *
   * This represents an ongoing authentication flow and provides access to verification steps and
   * authentication state. Returns `null` when no sign-in is active or if the SDK is not
   * initialized.
   */
  val signIn: SignIn?
    get() = if (::client.isInitialized) client.signIn else null

  /**
   * The current sign-up attempt, if one is in progress.
   *
   * This represents an ongoing user registration flow and provides access to verification steps and
   * registration state. Returns `null` when no sign-up is active or if the SDK is not initialized.
   */
  val signUp: SignUp?
    get() = if (::client.isInitialized) client.signUp else null

  // endregion

  // region Public Methods

  /**
   * Initializes the Clerk SDK with the provided configuration.
   *
   * This method must be called before using any other Clerk functionality. It configures the API
   * client, initializes local storage, and begins the authentication state setup. Observe
   * [isInitialized] to know when the SDK is ready.
   *
   * @param context The application context used for initialization and storage setup.
   * @param publishableKey The publishable key from your Clerk Dashboard that connects your app to
   *   Clerk.
   * @throws IllegalArgumentException if the publishable key format is invalid.
   */
  fun initialize(context: Context, publishableKey: String) {
    initialize(context, publishableKey, null)
  }

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
   *   details. Clerk. This key should be in the format `pk_test_...` or `pk_live_...`.
   * @param options Optional configuration for enabling extra functionality. See
   *   [ClerkConfigurationOptions] for details.
   * @param theme Optional theme to customize the appearance of Clerk UI components. See
   *   [ClerkTheme] for details.
   * @throws IllegalArgumentException if the publishable key format is invalid.
   */
  fun initialize(
    context: Context,
    publishableKey: String,
    options: ClerkConfigurationOptions? = null,
    theme: ClerkTheme? = null,
  ) {
    this.debugMode = options?.enableDebugMode == true
    this.proxyUrl = options?.proxyUrl
    this.applicationContext = WeakReference(context)
    this.applicationId = options?.deviceAttestationOptions?.applicationId
    this.customTheme = theme
    configurationManager.configure(
      context = context,
      publishableKey = publishableKey,
      options = options,
    )
  }

  /**
   * Signs out the currently authenticated user.
   *
   * This operation removes the active session from both the server and local storage, clearing all
   * cached user data and authentication state. [sessionFlow] and [userFlow] will emit `null`.
   *
   * @return A [ClerkResult.Success] with `Unit` on successful sign-out, or a [ClerkResult.Failure]
   *   containing a [ClerkErrorResponse] if an error occurs.
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

/**
 * Data class for enabling extra functionality on the Clerk SDK.
 *
 * @property enableDebugMode If `true`, enables verbose logging for SDK operations and API calls.
 *   Defaults to `false`.
 * @property deviceAttestationOptions Configuration for Android Play Integrity device attestation.
 *   Used to enhance security. Defaults to `null` (device attestation disabled).
 * @property proxyUrl Optional proxy URL for network requests Your Clerk app's proxy URL. Required
 *   for applications that run behind a reverse proxy. Must be a full URL (for example,
 *   https://proxy.example.com/__clerk).
 */
data class ClerkConfigurationOptions(
  val enableDebugMode: Boolean = false,
  val deviceAttestationOptions: DeviceAttestationOptions? = null,
  val proxyUrl: String? = null,
)

/**
 * Configuration options for Android Play Integrity device attestation.
 *
 * @property applicationId The application ID (package name) of your app (e.g., `com.example.app`).
 * @property cloudProjectNumber Your Google Cloud Project number, required for Play Integrity API.
 */
data class DeviceAttestationOptions(val applicationId: String, val cloudProjectNumber: Long)

/**
 * Extension function to convert a map of social provider configurations into a list of
 * [OAuthProvider] objects.
 *
 * This is useful for easily iterating over available OAuth providers for UI display or other logic.
 *
 * @return A list of [OAuthProvider] enums corresponding to the enabled social providers.
 * @receiver A map where keys are strategy identifiers (e.g., `oauth_google`) and values are
 *   [UserSettings.SocialConfig].
 */
fun Map<String, UserSettings.SocialConfig>.toOAuthProvidersList(): List<OAuthProvider> =
  this.map { OAuthProvider.fromStrategy(it.value.strategy) }
