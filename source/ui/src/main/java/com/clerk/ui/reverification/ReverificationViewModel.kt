package com.clerk.ui.reverification

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.auth.types.Strategy
import com.clerk.api.biometriccredential.BiometricCredentialAvailability
import com.clerk.api.credentials.resolvedCredentialFlowMessage
import com.clerk.api.credentials.shouldSuppressAutomaticCredentialFlowError
import com.clerk.api.credentials.shouldSuppressCredentialFlowError
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.session.GetTokenOptions
import com.clerk.api.session.Session
import com.clerk.api.session.SessionVerification
import com.clerk.api.session.fetchToken
import com.clerk.api.session.sendEmailCode
import com.clerk.api.session.sendMfaPhoneCode
import com.clerk.api.session.sendPhoneCode
import com.clerk.api.session.sendResetPasswordEmailCode
import com.clerk.api.session.sendResetPasswordPhoneCode
import com.clerk.api.session.startVerification
import com.clerk.api.session.verifyWithBackupCode
import com.clerk.api.session.verifyWithBiometrics
import com.clerk.api.session.verifyWithEmailCode
import com.clerk.api.session.verifyWithMfaPhoneCode
import com.clerk.api.session.verifyWithPasskey
import com.clerk.api.session.verifyWithPassword
import com.clerk.api.session.verifyWithPhoneCode
import com.clerk.api.session.verifyWithResetPasswordEmailCode
import com.clerk.api.session.verifyWithResetPasswordPhoneCode
import com.clerk.api.session.verifyWithTOTP
import com.clerk.ui.auth.VerificationUiState
import com.clerk.ui.auth.isBiometricCredentialCancellation
import com.clerk.ui.core.error.isIncorrectCode
import com.clerk.ui.core.log.ClerkLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive

internal sealed interface ReverificationPhase {
  data object Starting : ReverificationPhase

  data object Ready : ReverificationPhase

  data class Failed(val message: String) : ReverificationPhase

  data class Complete(val sessionId: String) : ReverificationPhase
}

internal data class ReverificationScreenState(
  val isLoading: Boolean = false,
  val fieldError: String? = null,
  val passwordResetReason: PasswordResetReason? = null,
  val codeState: VerificationUiState = VerificationUiState.Idle,
)

@Suppress("TooManyFunctions")
internal class ReverificationViewModel(
  private val level: SessionVerification.Level,
  private val sessionId: String?,
  private val currentSession: () -> Session? = { Clerk.session },
  private val prefersPassword: () -> Boolean = { Clerk.prefersPasswordSignIn },
  private val hasLocalBiometricCredential: () -> Boolean = {
    Clerk.biometricCredentials.currentUserLocalAvailability() ==
      BiometricCredentialAvailability.Available
  },
) : ViewModel() {
  var phase: ReverificationPhase by mutableStateOf(ReverificationPhase.Starting)
    private set

  var screen: ReverificationScreenState by mutableStateOf(ReverificationScreenState())
    private set

  val backStack = mutableStateListOf<ReverificationDestination>()

  var newPassword by mutableStateOf("")
  var confirmNewPassword by mutableStateOf("")
  var signOutOfOtherSessions by mutableStateOf(false)
  var newPasswordError: String? by mutableStateOf(null)
    private set

  private val errorMessages = Channel<String>(Channel.BUFFERED)
  val errors = errorMessages.receiveAsFlow()

  private var session: Session? = null
  private var verification: SessionVerification? = null
  private var biometricAvailable = false
  private val codesSent = mutableSetOf<String>()
  private var lastCodeSent: String? = null
  private val automaticAttempts = mutableSetOf<ReverificationDestination>()

  init {
    viewModelScope.launch { start() }
  }

  private suspend fun start() {
    val session = currentSession()?.takeIf { it.id == sessionId } ?: return
    this.session = session
    biometricAvailable = withContext(Dispatchers.IO) { hasLocalBiometricCredential() }
    when (val result = session.startVerification(level)) {
      is ClerkResult.Success -> handle(result.value)
      is ClerkResult.Failure -> {
        ClerkLog.e("Failed to start reverification: ${result.errorMessage}")
        phase = ReverificationPhase.Failed(result.errorMessage)
      }
    }
  }

  private suspend fun handle(verification: SessionVerification) {
    this.verification = verification
    when (verification.status) {
      SessionVerification.Status.NEEDS_FIRST_FACTOR -> {
        if (phase == ReverificationPhase.Ready) return
        showRoot(ReverificationStage.FIRST_FACTOR)
      }
      SessionVerification.Status.NEEDS_SECOND_FACTOR -> showRoot(ReverificationStage.SECOND_FACTOR)
      SessionVerification.Status.COMPLETE -> {
        val session = session ?: return
        session.fetchToken(GetTokenOptions(skipCache = true))
        phase = ReverificationPhase.Complete(session.id)
      }
      SessionVerification.Status.UNKNOWN ->
        phase = ReverificationPhase.Failed(UNKNOWN_STATUS_MESSAGE)
    }
  }

  private fun showRoot(stage: ReverificationStage) {
    val root =
      ReverificationFactors.startingFactor(availableFactors(stage), stage, prefersPassword())?.let {
        ReverificationDestination.FactorEntry(it, stage)
      } ?: ReverificationDestination.GetHelp
    backStack.clear()
    backStack.add(root)
    screen = ReverificationScreenState()
    phase = ReverificationPhase.Ready
  }

  fun availableFactors(stage: ReverificationStage): List<Factor> {
    val factors =
      when (stage) {
        ReverificationStage.FIRST_FACTOR -> verification?.supportedFirstFactors
        ReverificationStage.SECOND_FACTOR -> verification?.supportedSecondFactors
      }
    return ReverificationFactors.usable(factors.orEmpty(), stage, biometricAvailable)
  }

  fun alternativeFactors(currentFactor: Factor, stage: ReverificationStage): List<Factor> =
    availableFactors(stage).filter { it != currentFactor }

  val resetPasswordFactor: Factor?
    get() = ReverificationFactors.resetPasswordFactor(verification?.supportedFirstFactors.orEmpty())

  fun navigate(destination: ReverificationDestination) {
    backStack.add(destination)
    screen = ReverificationScreenState()
  }

  fun navigateBack(): Boolean {
    if (backStack.size <= 1) return false
    backStack.removeAt(backStack.lastIndex)
    screen = ReverificationScreenState()
    return true
  }

  fun clearFieldError() {
    if (screen.fieldError != null || screen.passwordResetReason != null) {
      screen = screen.copy(fieldError = null, passwordResetReason = null)
    }
  }

  fun resetCodeState() {
    screen = screen.copy(codeState = VerificationUiState.Idle)
  }

  fun updateNewPassword(value: String) {
    newPassword = value
    newPasswordError = null
  }

  fun verifyPassword(password: String) = runOnce { session ->
    when (val result = session.verifyWithPassword(password)) {
      is ClerkResult.Success -> {
        screen = ReverificationScreenState()
        handle(result.value)
      }
      is ClerkResult.Failure ->
        screen =
          ReverificationScreenState(
            fieldError = result.errorMessage,
            passwordResetReason =
              PasswordResetReason.of(result).takeIf { resetPasswordFactor != null },
          )
    }
  }

  fun verifyBackupCode(code: String) = runOnce { session ->
    when (val result = session.verifyWithBackupCode(code.trim())) {
      is ClerkResult.Success -> {
        screen = ReverificationScreenState()
        handle(result.value)
      }
      is ClerkResult.Failure -> screen = ReverificationScreenState(fieldError = result.errorMessage)
    }
  }

  fun sendCodeIfNeeded(factor: Factor, stage: ReverificationStage) {
    val key = codeKey(factor, stage)
    if (factor.strategyType == Strategy.Totp) return
    if (key in codesSent && lastCodeSent == key) return
    sendCode(factor, stage)
  }

  fun sendCode(factor: Factor, stage: ReverificationStage) {
    val session = session ?: return
    val key = codeKey(factor, stage)
    lastCodeSent = null
    screen = screen.copy(codeState = VerificationUiState.Idle)
    viewModelScope.launch {
      when (val result = prepare(session, factor, stage)) {
        null -> Unit
        is ClerkResult.Success -> {
          codesSent += key
          lastCodeSent = key
        }
        is ClerkResult.Failure -> {
          ClerkLog.e("Failed to send a reverification code: ${result.errorMessage}")
          errorMessages.send(result.errorMessage)
        }
      }
    }
  }

  fun verifyCode(factor: Factor, stage: ReverificationStage, code: String) {
    val session = session ?: return
    if (screen.codeState == VerificationUiState.Verifying) return
    screen = screen.copy(codeState = VerificationUiState.Verifying)
    viewModelScope.launch {
      when (val result = attemptCode(session, factor, stage, code)) {
        null -> screen = screen.copy(codeState = VerificationUiState.Idle)
        is ClerkResult.Success -> {
          screen = screen.copy(codeState = VerificationUiState.Verified)
          handle(result.value)
        }
        is ClerkResult.Failure -> onCodeFailure(factor, result)
      }
    }
  }

  private suspend fun onCodeFailure(
    factor: Factor,
    failure: ClerkResult.Failure<ClerkErrorResponse>,
  ) {
    when {
      factor.isResetPasswordFactor && failure.paramName == "password" -> {
        newPasswordError = failure.errorMessage
        val top = backStack.lastOrNull()
        if (top is ReverificationDestination.FactorEntry && top.factor.isResetPasswordFactor) {
          navigateBack()
        }
      }
      failure.isIncorrectCode -> screen = screen.copy(codeState = VerificationUiState.IncorrectCode)
      else -> {
        screen = screen.copy(codeState = VerificationUiState.Idle)
        errorMessages.send(failure.errorMessage)
      }
    }
  }

  fun verifyWithCredential(factor: Factor, stage: ReverificationStage) {
    verifyWithCredential(factor, stage, automatic = false)
  }

  fun attemptAutomatically(factor: Factor, stage: ReverificationStage) {
    if (!automaticAttempts.add(ReverificationDestination.FactorEntry(factor, stage))) return
    verifyWithCredential(factor, stage, automatic = true)
  }

  private fun verifyWithCredential(
    factor: Factor,
    stage: ReverificationStage,
    automatic: Boolean,
  ) = runOnce { session ->
    lastCodeSent = null
    val usesBiometrics = factor.strategyType == Strategy.TrustedDevice
    val result =
      if (usesBiometrics) {
        session.verifyWithBiometrics(level = stage.level)
      } else {
        session.verifyWithPasskey(level = stage.level)
      }
    when (result) {
      is ClerkResult.Success -> handle(result.value)
      is ClerkResult.Failure -> {
        val quiet =
          when {
            usesBiometrics -> result.isBiometricCredentialCancellation
            automatic -> result.shouldSuppressAutomaticCredentialFlowError
            else -> result.shouldSuppressCredentialFlowError
          }
        if (!quiet) {
          ClerkLog.e("Failed to reverify with ${factor.strategy}: ${result.errorMessage}")
          errorMessages.send(
            if (usesBiometrics) result.errorMessage else result.resolvedCredentialFlowMessage
          )
        }
      }
    }
  }

  private fun runOnce(action: suspend (Session) -> Unit) {
    val session = session ?: return
    if (screen.isLoading) return
    screen = screen.copy(isLoading = true)
    viewModelScope.launch {
      try {
        action(session)
      } finally {
        screen = screen.copy(isLoading = false)
      }
    }
  }

  private suspend fun prepare(
    session: Session,
    factor: Factor,
    stage: ReverificationStage,
  ): ClerkResult<SessionVerification, ClerkErrorResponse>? {
    val emailAddressId = factor.emailAddressId.orEmpty()
    val phoneNumberId = factor.phoneNumberId.orEmpty()
    return when (factor.strategyType) {
      Strategy.EmailCode -> session.sendEmailCode(emailAddressId)
      Strategy.PhoneCode ->
        if (stage == ReverificationStage.SECOND_FACTOR) {
          session.sendMfaPhoneCode(phoneNumberId)
        } else {
          session.sendPhoneCode(phoneNumberId)
        }
      Strategy.ResetPasswordEmailCode -> session.sendResetPasswordEmailCode(emailAddressId)
      Strategy.ResetPasswordPhoneCode -> session.sendResetPasswordPhoneCode(phoneNumberId)
      else -> null
    }
  }

  private suspend fun attemptCode(
    session: Session,
    factor: Factor,
    stage: ReverificationStage,
    code: String,
  ): ClerkResult<SessionVerification, ClerkErrorResponse>? =
    when (factor.strategyType) {
      Strategy.EmailCode -> session.verifyWithEmailCode(code)
      Strategy.PhoneCode ->
        if (stage == ReverificationStage.SECOND_FACTOR) {
          session.verifyWithMfaPhoneCode(code)
        } else {
          session.verifyWithPhoneCode(code)
        }
      Strategy.Totp -> session.verifyWithTOTP(code)
      Strategy.ResetPasswordEmailCode ->
        session.verifyWithResetPasswordEmailCode(code, newPassword, signOutOfOtherSessions)
      Strategy.ResetPasswordPhoneCode ->
        session.verifyWithResetPasswordPhoneCode(code, newPassword, signOutOfOtherSessions)
      else -> null
    }

  private fun codeKey(factor: Factor, stage: ReverificationStage): String =
    listOf(verification?.id.orEmpty(), stage.name, factor.strategy, factor.safeIdentifier.orEmpty())
      .joinToString(":")

  private companion object {
    const val UNKNOWN_STATUS_MESSAGE = "Clerk returned a reverification status it doesn't know."
  }
}

private val ClerkResult.Failure<ClerkErrorResponse>.paramName: String?
  get() =
    (error?.errors?.firstOrNull()?.meta?.get("param_name") as? JsonPrimitive)
      ?.takeIf { it.isString }
      ?.content
