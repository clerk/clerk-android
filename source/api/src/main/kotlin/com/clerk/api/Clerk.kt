package com.clerk.api

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import com.clerk.api.Clerk.activeSession
import com.clerk.api.Clerk.activeUser
import com.clerk.api.Clerk.initialize
import com.clerk.api.Clerk.isInitialized
import com.clerk.api.Clerk.session
import com.clerk.api.Clerk.user
import com.clerk.api.attestation.DeviceAttestationHelper
import com.clerk.api.auth.Auth
import com.clerk.api.configuration.ConfigurationManager
import com.clerk.api.configuration.PublishableKeyHelper
import com.clerk.api.externalaccount.ExternalAccountService
import com.clerk.api.locale.LocaleProvider
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.InstanceEnvironmentType
import com.clerk.api.network.model.environment.UserSettings
import com.clerk.api.network.model.environment.enabledFirstFactorAttributes
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.factor.isResetFactor
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.session.Session
import com.clerk.api.session.SessionTokensCache
import com.clerk.api.signin.SignIn
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.SSOService
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import com.clerk.api.ui.ClerkTheme
import com.clerk.api.user.User
import com.clerk.sdk.BuildConfig
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
@Suppress("TooManyFunctions")
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

  /** The current foreground activity used for Credential Manager operations. */
  internal var currentActivity: WeakReference<Activity>? = null

  private var trackedApplication: Application? = null

  private val activityLifecycleCallbacks =
    object : Application.ActivityLifecycleCallbacks {
      override fun onActivityCreated(activity: Activity, savedInstanceState: android.os.Bundle?) {
        currentActivity = WeakReference(activity)
      }

      override fun onActivityStarted(activity: Activity) {
        currentActivity = WeakReference(activity)
      }

      override fun onActivityResumed(activity: Activity) {
        currentActivity = WeakReference(activity)
      }

      override fun onActivityPaused(activity: Activity) = Unit

      override fun onActivityStopped(activity: Activity) {
        if (currentActivity?.get() == activity && !activity.isChangingConfigurations) {
          currentActivity = null
        }
      }

      override fun onActivitySaveInstanceState(activity: Activity, outState: android.os.Bundle) =
        Unit

      override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity?.get() == activity) {
          currentActivity = null
        }
      }
    }

  internal var applicationId: String? = null

  private val _multiSessionModeIsEnabled = MutableStateFlow(false)

  /**
   * Reactive state indicating whether this Clerk instance allows multiple sessions on the same
   * client.
   *
   * When enabled, a client can hold sessions for multiple accounts and switch the active session by
   * updating [Client.lastActiveSessionId].
   */
  val multiSessionModeIsEnabledFlow: StateFlow<Boolean> = _multiSessionModeIsEnabled.asStateFlow()

  /** Internal environment configuration containing display settings and authentication options. */
  internal var environment: Environment? = null

  /**
   * The Client object representing the current device and its authentication state.
   *
   * Contains information about active sessions, sign-in attempts, and device-specific data. This is
   * initialized after the SDK's `initialize` method has been successfully called.
   */
  lateinit var client: Client
    private set

  /** Internal property to check if the client has been initialized. */
  internal val clientInitialized: Boolean
    get() = ::client.isInitialized

  /**
   * Reactive state indicating whether the Clerk SDK has completed initialization.
   *
   * Observe this StateFlow to know when the SDK is ready for authentication operations. The SDK
   * must be initialized by calling [initialize] before most other methods can be used. Emits `true`
   * once initialization is complete, `false` otherwise.
   */
  val isInitialized: StateFlow<Boolean> = configurationManager.isInitialized

  /**
   * The publishable key from Clerk Dashboard used for API authentication.
   *
   * This key determines the API base URL and connects the app to the correct Clerk instance.
   */
  var publishableKey: String? = null

  /**
   * Whether to enable telemetry for the SDK.
   *
   * When enabled, the SDK may collect anonymous diagnostic and usage data to help improve Clerk's
   * products and services. This data does not include any personal identifiable information (PII).
   *
   * Set this via [ClerkConfigurationOptions] in the [initialize] method. Defaults to `true`.
   */
  var telemetryEnabled: Boolean = true

  /**
   * The name of the application, as configured in the Clerk Dashboard.
   *
   * Used for display purposes in authentication UI and other contexts. Returns `null` if the SDK is
   * not yet initialized or the application name is not set.
   */
  /**
   * Reactive state for initialization errors.
   *
   * Observe this StateFlow to detect when initialization has failed. When combined with
   * [isInitialized], this allows apps to handle initialization failures gracefully.
   *
   * Emits the last error that occurred during initialization, or null if initialization succeeded
   * or hasn't been attempted yet.
   *
   * Example usage:
   * ```kotlin
   * combine(Clerk.isInitialized, Clerk.initializationError) { initialized, error ->
   *     when {
   *         initialized -> UiState.Ready
   *         error != null -> UiState.InitializationFailed(error)
   *         else -> UiState.Loading
   *     }
   * }
   * ```
   */
  val initializationError: StateFlow<Throwable?> = configurationManager.initializationError

  val applicationName: String?
    get() = environment?.displayConfig?.applicationName

  /**
   * The current version of the Clerk Android SDK.
   *
   * @return A string representing the semantic version of the SDK (e.g., "1.0.0").
   */
  val version: String
    get() = BuildConfig.SDK_VERSION

  val instanceEnvironmentType: InstanceEnvironmentType
    get() =
      if (PublishableKeyHelper().isLive(publishableKey = publishableKey))
        InstanceEnvironmentType.PRODUCTION
      else InstanceEnvironmentType.DEVELOPMENT

  /**
   * Indicates whether prebuilt UI components should display the development mode warning.
   *
   * The warning is shown only when the current environment asks for it and the instance is not a
   * production instance.
   */
  val shouldShowDevelopmentModeWarning: Boolean
    get() {
      val displayConfig = environment?.displayConfig ?: return false
      return displayConfig.showDevModeWarning &&
        displayConfig.instanceEnvironmentType != InstanceEnvironmentType.PRODUCTION
    }

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
    get() = environment?.enabledFirstFactorAttributes().orEmpty()

  val isUserNameEnabled: Boolean
    get() = environment?.usernameIsEnabled ?: false

  /**
   * Indicates whether the 'First Name' field is enabled for user profiles.
   *
   * This setting is configured in your Clerk Dashboard under User & Authentication settings.
   *
   * @return `true` if the 'First Name' attribute is enabled, `false` otherwise. Returns `false` if
   *   the SDK is not yet initialized.
   */
  val isFirstNameEnabled: Boolean
    get() = environment?.firstNameIsEnabled ?: false

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
    get() = environment?.lastNameIsEnabled ?: false

  val passwordIsEnabled: Boolean
    get() = environment?.passwordIsEnabled ?: false

  val mfaIsEnabled: Boolean
    get() = environment?.mfaIsEnabled ?: false

  val passkeyIsEnabled: Boolean
    get() = environment?.passkeyIsEnabled ?: false

  val isEmailEnabled: Boolean
    get() = environment?.emailIsEnabled ?: false

  val isPhoneNumberEnabled: Boolean
    get() = environment?.phoneNumberIsEnabled ?: false

  val isEmailImmutable: Boolean
    get() = environment?.emailIsImmutable ?: false

  val isPhoneNumberImmutable: Boolean
    get() = environment?.phoneNumberIsImmutable ?: false

  val isUsernameImmutable: Boolean
    get() = environment?.usernameIsImmutable ?: false

  val deleteSelfIsEnabled: Boolean
    get() = environment?.userSettings?.actions?.deleteSelf ?: false

  val organizationCreationDefaultsIsEnabled: Boolean
    get() = environment?.organizationSettings?.organizationCreationDefaults?.enabled ?: false

  val organizationIsEnabled: Boolean
    get() = environment?.organizationSettings?.enabled ?: false

  val organizationDomainsIsEnabled: Boolean
    get() = environment?.organizationSettings?.domains?.enabled ?: false

  val organizationDomainEnrollmentModes: List<String>
    get() = environment?.organizationSettings?.domains?.enrollmentModes ?: emptyList()

  val organizationAdminDeleteIsEnabled: Boolean
    get() = environment?.organizationSettings?.actions?.adminDelete ?: false

  val organizationSelectionIsForced: Boolean
    get() = environment?.organizationSettings?.forceOrganizationSelection ?: false

  val organizationSlugIsEnabled: Boolean
    get() = environment?.organizationSettings?.slug?.disabled?.not() ?: true

  val organizationDefaultRoleKey: String?
    get() = environment?.organizationSettings?.domains?.defaultRole

  private val _organizationLogoUrlFlow = MutableStateFlow<String?>(null)

  /**
   * Reactive image URL for the application logo used in authentication UI components.
   *
   * Emits `null` until the SDK environment is initialized or when no logo URL is configured.
   */
  val organizationLogoUrlFlow: StateFlow<String?> = _organizationLogoUrlFlow.asStateFlow()

  /**
   * The image URL for the application logo used in authentication UI components.
   *
   * This logo appears in sign-in screens, sign-up flows, and other authentication interfaces. The
   * URL is configured in your Clerk Dashboard under branding settings. Returns `null` if the SDK is
   * not yet initialized or no logo URL is configured.
   */
  val organizationLogoUrl: String?
    get() = environment?.displayConfig?.logoImageUrl

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
    get() = environment?.displayConfig?.googleOneTapClientId != null

  /**
   * Indicates whether Clerk branding is enabled for the application.
   *
   * When enabled, the "Secured by Clerk" badge is displayed in authentication UI components. This
   * setting is configured in your Clerk Dashboard under branding settings.
   *
   * @return `true` if branding is enabled, `false` otherwise. Returns `true` if the SDK is not yet
   *   initialized.
   */
  val isBranded: Boolean
    get() = environment?.displayConfig?.branded ?: true

  /**
   * The URL of the Terms of Service page for the application.
   *
   * This URL is configured in your Clerk Dashboard under legal settings and is used to display a
   * link to the terms of service during sign-up flows when legal consent is required.
   *
   * @return The terms URL if configured, `null` otherwise. Returns `null` if the SDK is not yet
   *   initialized.
   */
  val termsUrl: String?
    get() = environment?.displayConfig?.termsUrl

  /**
   * The URL of the Privacy Policy page for the application.
   *
   * This URL is configured in your Clerk Dashboard under legal settings and is used to display a
   * link to the privacy policy during sign-up flows when legal consent is required.
   *
   * @return The privacy policy URL if configured, `null` otherwise. Returns `null` if the SDK is
   *   not yet initialized.
   */
  val privacyPolicyUrl: String?
    get() = environment?.displayConfig?.privacyPolicyUrl

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
    get() = environment?.mfaPhoneCodeIsEnabled ?: false

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
    get() = environment?.mfaBackupCodeIsEnabled ?: false

  val mfaAuthenticatorAppIsEnabled: Boolean
    get() = environment?.mfaAuthenticatorAppIsEnabled ?: false

  /**
   * Indicates whether this Clerk instance allows multiple sessions on the same client.
   *
   * When enabled, a client can hold sessions for multiple accounts and switch the active session by
   * updating [Client.lastActiveSessionId].
   */
  val multiSessionModeIsEnabled: Boolean
    get() = environment?.authConfig?.singleSessionMode?.not() ?: false

  // endregion

  // region Session Management

  /** Internal mutable state flow for all sessions on the current client. */
  private val _sessions = MutableStateFlow<List<Session>>(emptyList())

  /**
   * Reactive state for all sessions available on the current client.
   *
   * In multi-session mode this may contain sessions for multiple user accounts. The current session
   * is still determined by [Client.lastActiveSessionId] and exposed through [sessionFlow].
   */
  val sessionsFlow: StateFlow<List<Session>> = _sessions.asStateFlow()

  /** Internal mutable state flow for session changes. */
  private val _session = MutableStateFlow<Session?>(null)

  /**
   * Reactive state for the current user session.
   *
   * Observe this StateFlow to react to session changes such as sign-in, sign-out, or session
   * refresh. Emits `null` when no session exists. Note that the session may have any status
   * (active, pending, etc.) - use [Session.status] to check the current state.
   */
  val sessionFlow: StateFlow<Session?> = _session.asStateFlow()

  /**
   * The current user session, regardless of status.
   *
   * Returns the session matching [Client.lastActiveSessionId] from all sessions, including pending
   * sessions. This allows users with pending sessions to see their profile while completing
   * required tasks. Returns `null` when no session exists or if the SDK is not initialized.
   *
   * Note: Sessions with status [Session.SessionStatus.PENDING] cannot issue session tokens.
   * Attempting to call [Session.fetchToken] on a pending session will log a warning and return
   * null.
   *
   * @see activeSession for a session only when status is ACTIVE.
   */
  val session: Session?
    get() = sessionFlow.value

  /**
   * The current session only if its status is ACTIVE.
   *
   * Returns `null` if no session exists, the SDK is not initialized, or if the session status is
   * not ACTIVE (e.g., PENDING).
   *
   * @see session for the session regardless of status.
   */
  val activeSession: Session?
    get() = sessionFlow.value?.takeIf { it.status == Session.SessionStatus.ACTIVE }

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
    get() = sessionFlow.value != null

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
   * The current user, regardless of session status.
   *
   * Returns the user from the current session even if the session status is PENDING. Returns `null`
   * if no session exists or if the SDK is not initialized.
   *
   * @see activeUser for the user only when session status is ACTIVE.
   */
  val user: User?
    get() = userFlow.value

  /**
   * The current user only if the session status is ACTIVE.
   *
   * Returns `null` if no session exists, the SDK is not initialized, or if the session status is
   * not ACTIVE (e.g., PENDING).
   *
   * @see user for the user regardless of session status.
   */
  val activeUser: User?
    get() = activeSession?.user

  /**
   * The current user's membership in the active organization.
   *
   * Returns the membership whose organization matches [Session.lastActiveOrganizationId] on the
   * active session. Returns `null` when there is no active session, no active organization
   * selection, or the user has no matching hydrated organization membership.
   */
  val organizationMembership: OrganizationMembership?
    get() {
      val activeSession = activeSession ?: return null
      val activeOrganizationId = activeSession.lastActiveOrganizationId ?: return null
      return activeSession.user?.organizationMemberships?.firstOrNull {
        it.organization.id == activeOrganizationId
      }
    }

  /**
   * The active organization for the current session.
   *
   * Returns `null` when there is no current session, no active organization selection, or the
   * current user does not have a matching hydrated organization membership.
   */
  val organization: Organization?
    get() = organizationMembership?.organization

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
    get() = environment?.userSettings?.social ?: emptyMap()

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

  // region Auth Namespace

  /**
   * The Auth namespace providing all authentication entry points.
   *
   * Use this property to access sign-in, sign-up, and session management methods with a DSL-style
   * API.
   *
   * ### Example usage:
   * ```kotlin
   * // Sign in with email
   * Clerk.auth.signIn { email = "user@email.com" }
   *
   * // Sign in with password
   * Clerk.auth.signInWithPassword {
   *     identifier = "user@email.com"
   *     password = "secretpassword"
   * }
   *
   * // Sign out
   * Clerk.auth.signOut()
   * ```
   *
   * @see Auth for all available authentication methods.
   */
  val auth: Auth = Auth()

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
    if (configurationManager.isConfigured()) {
      context.findActivityOrNull()?.let { currentActivity = WeakReference(it) }
      configurationManager.configure(
        context = context,
        publishableKey = publishableKey,
        options = options,
      )
      return
    }

    val appContext = context.applicationContext
    this.debugMode = options?.enableDebugMode == true
    this.proxyUrl = options?.proxyUrl
    this.applicationContext = WeakReference(appContext)
    val application = appContext as? Application
    if (application != null && trackedApplication !== application) {
      trackedApplication?.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
      application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
      trackedApplication = application
    }
    // Seed currentActivity from the passed context if it (or a wrapper around
    // it) is an Activity. Without this, callers that initialize() after the
    // host Activity has already passed onResume — e.g. React Native bridges,
    // late-init flows behind a permission gate, or any framework that boots
    // Clerk after activity creation — would see currentActivity stay null
    // until the next OS-driven resume cycle, breaking the first Credential
    // Manager call (Google sign-in, passkeys). Callers without an Activity
    // context can use [attachActivity] instead.
    context.findActivityOrNull()?.let { currentActivity = WeakReference(it) }
    this.customTheme = theme
    this.telemetryEnabled = options?.telemetryEnabled ?: true
    configurationManager.configure(
      context = context,
      publishableKey = publishableKey,
      options = options,
    )
  }

  /**
   * Clears the active Clerk configuration and local runtime state.
   *
   * This is a local SDK reset: it cancels in-process refresh work, clears the configured API
   * client, drops the device token, clears session/user flows, and removes cached authentication
   * state. It does not revoke the server-side session. Call `Clerk.auth.signOut()` first if the
   * current session should also be ended on Clerk's servers.
   *
   * After reset completes, call [initialize] again to configure a new publishable key or proxy URL.
   */
  fun reset() {
    configurationManager.reset()
    StorageHelper.deleteValue(StorageKey.DEVICE_TOKEN)
    clearSessionAndUserState()
    SessionTokensCache.clear()
    SSOService.cancelPendingAuthentication()
    ExternalAccountService.cancelPendingExternalAccountConnection()
    DeviceAttestationHelper.clearCache()
    LocaleProvider.cleanup()
    ClerkApi.reset()
    updateClient(Client())
    environment = null
    publishableKey = null
    baseUrl = ""
    proxyUrl = null
    debugMode = false
    telemetryEnabled = true
    customTheme = null
    applicationId = null
    applicationContext = null
    currentActivity = null
    trackedApplication?.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
    trackedApplication = null
  }

  /**
   * Switches the active Clerk configuration in the current process.
   *
   * This performs a local [reset] and then calls [initialize] with the provided configuration. Like
   * [initialize], the client/environment refresh happens asynchronously; observe [isInitialized] to
   * know when the new configuration is ready.
   */
  fun switchConfiguration(
    context: Context,
    publishableKey: String,
    options: ClerkConfigurationOptions? = null,
    theme: ClerkTheme? = null,
  ) {
    reset()
    initialize(context = context, publishableKey = publishableKey, options = options, theme = theme)
  }

  /**
   * Provides the current foreground [Activity] to Clerk explicitly.
   *
   * Useful for framework integrations — e.g. React Native bridges, plug-in SDKs, or any host that
   * calls [initialize] with a non-Activity [Context] after the host Activity has already passed
   * [Activity.onResume]. In that case the [Application.ActivityLifecycleCallbacks] registered by
   * [initialize] miss the initial resume, leaving Clerk's tracked activity null — which makes the
   * first Credential Manager call (Google sign-in, passkeys) fail with a `MissingActivity` error
   * until the user backgrounds and foregrounds the app.
   *
   * Calling this with the current Activity immediately after [initialize] eliminates that gap.
   * Subsequent activity changes are still observed via the registered lifecycle callbacks; this
   * method only seeds the initial value.
   *
   * @param activity The current foreground Activity. Held as a [WeakReference] so it can still be
   *   garbage-collected when destroyed.
   */
  fun attachActivity(activity: Activity) {
    currentActivity = WeakReference(activity)
  }

  /**
   * Walks a [Context]/[ContextWrapper] chain looking for an [Activity].
   *
   * Returns the first Activity found, or null if the chain bottoms out at the Application context
   * (or any other non-Activity context). Used by [initialize] so callers that pass an Activity (or
   * a wrapper around one) automatically seed [currentActivity].
   */
  private fun Context.findActivityOrNull(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
      if (ctx is Activity) return ctx
      ctx = ctx.baseContext
    }
    return null
  }

  /**
   * Manually triggers a reinitialization attempt after a failed initialization.
   *
   * This method is useful when initialization has failed (e.g., due to network issues when the app
   * was cold-started by a push notification) and you want to retry after conditions have improved.
   *
   * The SDK will automatically retry initialization up to 3 times with exponential backoff, but
   * this method allows manual retries after those automatic attempts have been exhausted.
   *
   * Example usage:
   * ```kotlin
   * // In your ViewModel or Activity
   * if (Clerk.initializationError.value != null && !Clerk.isInitialized.value) {
   *     val started = Clerk.reinitialize()
   *     if (started) {
   *         // Wait for isInitialized to become true
   *     }
   * }
   * ```
   *
   * @return true if reinitialization was started, false if the SDK is not configured or is already
   *   initialized.
   */
  fun reinitialize(): Boolean = configurationManager.reinitialize()

  /**
   * Updates the stored device token and refreshes the native Clerk state.
   *
   * This is the supported way to swap the native device token after [initialize] has already run.
   * The new token is persisted before forcing a fresh client/environment fetch, and that refresh
   * intentionally omits the current in-memory client id header so a stale anonymous client cannot
   * conflict with the newly supplied token.
   *
   * @param deviceToken The non-blank Clerk device token to persist and use for the refresh.
   * @return A [ClerkResult] indicating whether the token sync and refresh succeeded.
   */
  suspend fun updateDeviceToken(deviceToken: String): ClerkResult<Unit, ClerkErrorResponse> =
    configurationManager.updateDeviceToken(deviceToken)

  /**
   * Returns the current device token from encrypted storage, or null if unavailable.
   *
   * This is used by the Expo bridge to sync the native client token with the JS SDK.
   */
  fun getDeviceToken(): String? =
    com.clerk.api.storage.StorageHelper.loadValue(com.clerk.api.storage.StorageKey.DEVICE_TOKEN)

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
    _organizationLogoUrlFlow.value = environment.displayConfig.logoImageUrl
    _multiSessionModeIsEnabled.value = !environment.authConfig.singleSessionMode
  }

  internal fun credentialActivity(): Activity? = currentActivity?.get()

  /**
   * Internal method to update the client and trigger state updates.
   *
   * Called by [ConfigurationManager] when client data is refreshed from the server.
   *
   * @param client The updated client configuration.
   */
  internal fun updateClient(client: Client) {
    this.client = client.withResolvedActiveSession(previousSession = _session.value)
    // Only update state if flows are initialized (not during static initialization)
    try {
      updateSessionAndUserState()
    } catch (e: Exception) {
      ClerkLog.e("${e.message}")
    }
  }

  private fun Client.withResolvedActiveSession(previousSession: Session?): Client {
    val currentActiveSessionId =
      lastActiveSessionId?.takeIf { activeSessionId -> sessions.any { it.id == activeSessionId } }
    val resolvedActiveSessionId =
      currentActiveSessionId
        ?: previousSession?.id?.takeIf { previousSessionId ->
          sessions.any { it.id == previousSessionId }
        }
        ?: if (lastActiveSessionId == null) sessions.singleOrNull()?.id else null
    return if (resolvedActiveSessionId == lastActiveSessionId || resolvedActiveSessionId == null) {
      this
    } else {
      copy(lastActiveSessionId = resolvedActiveSessionId)
    }
  }

  /**
   * Internal method to update session and user state flows.
   *
   * Should be called whenever the client state changes that might affect the current session or
   * user. This method finds the session matching [Client.lastActiveSessionId] regardless of status,
   * allowing users with pending sessions to maintain a "signed in" experience.
   */
  internal fun updateSessionAndUserState() {
    val previousSession = _session.value

    // Find session by ID from all sessions (not just active sessions)
    val currentSessions = if (::client.isInitialized) client.sessions else emptyList()
    val currentSession = currentSessions.firstOrNull { it.id == client.lastActiveSessionId }

    if (currentSession?.status == Session.SessionStatus.PENDING) {
      ClerkLog.w(
        "Session is in pending state. " +
          "The user has tasks to complete before the session can be activated. " +
          "Session tokens cannot be issued for pending sessions."
      )
    }

    _sessions.value = currentSessions
    _session.value = currentSession
    _userFlow.value = currentSession?.user

    if (previousSession != currentSession) {
      auth.send(com.clerk.api.auth.AuthEvent.SessionChanged(currentSession))

      if (previousSession == null) {
        currentSession?.user?.let {
          auth.send(com.clerk.api.auth.AuthEvent.SignedIn(currentSession, it))
        }
      }

      if (previousSession != null && currentSession == null) {
        auth.send(com.clerk.api.auth.AuthEvent.SignedOut)
      }
    }
  }

  /**
   * Internal method to clear session and user state flows.
   *
   * Should be called when signing out to immediately clear local session state.
   */
  internal fun clearSessionAndUserState() {
    val previousSession = _session.value
    _sessions.value = emptyList()
    _session.value = null
    _userFlow.value = null

    if (previousSession != null) {
      auth.send(com.clerk.api.auth.AuthEvent.SessionChanged(null))
      auth.send(com.clerk.api.auth.AuthEvent.SignedOut)
    }
  }

  // endregion
}

/**
 * Data class for enabling extra functionality on the Clerk SDK.
 *
 * @property enableDebugMode If `true`, enables verbose logging for SDK operations and API calls.
 *   Defaults to `false`.
 * @property proxyUrl Optional proxy URL for network requests Your Clerk app's proxy URL. Required
 *   for applications that run behind a reverse proxy. Must be a full URL (for example,
 *   https://proxy.example.com/__clerk).
 */
data class ClerkConfigurationOptions(
  val enableDebugMode: Boolean = false,
  val proxyUrl: String? = null,
  val telemetryEnabled: Boolean = true,
)

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
  this.values
    .filter { it.enabled && it.authenticatable }
    .map { OAuthProvider.fromStrategy(it.strategy) }

fun SignIn.identifyingFirstFactor(strategy: String): Factor? =
  supportedFirstFactors?.firstOrNull { it.strategy == strategy && it.safeIdentifier == identifier }

val SignIn.resetPasswordFactor: Factor?
  get() =
    identifyingFirstFactor(strategy = Constants.Strategy.RESET_PASSWORD_EMAIL_CODE)
      ?: identifyingFirstFactor(strategy = Constants.Strategy.RESET_PASSWORD_PHONE_CODE)
      ?: supportedFirstFactors?.firstOrNull { it.isResetFactor() }
