package com.clerk.ui.auth

import com.clerk.api.auth.types.Strategy
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.session.Session
import com.clerk.api.session.SessionTask
import com.clerk.api.session.SessionTaskKey
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.clerk.ui.auth.AuthNavigationCommand.CompleteAuth
import com.clerk.ui.auth.AuthNavigationCommand.None
import com.clerk.ui.auth.AuthNavigationCommand.Push
import com.clerk.ui.auth.AuthNavigationCommand.ReplaceSessionTask
import com.clerk.ui.auth.AuthNavigationCommand.ResetToRoot
import com.clerk.ui.auth.AuthNavigationCommand.ShowSessionTask
import com.clerk.ui.auth.AuthRoutingInput.SessionStep
import com.clerk.ui.auth.AuthRoutingInput.SignInStep
import com.clerk.ui.auth.AuthRoutingInput.SignUpStep
import com.clerk.ui.signup.code.SignUpCodeField
import com.clerk.ui.signup.collectfield.CollectField
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthRoutingTest {

  private val emailCode = Factor(strategy = Strategy.EmailCode.value, safeIdentifier = "s@c.dev")
  private val totp = Factor(strategy = Strategy.Totp.value)
  private val context = AuthRoutingContext(startingFirstFactor = { emailCode })
  private val noFirstFactor = AuthRoutingContext(startingFirstFactor = { null })

  private data class Case(
    val name: String,
    val input: AuthRoutingInput,
    val expected: AuthNavigationCommand,
    val context: AuthRoutingContext = AuthRoutingContext(startingFirstFactor = { null }),
  )

  @Test
  fun signInRouting() {
    val cases =
      listOf(
        Case(
          "needs identifier",
          SignInStep(signIn(SignIn.Status.NEEDS_IDENTIFIER), null),
          ResetToRoot,
        ),
        Case(
          "needs first factor",
          SignInStep(signIn(SignIn.Status.NEEDS_FIRST_FACTOR), null),
          Push(AuthDestination.SignInFactorOne(emailCode)),
          context,
        ),
        Case(
          "needs first factor without a usable factor",
          SignInStep(signIn(SignIn.Status.NEEDS_FIRST_FACTOR), null),
          Push(AuthDestination.SignInGetHelp),
          noFirstFactor,
        ),
        Case(
          "needs second factor",
          SignInStep(signIn(SignIn.Status.NEEDS_SECOND_FACTOR, secondFactors = listOf(totp)), null),
          Push(AuthDestination.SignInFactorTwo(totp)),
        ),
        Case(
          "needs second factor without a usable factor",
          SignInStep(signIn(SignIn.Status.NEEDS_SECOND_FACTOR), null),
          Push(AuthDestination.SignInGetHelp),
        ),
        Case(
          "needs client trust",
          SignInStep(
            signIn(SignIn.Status.NEEDS_CLIENT_TRUST, secondFactors = listOf(emailCode)),
            null,
          ),
          Push(AuthDestination.SignInClientTrust(emailCode)),
        ),
        Case(
          "needs client trust without a usable factor",
          SignInStep(signIn(SignIn.Status.NEEDS_CLIENT_TRUST), null),
          Push(AuthDestination.SignInGetHelp),
        ),
        Case(
          "needs new password",
          SignInStep(signIn(SignIn.Status.NEEDS_NEW_PASSWORD), null),
          Push(AuthDestination.SignInSetNewPassword),
        ),
        Case("unknown status", SignInStep(signIn(SignIn.Status.UNKNOWN), null), None),
      ) +
        postAuthCases { createdSessionId, session ->
          SignInStep(signIn(SignIn.Status.COMPLETE, createdSessionId = createdSessionId), session)
        }

    assertCases(cases, afterSignUp = false)
  }

  @Test
  fun signInFirstFactorUsesLastSubmittedIdentifierWhenSignInHasNone() {
    var routedIdentifier: String? = null
    val context =
      AuthRoutingContext(
        lastSubmittedIdentifier = "typed@clerk.dev",
        startingFirstFactor = {
          routedIdentifier = it.identifier
          emailCode
        },
      )

    authNavigationCommand(SignInStep(signIn(SignIn.Status.NEEDS_FIRST_FACTOR), null), context)

    assertEquals("typed@clerk.dev", routedIdentifier)
  }

  @Test
  fun signUpCollectionRouting() {
    val cases =
      listOf(
        Case("abandoned", SignUpStep(signUp(SignUp.Status.ABANDONED), null), ResetToRoot),
        Case("unknown status", SignUpStep(signUp(SignUp.Status.UNKNOWN), null), None),
        Case(
          "collect password",
          SignUpStep(missing(missingFields = listOf("password")), null),
          Push(AuthDestination.SignUpCollectField(CollectField.Password)),
        ),
        Case(
          "collect email",
          SignUpStep(missing(missingFields = listOf("email_address")), null),
          Push(AuthDestination.SignUpCollectField(CollectField.Email)),
        ),
        Case(
          "collect phone",
          SignUpStep(missing(missingFields = listOf("phone_number")), null),
          Push(AuthDestination.SignUpCollectField(CollectField.Phone)),
        ),
        Case(
          "collect username",
          SignUpStep(missing(missingFields = listOf("username")), null),
          Push(AuthDestination.SignUpCollectField(CollectField.Username)),
        ),
        Case(
          "collect other required fields on the complete profile screen",
          SignUpStep(missing(missingFields = listOf("first_name", "last_name")), null),
          Push(AuthDestination.SignUpCompleteProfile(progress = 2)),
        ),
        Case(
          "collect before verifying",
          SignUpStep(
            missing(missingFields = listOf("password"), unverifiedFields = listOf("email_address")),
            null,
          ),
          Push(AuthDestination.SignUpCollectField(CollectField.Password)),
        ),
      )

    assertCases(cases, afterSignUp = true)
  }

  @Test
  fun signUpVerificationAndCompletionRouting() {
    val cases =
      listOf(
        Case(
          "verify email with a code",
          SignUpStep(missing(unverifiedFields = listOf("email_address")), null),
          Push(AuthDestination.SignUpCode(SignUpCodeField.Email(EMAIL))),
        ),
        Case(
          "verify email with a link",
          SignUpStep(
            missing(
              unverifiedFields = listOf("email_address"),
              emailStrategy = Strategy.EmailLink,
            ),
            null,
          ),
          Push(AuthDestination.SignUpEmailLink(EMAIL)),
        ),
        Case(
          "verify email without an address",
          SignUpStep(
            missing(unverifiedFields = listOf("email_address"), emailAddress = null),
            null,
          ),
          ResetToRoot,
        ),
        Case(
          "verify phone",
          SignUpStep(missing(unverifiedFields = listOf("phone_number")), null),
          Push(AuthDestination.SignUpCode(SignUpCodeField.Phone(PHONE))),
        ),
        Case(
          "verify phone without a number",
          SignUpStep(missing(unverifiedFields = listOf("phone_number"), phoneNumber = null), null),
          ResetToRoot,
        ),
        Case(
          "verify an unsupported field",
          SignUpStep(missing(unverifiedFields = listOf("web3_wallet")), null),
          ResetToRoot,
        ),
        Case("nothing left to do", SignUpStep(missing(), null), None),
      ) +
        postAuthCases { createdSessionId, session ->
          SignUpStep(signUp(SignUp.Status.COMPLETE, createdSessionId = createdSessionId), session)
        }

    assertCases(cases, afterSignUp = true)
  }

  @Test
  fun sessionTaskCompletionRouting() {
    val cases =
      listOf(
        Case("no pending task", SessionStep(null), CompleteAuth(offerBiometricEnrollment = false)),
        Case(
          "active session",
          SessionStep(session(Session.SessionStatus.ACTIVE)),
          CompleteAuth(offerBiometricEnrollment = false),
        ),
      ) +
        taskCases.map { (task, _, destination) ->
          Case(
            "next task $task",
            SessionStep(pendingSession(task)),
            ReplaceSessionTask(destination),
          )
        }

    assertCases(cases, afterSignUp = false)
  }

  @Test
  fun sessionTaskDestinationCoversEveryTask() {
    val expected =
      mapOf(
        SessionTaskKey.MFA_REQUIRED to AuthDestination.SessionTaskMfa,
        SessionTaskKey.RESET_PASSWORD to AuthDestination.SessionTaskResetPassword,
        SessionTaskKey.CHOOSE_ORGANIZATION to AuthDestination.SessionTaskChooseOrganization,
        SessionTaskKey.UNKNOWN to AuthDestination.SignInGetHelp,
      )

    assertEquals(expected, SessionTaskKey.entries.associateWith { sessionTaskDestination(it) })
  }

  @Test
  fun factorScreens() {
    val first =
      mapOf(
        Strategy.Passkey to SignInFactorScreen.Passkey,
        Strategy.Password to SignInFactorScreen.Password,
        Strategy.EmailLink to SignInFactorScreen.EmailLink,
        Strategy.EmailCode to SignInFactorScreen.Code,
        Strategy.PhoneCode to SignInFactorScreen.Code,
        Strategy.ResetPasswordEmailCode to SignInFactorScreen.Code,
        Strategy.ResetPasswordPhoneCode to SignInFactorScreen.Code,
        Strategy.Totp to SignInFactorScreen.GetHelp,
        Strategy.BackupCode to SignInFactorScreen.GetHelp,
        Strategy.from("oauth_google") to SignInFactorScreen.GetHelp,
        Strategy.from("some_future_strategy") to SignInFactorScreen.GetHelp,
      )
    val second =
      mapOf(
        Strategy.Totp to SignInFactorScreen.Code,
        Strategy.PhoneCode to SignInFactorScreen.Code,
        Strategy.EmailCode to SignInFactorScreen.Code,
        Strategy.BackupCode to SignInFactorScreen.BackupCode,
        Strategy.Passkey to SignInFactorScreen.Passkey,
        Strategy.Password to SignInFactorScreen.GetHelp,
        Strategy.EmailLink to SignInFactorScreen.GetHelp,
      )
    val clientTrust =
      mapOf(
        Strategy.PhoneCode to SignInFactorScreen.Code,
        Strategy.EmailCode to SignInFactorScreen.Code,
        Strategy.Passkey to SignInFactorScreen.Passkey,
        Strategy.Totp to SignInFactorScreen.GetHelp,
        Strategy.BackupCode to SignInFactorScreen.GetHelp,
      )

    assertEquals(first, first.keys.associateWith { firstFactorScreen(it) })
    assertEquals(second, second.keys.associateWith { secondFactorScreen(it) })
    assertEquals(clientTrust, clientTrust.keys.associateWith { clientTrustScreen(it) })
  }

  @Test
  fun pendingTaskSatisfaction() {
    taskCases.forEach { (_, key, destination) ->
      assertEquals(true, destination.satisfiesSessionTask(key))
      assertEquals(false, AuthDestination.AuthStart.satisfiesSessionTask(key))
    }
    assertEquals(
      true,
      AuthDestination.SessionTaskCreateOrganization()
        .satisfiesSessionTask(SessionTaskKey.CHOOSE_ORGANIZATION),
    )
  }

  /** Completion cases shared by sign-in and sign-up. */
  private fun postAuthCases(
    input: (createdSessionId: String?, session: Session?) -> AuthRoutingInput
  ) =
    taskCases.map { (task, key, destination) ->
      Case(
        "complete with pending $task",
        input("sess_1", pendingSession(task)),
        if (key == SessionTaskKey.UNKNOWN) Push(destination) else ShowSessionTask(destination),
      )
    } +
      listOf(
        Case(
          "complete before the created session reaches the client",
          input("sess_1", null),
          ShowSessionTask(AuthDestination.SessionTaskMfa),
        ),
        Case(
          "complete with forced organization selection",
          input("sess_1", session(Session.SessionStatus.ACTIVE)),
          ShowSessionTask(AuthDestination.SessionTaskChooseOrganization),
          AuthRoutingContext(organizationSelectionIsForced = true, startingFirstFactor = { null }),
        ),
        Case(
          "pending task wins over forced organization selection",
          input("sess_1", pendingSession("reset-password")),
          ShowSessionTask(AuthDestination.SessionTaskResetPassword),
          AuthRoutingContext(organizationSelectionIsForced = true, startingFirstFactor = { null }),
        ),
        Case(
          "complete",
          input("sess_1", session(Session.SessionStatus.ACTIVE)),
          CompleteAuth(offerBiometricEnrollment = true),
        ),
        Case(
          "complete without a created session",
          input(null, session(Session.SessionStatus.ACTIVE)),
          CompleteAuth(offerBiometricEnrollment = true),
          AuthRoutingContext(organizationSelectionIsForced = true, startingFirstFactor = { null }),
        ),
      )

  private fun assertCases(cases: List<Case>, afterSignUp: Boolean) {
    cases.forEach { case ->
      val expected =
        (case.expected as? CompleteAuth)
          ?.takeIf { it.offerBiometricEnrollment }
          ?.copy(afterSignUp = afterSignUp) ?: case.expected
      assertEquals(case.name, expected, authNavigationCommand(case.input, case.context))
    }
  }

  private val taskCases =
    listOf(
      Triple("setup-mfa", SessionTaskKey.MFA_REQUIRED, AuthDestination.SessionTaskMfa),
      Triple(
        "reset-password",
        SessionTaskKey.RESET_PASSWORD,
        AuthDestination.SessionTaskResetPassword,
      ),
      Triple(
        "choose-organization",
        SessionTaskKey.CHOOSE_ORGANIZATION,
        AuthDestination.SessionTaskChooseOrganization,
      ),
      Triple("some-future-task", SessionTaskKey.UNKNOWN, AuthDestination.SignInGetHelp),
    )

  private fun signIn(
    status: SignIn.Status,
    createdSessionId: String? = null,
    secondFactors: List<Factor>? = null,
  ) =
    SignIn(
      id = "sign_in_1",
      status = status,
      createdSessionId = createdSessionId,
      supportedSecondFactors = secondFactors,
    )

  private fun signUp(status: SignUp.Status, createdSessionId: String? = null) =
    missing(status = status, createdSessionId = createdSessionId)

  private fun missing(
    status: SignUp.Status = SignUp.Status.MISSING_REQUIREMENTS,
    missingFields: List<String> = emptyList(),
    unverifiedFields: List<String> = emptyList(),
    emailStrategy: Strategy = Strategy.EmailCode,
    emailAddress: String? = EMAIL,
    phoneNumber: String? = PHONE,
    createdSessionId: String? = null,
  ) =
    SignUp(
      id = "sign_up_1",
      status = status,
      requiredFields = missingFields + unverifiedFields,
      optionalFields = emptyList(),
      missingFields = missingFields,
      unverifiedFields = unverifiedFields,
      verifications =
        mapOf(
          "email_address" to
            Verification(status = Verification.Status.UNVERIFIED, strategy = emailStrategy.value)
        ),
      emailAddress = emailAddress,
      phoneNumber = phoneNumber,
      passwordEnabled = false,
      createdSessionId = createdSessionId,
    )

  private fun session(status: Session.SessionStatus, currentTask: SessionTask? = null) =
    Session(
      id = "sess_1",
      status = status,
      expireAt = 0L,
      lastActiveAt = 0L,
      createdAt = 0L,
      updatedAt = 0L,
      currentTask = currentTask,
    )

  private fun pendingSession(task: String) =
    session(Session.SessionStatus.PENDING, currentTask = SessionTask(task))

  private companion object {
    const val EMAIL = "sam@clerk.dev"
    const val PHONE = "+15555550100"
  }
}
