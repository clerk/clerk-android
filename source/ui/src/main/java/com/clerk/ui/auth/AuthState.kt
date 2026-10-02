package com.clerk.ui.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.clerk.api.Clerk
import com.clerk.api.Constants
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.session.Session
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.clerk.ui.auth.biometriccredential.BiometricCredentialEnrollmentPrompt
import com.clerk.ui.core.common.NavigableState
import com.clerk.ui.core.composition.AuthStateProvider
import com.clerk.ui.core.navigation.pop
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

@Stable
@Suppress("TooManyFunctions")
internal class AuthState(
  val mode: AuthMode = AuthMode.SignInOrUp,
  val backStack: NavBackStack<NavKey>,
  private val sharedPreferences: SharedPreferences,
  identifierConfig: AuthIdentifierConfig = AuthIdentifierConfig(),
  organizationLogoUrl: String? = null,
) : NavigableState<AuthDestination> {

  private var appliedIdentifierConfig: AuthIdentifierConfig? = null

  var persistIdentifiers by mutableStateOf(identifierConfig.persistIdentifiers)
    private set

  var unsafeMetadata by mutableStateOf(identifierConfig.unsafeMetadata)
    private set

  var lockPrefilledFields by mutableStateOf(identifierConfig.lockPrefilledFields)
    private set

  var identifierConfigVersion by mutableStateOf(0)
    private set

  private var authStartIdentifierState by
    mutableStateOf(sharedPreferences.getString(AUTH_START_IDENTIFIER_STORAGE_KEY, null).orEmpty())
  private var authStartPhoneNumberState by
    mutableStateOf(sharedPreferences.getString(AUTH_START_PHONE_NUMBER_STORAGE_KEY, null).orEmpty())
  private var initialIdentifierWasPrefilled by mutableStateOf(false)
  private var initialPhoneNumberWasPrefilled by mutableStateOf(false)
  private var initialFirstNameWasPrefilled by mutableStateOf(false)
  private var initialLastNameWasPrefilled by mutableStateOf(false)

  var authStartIdentifier: String
    get() = authStartIdentifierState
    set(value) {
      authStartIdentifierState = value
      persistStoredValue(AUTH_START_IDENTIFIER_STORAGE_KEY, value)
    }

  var authStartPhoneNumber: String
    get() = authStartPhoneNumberState
    set(value) {
      authStartPhoneNumberState = value
      persistStoredValue(AUTH_START_PHONE_NUMBER_STORAGE_KEY, value)
    }

  var lastSubmittedIdentifier by mutableStateOf<String?>(null)

  private var biometricCredentialEnrollmentWasOffered by mutableStateOf(false)

  var organizationLogoUrl by mutableStateOf(organizationLogoUrl)
    private set

  var signInPassword by mutableStateOf("")
  var signInNewPassword by mutableStateOf("")
  var signInConfirmNewPassword by mutableStateOf("")
  var signInBackupCode by mutableStateOf("")

  var signUpFirstName by mutableStateOf("")
  var signUpLastName by mutableStateOf("")
  var signUpPassword by mutableStateOf("")
  var signUpUsername by mutableStateOf("")
  var signUpEmail by mutableStateOf("")
  var signUpPhoneNumber by mutableStateOf("")
  var signUpLegalAccepted by mutableStateOf(false)

  val authStartIdentifierLocked: Boolean
    get() = lockPrefilledFields && initialIdentifierWasPrefilled

  val authStartPhoneNumberLocked: Boolean
    get() = lockPrefilledFields && initialPhoneNumberWasPrefilled

  val signUpFirstNameLocked: Boolean
    get() = lockPrefilledFields && initialFirstNameWasPrefilled

  val signUpLastNameLocked: Boolean
    get() = lockPrefilledFields && initialLastNameWasPrefilled

  override fun navigateTo(destination: NavKey) {
    backStack.add(destination)
  }

  override fun navigateBack() {
    if (backStack.size <= 1) return
    backStack.removeLastOrNull()
    clearSignInCredentialsIfAtRoot()
  }

  override fun clearBackStack() {
    resetToRoot()
  }

  fun navigateToAuthStartForIdentifierEdit() {
    resetToRoot()
  }

  override fun pop(numberOfScreens: Int) {
    backStack.pop(numberOfScreens)
    clearSignInCredentialsIfAtRoot()
  }

  /**
   * Safely resets the back stack to the root entry (AuthStart). This prevents NavDisplay crashes
   * during rapid auth state transitions.
   */
  private fun resetToRoot() {
    if (backStack.size > 1) {
      backStack.pop(backStack.size - 1)
    }
    clearSignInCredentialsIfAtRoot()
  }

  override fun popTo(destination: AuthDestination) {
    val targetIndex = backStack.indexOfLast { it == destination }
    if (targetIndex == -1) return

    val toPop = (backStack.size - 1) - targetIndex
    if (toPop > 0) {
      backStack.pop(toPop) // non-inclusive: leaves `destination` on top
    }
    clearSignInCredentialsIfAtRoot()
  }

  private fun clearSignInCredentialsIfAtRoot() {
    if (backStack.size != 1 || backStack.lastOrNull() != AuthDestination.AuthStart) return

    signInPassword = ""
    signInNewPassword = ""
    signInConfirmNewPassword = ""
    signInBackupCode = ""
  }

  internal fun setToStepForStatus(
    signIn: SignIn,
    session: Session? = signIn.correspondingSession(),
    onAuthComplete: () -> Unit,
  ) {
    route(AuthRoutingInput.SignInStep(signIn, session), onAuthComplete)
  }

  internal fun setToStepForStatus(
    signUp: SignUp,
    session: Session? = signUp.correspondingSession(),
    onAuthComplete: () -> Unit,
  ) {
    route(AuthRoutingInput.SignUpStep(signUp, session), onAuthComplete)
  }

  /** Routes after a session task (or the post-auth enrollment prompt) finished. */
  internal fun handleSessionTaskCompletion(session: Session?, onAuthComplete: () -> Unit) {
    route(AuthRoutingInput.SessionStep(session), onAuthComplete)
  }

  /** Navigates "use another method" from [factor]. */
  fun navigateToAlternativeMethods(factor: Factor, isSecondFactor: Boolean = false) {
    navigateTo(
      if (isSecondFactor) {
        AuthDestination.SignInFactorTwoUseAnotherMethod(currentFactor = factor)
      } else {
        AuthDestination.SignInFactorOneUseAnotherMethod(currentFactor = factor)
      }
    )
  }

  private fun route(input: AuthRoutingInput, onAuthComplete: () -> Unit) {
    val context =
      AuthRoutingContext(
        organizationSelectionIsForced = Clerk.organizationSelectionIsForced,
        lastSubmittedIdentifier = lastSubmittedIdentifier,
      )
    navigate(authNavigationCommand(input, context), onAuthComplete)
  }

  internal fun navigate(command: AuthNavigationCommand, onAuthComplete: () -> Unit) {
    when (command) {
      AuthNavigationCommand.None -> Unit
      AuthNavigationCommand.ResetToRoot -> resetToRoot()
      is AuthNavigationCommand.Push -> backStack.add(command.destination)
      is AuthNavigationCommand.ShowSessionTask ->
        if (backStack.lastOrNull() != command.destination) {
          backStack.add(command.destination)
        }
      is AuthNavigationCommand.ReplaceSessionTask -> {
        if (backStack.lastOrNull().isSessionTaskDestination()) {
          backStack.removeLastOrNull()
        }
        if (backStack.lastOrNull() != command.destination) {
          backStack.add(command.destination)
        }
      }
      is AuthNavigationCommand.CompleteAuth -> {
        if (
          command.offerBiometricEnrollment &&
            offerBiometricCredentialEnrollmentIfNeeded(command.afterSignUp)
        ) {
          return
        }
        onAuthComplete()
      }
    }
  }

  /**
   * Routes to the biometric credential enrollment prompt when it should be offered after a
   * completed auth flow. Returns `true` when the prompt was routed to.
   */
  @Suppress("ReturnCount")
  private fun offerBiometricCredentialEnrollmentIfNeeded(completedWithSignUp: Boolean): Boolean {
    if (biometricCredentialEnrollmentWasOffered) return false
    if (backStack.lastOrNull() == AuthDestination.BiometricCredentialEnrollment) return false
    if (
      !BiometricCredentialEnrollmentPrompt.shouldOffer(
        afterSignUp = completedWithSignUp,
        sharedPreferences = sharedPreferences,
      )
    ) {
      return false
    }

    biometricCredentialEnrollmentWasOffered = true
    BiometricCredentialEnrollmentPrompt.markPromptSeen(sharedPreferences)
    backStack.add(AuthDestination.BiometricCredentialEnrollment)
    return true
  }

  init {
    applyIdentifierConfig(identifierConfig)
  }

  @Suppress("CyclomaticComplexMethod")
  fun applyIdentifierConfig(config: AuthIdentifierConfig) {
    val previousConfig = appliedIdentifierConfig
    if (config == previousConfig) return

    appliedIdentifierConfig = config
    persistIdentifiers = config.persistIdentifiers
    unsafeMetadata = config.unsafeMetadata
    lockPrefilledFields = config.lockPrefilledFields

    val identifierFieldsChanged =
      previousConfig == null ||
        config.initialIdentifier != previousConfig.initialIdentifier ||
        config.initialFirstName != previousConfig.initialFirstName ||
        config.initialLastName != previousConfig.initialLastName ||
        config.persistIdentifiers != previousConfig.persistIdentifiers
    if (!identifierFieldsChanged) return

    identifierConfigVersion++

    if (!config.persistIdentifiers) {
      clearStoredIdentifiers()
      LastUsedIdentifierStorage.clear(sharedPreferences)
    }

    initialFirstNameWasPrefilled = !config.initialFirstName.isNullOrBlank()
    initialLastNameWasPrefilled = !config.initialLastName.isNullOrBlank()
    config.initialFirstName?.let { signUpFirstName = it }
    config.initialLastName?.let { signUpLastName = it }

    val initialIdentifier = config.initialIdentifier
    initialIdentifierWasPrefilled =
      !initialIdentifier.isNullOrBlank() && !initialIdentifier.looksLikePhoneNumber()
    initialPhoneNumberWasPrefilled =
      !initialIdentifier.isNullOrBlank() && initialIdentifier.looksLikePhoneNumber()
    when {
      initialIdentifier != null && initialIdentifier.looksLikePhoneNumber() -> {
        updateAuthStartPhoneNumber(initialIdentifier)
        updateAuthStartIdentifier("")
      }
      initialIdentifier != null -> {
        updateAuthStartIdentifier(initialIdentifier)
        updateAuthStartPhoneNumber("")
      }
      !config.persistIdentifiers -> {
        initialIdentifierWasPrefilled = false
        initialPhoneNumberWasPrefilled = false
        updateAuthStartIdentifier("")
        updateAuthStartPhoneNumber("")
      }
    }
  }

  internal fun updateOrganizationLogoUrl(logoUrl: String?) {
    organizationLogoUrl = logoUrl
  }

  fun storeLastUsedIdentifierType(identifierType: IdentifierType) {
    if (!persistIdentifiers) return
    LastUsedIdentifierStorage.store(sharedPreferences, identifierType)
  }

  val storedIdentifierType: IdentifierType?
    get() = LastUsedIdentifierStorage.retrieve(sharedPreferences)

  private fun updateAuthStartIdentifier(value: String) {
    authStartIdentifierState = value
    persistStoredValue(AUTH_START_IDENTIFIER_STORAGE_KEY, value)
  }

  private fun updateAuthStartPhoneNumber(value: String) {
    authStartPhoneNumberState = value
    persistStoredValue(AUTH_START_PHONE_NUMBER_STORAGE_KEY, value)
  }

  private fun persistStoredValue(key: String, value: String) {
    if (!persistIdentifiers) return
    sharedPreferences.edit().putString(key, value).commit()
  }

  private fun clearStoredIdentifiers() {
    sharedPreferences
      .edit()
      .remove(AUTH_START_IDENTIFIER_STORAGE_KEY)
      .remove(AUTH_START_PHONE_NUMBER_STORAGE_KEY)
      .commit()
  }
}

internal data class AuthIdentifierConfig(
  val initialIdentifier: String? = null,
  val initialFirstName: String? = null,
  val initialLastName: String? = null,
  val lockPrefilledFields: Boolean = false,
  val persistIdentifiers: Boolean = true,
  val unsafeMetadata: Map<String, Any>? = null,
)

internal fun authSharedPreferences(context: Context): SharedPreferences {
  return context.applicationContext.getSharedPreferences(
    Constants.Storage.CLERK_PREFERENCES_FILE_NAME,
    Context.MODE_PRIVATE,
  )
}

internal const val AUTH_START_IDENTIFIER_STORAGE_KEY = "authStartIdentifier"

internal const val AUTH_START_PHONE_NUMBER_STORAGE_KEY = "authStartPhoneNumber"

private fun String.looksLikePhoneNumber(): Boolean {
  val input = trim()
  if (input.isEmpty()) return false

  return try {
    val defaultRegion = Locale.getDefault().country.takeIf { it.isNotBlank() } ?: "US"
    val phoneNumberUtil = PhoneNumberUtil.getInstance()
    val parsed = phoneNumberUtil.parse(input, defaultRegion)
    phoneNumberUtil.isPossibleNumber(parsed)
  } catch (_: NumberParseException) {
    false
  }
}

@Composable
internal fun PreviewAuthStateProvider(content: @Composable () -> Unit) {
  val backStack = rememberNavBackStack()
  AuthStateProvider(backStack) { content() }
}
