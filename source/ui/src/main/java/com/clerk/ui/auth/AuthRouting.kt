@file:Suppress("TooManyFunctions")

package com.clerk.ui.auth

import androidx.navigation3.runtime.NavKey
import com.clerk.api.auth.types.Strategy
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.session.Session
import com.clerk.api.session.SessionTaskKey
import com.clerk.api.session.pendingTaskKey
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.startingFirstFactor
import com.clerk.api.signin.startingSecondFactor
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.firstFieldToCollect
import com.clerk.api.signup.firstFieldToVerify
import com.clerk.api.signup.isEmailLinkVerificationSupported
import com.clerk.ui.signup.code.SignUpCodeField
import com.clerk.ui.signup.collectfield.CollectField

internal sealed interface AuthRoutingInput {
  data class SignInStep(val signIn: SignIn, val session: Session?) : AuthRoutingInput

  data class SignUpStep(val signUp: SignUp, val session: Session?) : AuthRoutingInput

  data class SessionStep(val session: Session?) : AuthRoutingInput

  data class PendingSessionTask(val session: Session?, val top: NavKey?) : AuthRoutingInput
}

internal data class AuthRoutingContext(
  val organizationSelectionIsForced: Boolean = false,
  val lastSubmittedIdentifier: String? = null,
  val startingFirstFactor: (SignIn) -> Factor? = { it.startingFirstFactor },
)

internal sealed interface AuthNavigationCommand {
  data object None : AuthNavigationCommand

  data object ResetToRoot : AuthNavigationCommand

  data class Push(val destination: NavKey) : AuthNavigationCommand

  data class ShowSessionTask(val destination: NavKey) : AuthNavigationCommand

  data class ReplaceSessionTask(val destination: NavKey) : AuthNavigationCommand

  data class CompleteAuth(val offerBiometricEnrollment: Boolean, val afterSignUp: Boolean = false) :
    AuthNavigationCommand
}

internal fun authNavigationCommand(
  input: AuthRoutingInput,
  context: AuthRoutingContext = AuthRoutingContext(),
): AuthNavigationCommand =
  when (input) {
    is AuthRoutingInput.SignInStep -> signInCommand(input.signIn, input.session, context)
    is AuthRoutingInput.SignUpStep -> signUpCommand(input.signUp, input.session, context)
    is AuthRoutingInput.SessionStep ->
      input.session?.pendingTaskKey?.let {
        AuthNavigationCommand.ReplaceSessionTask(sessionTaskDestination(it))
      } ?: AuthNavigationCommand.CompleteAuth(offerBiometricEnrollment = false)
    is AuthRoutingInput.PendingSessionTask -> {
      val taskKey = input.session?.pendingTaskKey
      if (taskKey == null || input.top.satisfiesSessionTask(taskKey)) {
        AuthNavigationCommand.None
      } else {
        AuthNavigationCommand.Push(sessionTaskDestination(taskKey))
      }
    }
  }

internal fun sessionTaskDestination(taskKey: SessionTaskKey): NavKey =
  when (taskKey) {
    SessionTaskKey.MFA_REQUIRED -> AuthDestination.SessionTaskMfa
    SessionTaskKey.RESET_PASSWORD -> AuthDestination.SessionTaskResetPassword
    SessionTaskKey.CHOOSE_ORGANIZATION -> AuthDestination.SessionTaskChooseOrganization
    SessionTaskKey.UNKNOWN -> AuthDestination.SignInGetHelp
  }

internal fun NavKey?.satisfiesSessionTask(taskKey: SessionTaskKey): Boolean =
  when (taskKey) {
    SessionTaskKey.CHOOSE_ORGANIZATION ->
      this == AuthDestination.SessionTaskChooseOrganization ||
        this is AuthDestination.SessionTaskCreateOrganization
    else -> this == sessionTaskDestination(taskKey)
  }

internal fun NavKey?.isSessionTaskDestination(): Boolean =
  this == AuthDestination.SessionTaskMfa ||
    this == AuthDestination.SessionTaskResetPassword ||
    this == AuthDestination.SessionTaskChooseOrganization ||
    this is AuthDestination.SessionTaskCreateOrganization

private fun signInCommand(
  signIn: SignIn,
  session: Session?,
  context: AuthRoutingContext,
): AuthNavigationCommand =
  when (signIn.status) {
    SignIn.Status.COMPLETE ->
      postAuthCommand(
        session = session,
        createdSessionId = signIn.createdSessionId,
        context = context,
        afterSignUp = false,
      )
    SignIn.Status.NEEDS_IDENTIFIER -> AuthNavigationCommand.ResetToRoot
    SignIn.Status.NEEDS_FIRST_FACTOR -> {
      val resolved =
        if (signIn.identifier.isNullOrBlank() && !context.lastSubmittedIdentifier.isNullOrBlank()) {
          signIn.copy(identifier = context.lastSubmittedIdentifier)
        } else {
          signIn
        }
      pushOrHelp(context.startingFirstFactor(resolved)?.let { AuthDestination.SignInFactorOne(it) })
    }
    SignIn.Status.NEEDS_SECOND_FACTOR ->
      pushOrHelp(signIn.startingSecondFactor?.let { AuthDestination.SignInFactorTwo(it) })
    SignIn.Status.NEEDS_NEW_PASSWORD ->
      AuthNavigationCommand.Push(AuthDestination.SignInSetNewPassword)
    SignIn.Status.NEEDS_CLIENT_TRUST ->
      pushOrHelp(signIn.startingSecondFactor?.let { AuthDestination.SignInClientTrust(it) })
    SignIn.Status.UNKNOWN -> AuthNavigationCommand.None
  }

private fun signUpCommand(
  signUp: SignUp,
  session: Session?,
  context: AuthRoutingContext,
): AuthNavigationCommand =
  when (signUp.status) {
    SignUp.Status.ABANDONED -> AuthNavigationCommand.ResetToRoot
    SignUp.Status.MISSING_REQUIREMENTS -> missingRequirementsCommand(signUp)
    SignUp.Status.COMPLETE ->
      postAuthCommand(
        session = session,
        createdSessionId = signUp.createdSessionId,
        context = context,
        afterSignUp = true,
      )
    SignUp.Status.UNKNOWN -> AuthNavigationCommand.None
  }

private fun missingRequirementsCommand(signUp: SignUp): AuthNavigationCommand {
  val fieldToCollect = signUp.firstFieldToCollect
  val fieldToVerify = signUp.firstFieldToVerify
  return when {
    fieldToCollect != null ->
      AuthNavigationCommand.Push(
        collectField(fieldToCollect)?.let { AuthDestination.SignUpCollectField(it) }
          ?: AuthDestination.SignUpCompleteProfile(signUp.missingFields.count())
      )
    fieldToVerify != null ->
      verifyFieldDestination(signUp, fieldToVerify)?.let { AuthNavigationCommand.Push(it) }
        ?: AuthNavigationCommand.ResetToRoot
    else -> AuthNavigationCommand.None
  }
}

private fun verifyFieldDestination(signUp: SignUp, field: String): NavKey? =
  when (field) {
    EMAIL_ADDRESS ->
      signUp.emailAddress?.let { email ->
        if (signUp.isEmailLinkVerificationSupported) {
          AuthDestination.SignUpEmailLink(emailAddress = email)
        } else {
          AuthDestination.SignUpCode(SignUpCodeField.Email(email))
        }
      }
    PHONE_NUMBER ->
      signUp.phoneNumber?.let { AuthDestination.SignUpCode(SignUpCodeField.Phone(it)) }
    else -> null
  }

private fun collectField(field: String): CollectField? =
  when (field) {
    PASSWORD -> CollectField.Password
    EMAIL_ADDRESS -> CollectField.Email
    PHONE_NUMBER -> CollectField.Phone
    USERNAME -> CollectField.Username
    else -> null
  }

private fun postAuthCommand(
  session: Session?,
  createdSessionId: String?,
  context: AuthRoutingContext,
  afterSignUp: Boolean,
): AuthNavigationCommand {
  val taskKey = session?.pendingTaskKey
  val createdSessionAwaitsMfaSetup = createdSessionId != null && session == null
  return when {
    taskKey == SessionTaskKey.UNKNOWN -> AuthNavigationCommand.Push(AuthDestination.SignInGetHelp)
    taskKey != null -> AuthNavigationCommand.ShowSessionTask(sessionTaskDestination(taskKey))
    createdSessionAwaitsMfaSetup ->
      AuthNavigationCommand.ShowSessionTask(AuthDestination.SessionTaskMfa)
    createdSessionId != null && context.organizationSelectionIsForced ->
      AuthNavigationCommand.ShowSessionTask(AuthDestination.SessionTaskChooseOrganization)
    else -> AuthNavigationCommand.CompleteAuth(offerBiometricEnrollment = true, afterSignUp)
  }
}

private fun pushOrHelp(destination: NavKey?): AuthNavigationCommand =
  AuthNavigationCommand.Push(destination ?: AuthDestination.SignInGetHelp)

internal enum class SignInFactorScreen {
  Passkey,
  Password,
  EmailLink,
  Code,
  BackupCode,
  GetHelp,
}

internal fun firstFactorScreen(strategy: Strategy): SignInFactorScreen =
  when (strategy) {
    Strategy.Passkey -> SignInFactorScreen.Passkey
    Strategy.Password -> SignInFactorScreen.Password
    Strategy.EmailLink -> SignInFactorScreen.EmailLink
    Strategy.EmailCode,
    Strategy.PhoneCode,
    Strategy.ResetPasswordPhoneCode,
    Strategy.ResetPasswordEmailCode -> SignInFactorScreen.Code
    else -> SignInFactorScreen.GetHelp
  }

internal fun secondFactorScreen(strategy: Strategy): SignInFactorScreen =
  when (strategy) {
    Strategy.Totp,
    Strategy.PhoneCode,
    Strategy.EmailCode -> SignInFactorScreen.Code
    Strategy.BackupCode -> SignInFactorScreen.BackupCode
    Strategy.Passkey -> SignInFactorScreen.Passkey
    else -> SignInFactorScreen.GetHelp
  }

internal fun clientTrustScreen(strategy: Strategy): SignInFactorScreen =
  when (strategy) {
    Strategy.PhoneCode,
    Strategy.EmailCode -> SignInFactorScreen.Code
    Strategy.Passkey -> SignInFactorScreen.Passkey
    else -> SignInFactorScreen.GetHelp
  }

private const val EMAIL_ADDRESS = "email_address"
private const val PHONE_NUMBER = "phone_number"
private const val PASSWORD = "password"
private const val USERNAME = "username"
