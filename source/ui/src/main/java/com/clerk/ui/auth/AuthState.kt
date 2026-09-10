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
import com.clerk.api.Session
import com.clerk.api.SessionTaskKey
import com.clerk.api.SignIn
import com.clerk.api.SignInStatus
import com.clerk.api.SignUp
import com.clerk.api.SignUpStatus
import com.clerk.ui.auth.biometriccredential.BiometricCredentialEnrollmentPrompt
import com.clerk.ui.core.common.NavigableState
import com.clerk.ui.core.composition.AuthStateProvider
import com.clerk.ui.core.navigation.pop
import com.clerk.ui.signup.code.SignUpCodeField
import com.clerk.ui.signup.collectfield.CollectField
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val EMAIL_ADDRESS = "email_address"

private const val PHONE_NUMBER = "phone_number"

private const val PASSWORD = "password"

private const val USERNAME = "username"

@Stable
@Suppress("TooManyFunctions")
internal class AuthState(
  val clerk: Clerk,
  val mode: AuthMode = AuthMode.SignInOrUp,
  val backStack: NavBackStack<NavKey>,
  private val sharedPreferences: SharedPreferences,
  identifierConfig: AuthIdentifierConfig = AuthIdentifierConfig(),
  organizationLogoUrl: String? = null,
) : NavigableState<AuthDestination> {

  var presentationError by mutableStateOf<String?>(null)
    private set

  fun clearPresentationError() {
    presentationError = null
  }

  private val finalization = Mutex()

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

  // Auth start fields
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

  // Sign In
  var signInPassword by mutableStateOf("")
  var signInNewPassword by mutableStateOf("")
  var signInConfirmNewPassword by mutableStateOf("")
  var signInBackupCode by mutableStateOf("")

  // Sign Up
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
    if (targetIndex == -1) return // Not found → no-op

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

  internal suspend fun setToStepForStatus(signIn: SignIn, onAuthComplete: () -> Unit): Boolean =
    com.clerk.ui.core.common
      .runUiOperation {
        finalization.withLock { advanceToStepForStatus(signIn, onAuthComplete = onAuthComplete) }
      }
      .fold(
        onSuccess = { true },
        onFailure = {
          presentationError = it.localizedMessage ?: "The operation could not be completed."
          false
        },
      )

  internal suspend fun setToStepForStatus(signUp: SignUp, onAuthComplete: () -> Unit): Boolean =
    com.clerk.ui.core.common
      .runUiOperation {
        finalization.withLock { advanceToStepForStatus(signUp, onAuthComplete = onAuthComplete) }
      }
      .fold(
        onSuccess = { true },
        onFailure = {
          presentationError = it.localizedMessage ?: "The operation could not be completed."
          false
        },
      )

  private var completedPresentationSessionId: String? = null

  internal fun completePresentation(onAuthComplete: () -> Unit) {
    val session = clerk.session ?: return
    if (
      session.status != com.clerk.api.SessionStatus.Active ||
        clerk.user == null ||
        session.currentTask != null ||
        completedPresentationSessionId == session.id
    )
      return
    completedPresentationSessionId = session.id
    onAuthComplete()
  }

  private suspend fun advanceToStepForStatus(
    signIn: SignIn,
    session: Session? = null,
    onAuthComplete: () -> Unit,
  ) {
    if (
      signIn.status == SignInStatus.Complete &&
        (signIn.createdSessionId == null || clerk.session?.id != signIn.createdSessionId)
    )
      signIn.finalize()
    val resolvedSession = session ?: signIn.correspondingSession(clerk)
    when (signIn.status) {
      SignInStatus.Complete -> {
        handlePostAuthCompletion(
          taskKey = resolvedSession.pendingSessionTaskKey(),
          hasUnresolvedCreatedSession = signIn.createdSessionId != null && resolvedSession == null,
          completedWithSignUp = false,
          onAuthComplete = onAuthComplete,
        )
      }
      SignInStatus.NeedsIdentifier -> resetToRoot()
      SignInStatus.NeedsFirstFactor -> routeToFirstFactorOrHelp(signIn)
      SignInStatus.NeedsSecondFactor -> routeToSecondFactorOrHelp(signIn)
      SignInStatus.NeedsNewPassword -> backStack.add(AuthDestination.SignInSetNewPassword)
      SignInStatus.NeedsClientTrust -> routeToClientTrustOrHelp(signIn)
      else -> backStack.add(AuthDestination.SignInGetHelp)
    }
  }

  private suspend fun handlePostAuthCompletion(
    taskKey: SessionTaskKey?,
    hasUnresolvedCreatedSession: Boolean,
    completedWithSignUp: Boolean,
    onAuthComplete: () -> Unit,
  ) {
    when (
      postAuthCompletionAction(
        taskKey = taskKey,
        hasUnresolvedCreatedSession = hasUnresolvedCreatedSession,
      )
    ) {
      PostAuthCompletionAction.ROUTE_TO_MFA -> routeToSessionTaskMfa()
      PostAuthCompletionAction.ROUTE_TO_RESET_PASSWORD -> routeToSessionTaskResetPassword()
      PostAuthCompletionAction.ROUTE_TO_CHOOSE_ORGANIZATION -> routeToChooseOrganization()
      PostAuthCompletionAction.ROUTE_TO_HELP -> backStack.add(AuthDestination.SignInGetHelp)
      PostAuthCompletionAction.COMPLETE_AUTH -> {
        if (offerBiometricCredentialEnrollmentIfNeeded(completedWithSignUp)) return
        completePresentation(onAuthComplete)
      }
    }
  }

  /**
   * Routes to the biometric credential enrollment prompt when it should be offered after a
   * completed auth flow. Returns `true` when the prompt was routed to.
   */
  @Suppress("ReturnCount")
  private suspend fun offerBiometricCredentialEnrollmentIfNeeded(
    completedWithSignUp: Boolean
  ): Boolean {
    if (biometricCredentialEnrollmentWasOffered) return false
    if (backStack.lastOrNull() == AuthDestination.BiometricCredentialEnrollment) return false
    if (
      !BiometricCredentialEnrollmentPrompt.shouldOffer(
        clerk = clerk,
        afterSignUp = completedWithSignUp,
        sharedPreferences = sharedPreferences,
      )
    ) {
      return false
    }

    biometricCredentialEnrollmentWasOffered = true
    BiometricCredentialEnrollmentPrompt.markPromptSeen(clerk, sharedPreferences)
    backStack.add(AuthDestination.BiometricCredentialEnrollment)
    return true
  }

  private fun routeToSessionTaskMfa() {
    routeToSessionTask(AuthDestination.SessionTaskMfa)
  }

  private fun routeToSessionTaskResetPassword() {
    routeToSessionTask(AuthDestination.SessionTaskResetPassword)
  }

  private fun routeToChooseOrganization() {
    routeToSessionTask(AuthDestination.SessionTaskChooseOrganization)
  }

  private fun routeToSessionTask(destination: NavKey) {
    if (backStack.lastOrNull() != destination) {
      backStack.add(destination)
    }
  }

  private fun routeToFirstFactorOrHelp(signIn: SignIn) {
    signIn.startingFirstFactor(clerk, lastSubmittedIdentifier)?.let {
      backStack.add(AuthDestination.SignInFactorOne(factor = it))
    } ?: backStack.add(AuthDestination.SignInGetHelp)
  }

  private fun routeToSecondFactorOrHelp(signIn: SignIn) {
    signIn.startingSecondFactor?.let { backStack.add(AuthDestination.SignInFactorTwo(factor = it)) }
      ?: backStack.add(AuthDestination.SignInGetHelp)
  }

  private fun routeToClientTrustOrHelp(signIn: SignIn) {
    signIn.startingSecondFactor?.let {
      backStack.add(AuthDestination.SignInClientTrust(factor = it))
    } ?: backStack.add(AuthDestination.SignInGetHelp)
  }

  private suspend fun advanceToStepForStatus(
    signUp: SignUp,
    session: Session? = null,
    onAuthComplete: () -> Unit,
  ) {
    if (
      signUp.status == SignUpStatus.Complete &&
        (signUp.createdSessionId == null || clerk.session?.id != signUp.createdSessionId)
    )
      signUp.finalize()
    val resolvedSession = session ?: signUp.correspondingSession(clerk)
    when (signUp.status) {
      SignUpStatus.Abandoned -> resetToRoot()
      SignUpStatus.MissingRequirements -> handleMissingRequirements(signUp)
      SignUpStatus.Complete -> {
        handlePostAuthCompletion(
          taskKey = resolvedSession.pendingSessionTaskKey(),
          hasUnresolvedCreatedSession = signUp.createdSessionId != null && resolvedSession == null,
          completedWithSignUp = true,
          onAuthComplete = onAuthComplete,
        )
        return
      }
      else -> backStack.add(AuthDestination.SignInGetHelp)
    }
  }

  private fun handleMissingRequirements(signUp: SignUp) {
    val firstFieldToCollect = signUp.firstFieldToCollect
    if (firstFieldToCollect != null) {
      handleFieldCollection(signUp)
      return
    }

    val firstFieldToVerify = signUp.firstFieldToVerify
    if (firstFieldToVerify != null) {
      handleFieldVerification(signUp, firstFieldToVerify)
    }
  }

  private fun handleFieldVerification(signUp: SignUp, fieldToVerify: String) {
    when (fieldToVerify) {
      EMAIL_ADDRESS -> {
        val emailAddress = signUp.emailAddress
        if (emailAddress != null) {
          val destination =
            if (signUp.emailVerificationStrategy(clerk) == "email_link") {
              AuthDestination.SignUpEmailLink(emailAddress = emailAddress)
            } else {
              AuthDestination.SignUpCode(field = SignUpCodeField.Email(emailAddress))
            }
          backStack.add(destination)
        } else {
          resetToRoot()
        }
      }
      PHONE_NUMBER -> {
        val phoneNumber = signUp.phoneNumber
        if (phoneNumber != null) {
          backStack.add(AuthDestination.SignUpCode(SignUpCodeField.Phone(phoneNumber)))
        } else {
          resetToRoot()
        }
      }
      else -> resetToRoot()
    }
  }

  private fun handleFieldCollection(signUp: SignUp) {
    val nextFieldToCollect = signUp.firstFieldToCollect
    if (nextFieldToCollect != null) {
      when (nextFieldToCollect) {
        PASSWORD -> backStack.add(AuthDestination.SignUpCollectField(CollectField.Password))
        EMAIL_ADDRESS -> backStack.add(AuthDestination.SignUpCollectField(CollectField.Email))
        PHONE_NUMBER -> backStack.add(AuthDestination.SignUpCollectField(CollectField.Phone))
        USERNAME -> backStack.add(AuthDestination.SignUpCollectField(CollectField.Username))
        else -> backStack.add(AuthDestination.SignUpCompleteProfile(signUp.missingFields.count()))
      }
    }
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
  val unsafeMetadata: kotlinx.serialization.json.JsonObject? = null,
)

internal fun authSharedPreferences(context: Context): SharedPreferences {
  return context.applicationContext.getSharedPreferences(
    "clerk_preferences",
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
  com.clerk.ui.core.preview.ClerkPreview {
    val backStack = rememberNavBackStack(AuthDestination.AuthStart)
    AuthStateProvider(backStack) { content() }
  }
}
