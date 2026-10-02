@file:Suppress("TooManyFunctions") // The routing table is kept in one file on purpose.

package com.clerk.ui.auth

import androidx.navigation3.runtime.NavKey
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.session.Session
import com.clerk.api.session.SessionTaskKey
import com.clerk.api.session.pendingTaskKey
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.startingFirstFactor
import com.clerk.api.signin.startingSecondFactor
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.emailVerificationStrategy
import com.clerk.api.signup.firstFieldToCollect
import com.clerk.api.signup.firstFieldToVerify
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.signup.code.SignUpCodeField
import com.clerk.ui.signup.collectfield.CollectField

/**
 * Prebuilt auth routing.
 *
 * Every decision about where the auth flow goes next lives here, as pure functions from auth state
 * to a [AuthNavigationCommand] or [SignInFactorScreen]. [AuthState] applies the command to the back
 * stack; screens only report the [SignIn], [SignUp], or [Session] they ended with.
 *
 * All strategy comparisons used for routing are kept in this file.
 */
internal sealed interface AuthRoutingInput {
  /** A sign-in attempt returned by an auth step. */
  data class SignInStep(val signIn: SignIn, val session: Session?) : AuthRoutingInput

  /** A sign-up attempt returned by an auth step. */
  data class SignUpStep(val signUp: SignUp, val session: Session?) : AuthRoutingInput

  /** The session after a session task (or the post-auth enrollment prompt) finished. */
  data class SessionStep(val session: Session?) : AuthRoutingInput

  /** The active session changed while [top] is shown; a new pending task must be surfaced. */
  data class PendingSessionTask(val session: Session?, val top: NavKey?) : AuthRoutingInput
}

/** Values from outside the attempt itself that routing depends on. */
internal data class AuthRoutingContext(
  val organizationSelectionIsForced: Boolean = false,
  /** Identifier typed on the start screen, used when the sign-in does not echo it back. */
  val lastSubmittedIdentifier: String? = null,
  val startingFirstFactor: (SignIn) -> Factor? = { it.startingFirstFactor },
)

internal sealed interface AuthNavigationCommand {
  /** Stay where we are. */
  data object None : AuthNavigationCommand

  /** Pop back to the start screen. */
  data object ResetToRoot : AuthNavigationCommand

  data class Push(val destination: NavKey) : AuthNavigationCommand

  /** Push a session task screen unless it is already on top. */
  data class ShowSessionTask(val destination: NavKey) : AuthNavigationCommand

  /** Swap the session task screen on top for the next one. */
  data class ReplaceSessionTask(val destination: NavKey) : AuthNavigationCommand

  /**
   * Authentication is done. When [offerBiometricEnrollment] is set the enrollment prompt may be
   * shown first.
   */
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

/** The screen for each session task. */
internal fun sessionTaskDestination(taskKey: SessionTaskKey): NavKey =
  when (taskKey) {
    SessionTaskKey.MFA_REQUIRED -> AuthDestination.SessionTaskMfa
    SessionTaskKey.RESET_PASSWORD -> AuthDestination.SessionTaskResetPassword
    SessionTaskKey.CHOOSE_ORGANIZATION -> AuthDestination.SessionTaskChooseOrganization
    SessionTaskKey.UNKNOWN -> AuthDestination.SignInGetHelp
  }

/** Whether [this] destination already handles the pending [taskKey]. */
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
        if (signUp.emailVerificationStrategy == StrategyKeys.EMAIL_LINK) {
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
  return when {
    taskKey != null -> AuthNavigationCommand.ShowSessionTask(sessionTaskDestination(taskKey))
    // The created session has not reached the client yet; it is gated on an MFA setup task.
    createdSessionId != null && session == null ->
      AuthNavigationCommand.ShowSessionTask(AuthDestination.SessionTaskMfa)
    createdSessionId != null && context.organizationSelectionIsForced ->
      AuthNavigationCommand.ShowSessionTask(AuthDestination.SessionTaskChooseOrganization)
    else -> AuthNavigationCommand.CompleteAuth(offerBiometricEnrollment = true, afterSignUp)
  }
}

private fun pushOrHelp(destination: NavKey?): AuthNavigationCommand =
  AuthNavigationCommand.Push(destination ?: AuthDestination.SignInGetHelp)

/** The screen a sign-in factor is verified with. */
internal enum class SignInFactorScreen {
  Passkey,
  Password,
  EmailLink,
  Code,
  BackupCode,
  GetHelp,
}

internal fun firstFactorScreen(strategy: String): SignInFactorScreen =
  when (strategy) {
    StrategyKeys.PASSKEY -> SignInFactorScreen.Passkey
    StrategyKeys.PASSWORD -> SignInFactorScreen.Password
    StrategyKeys.EMAIL_LINK -> SignInFactorScreen.EmailLink
    StrategyKeys.EMAIL_CODE,
    StrategyKeys.PHONE_CODE,
    StrategyKeys.RESET_PASSWORD_PHONE_CODE,
    StrategyKeys.RESET_PASSWORD_EMAIL_CODE -> SignInFactorScreen.Code
    else -> SignInFactorScreen.GetHelp
  }

internal fun secondFactorScreen(strategy: String): SignInFactorScreen =
  when (strategy) {
    StrategyKeys.TOTP,
    StrategyKeys.PHONE_CODE,
    StrategyKeys.EMAIL_CODE -> SignInFactorScreen.Code
    StrategyKeys.BACKUP_CODE -> SignInFactorScreen.BackupCode
    StrategyKeys.PASSKEY -> SignInFactorScreen.Passkey
    else -> SignInFactorScreen.GetHelp
  }

internal fun clientTrustScreen(strategy: String): SignInFactorScreen =
  when (strategy) {
    StrategyKeys.PHONE_CODE,
    StrategyKeys.EMAIL_CODE -> SignInFactorScreen.Code
    StrategyKeys.PASSKEY -> SignInFactorScreen.Passkey
    else -> SignInFactorScreen.GetHelp
  }

private const val EMAIL_ADDRESS = "email_address"
private const val PHONE_NUMBER = "phone_number"
private const val PASSWORD = "password"
private const val USERNAME = "username"
