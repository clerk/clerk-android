package com.clerk

import android.content.Context
import com.clerk.configuration.ClerkConfigurationState
import com.clerk.configuration.ConfigurationManager
import com.clerk.log.ClerkLog
import com.clerk.model.client.Client
import com.clerk.model.environment.Environment
import com.clerk.model.environment.UserSettings
import com.clerk.model.session.Session
import com.clerk.model.user.User
import com.clerk.service.SignOutService
import com.clerk.signin.SignIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * This is the main entrypoint class for the clerk package. It contains a number of methods and
 * properties for interacting with the Clerk API.
 */
object Clerk {

  /**
   * The ConfigurationManager object is used to manage the configuration of the Clerk It handles the
   * initialization of the SDK and the configuration of the Clerk API client.
   */
  internal val configurationManager = ConfigurationManager()

  // region Configuration Properties

  /** Enable for additional debugging signals. */
  var debugMode: Boolean = false
    private set

  // endregion

  // region State Properties

  /** The Client object for the current device. */
  lateinit var client: Client

  /** The Environment object for the current client */
  private lateinit var environment: Environment

  /**
   * The image URL for the logo to be used in the UI. This is the URL of the logo image that will be
   * used in things like the sign-in screen and the sign-up screen.
   */
  val logoUrl: String
    get() = environment.displayConfig.logoImageUrl

  /**
   * Gets the map of available social providers configured in the Clerk environment.
   *
   * This map contains the strategies for each social provider, which can be used to identify OAuth
   * providers when initiating a sign-in process. The keys are the strategy identifiers (e.g.,
   * "oauth_google"), and the values provide configuration details for each provider.
   *
   * Use this to obtain the available social providers and their respective strategy names when
   * constructing [SignIn.create] with an OAuth identifier.
   */
  val socialProviders: Map<String, UserSettings.SocialConfig>
    get() = environment.userSettings.social

  public val isInitialized: StateFlow<Boolean> = configurationManager.isInitialized.asStateFlow()

  public var signIn: SignIn? = null
    get() = client.signIn

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
  fun initialize(
    context: Context,
    publishableKey: String,
    debugMode: Boolean = false,
    onInitialized: (Boolean) -> Unit = {},
  ) {
    this.debugMode = debugMode
    configurationManager.configure(context, publishableKey) { state ->
      when (state) {
        is ClerkConfigurationState.Success -> {
          if (debugMode) {
            ClerkLog.d(
              "Clerk configured successfully: client: ${state.client}, environment: ${state.environment}"
            )
          }
          this.client = state.client
          this.environment = state.environment
          onInitialized(true)
        }
        ClerkConfigurationState.Error -> {
          ClerkLog.e("Failed to configure Clerk")
          onInitialized(false)
        }
      }
    }
  }

  /**
   * Signs the current user out of the application. This will remove the current session and clear
   * any cached user data.
   */
  fun signOut(): Flow<SignOutService.SignOutState> = SignOutService.signOut()
}
