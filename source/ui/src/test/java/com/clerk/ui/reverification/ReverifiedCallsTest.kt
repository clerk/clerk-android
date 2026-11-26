package com.clerk.ui.reverification

import com.clerk.api.Clerk
import com.clerk.api.biometriccredential.BiometricCredential
import com.clerk.api.biometriccredential.BiometricCredentialAvailability
import com.clerk.api.biometriccredential.BiometricCredentials
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.emailaddress.delete
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.api.externalaccount.delete
import com.clerk.api.network.model.backupcodes.BackupCodeResource
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.delete
import com.clerk.api.passkeys.Passkey
import com.clerk.api.passkeys.delete
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.api.phonenumber.delete
import com.clerk.api.phonenumber.makeDefaultSecondFactor
import com.clerk.api.phonenumber.setReservedForSecondFactor
import com.clerk.api.session.Session
import com.clerk.api.session.SessionTask
import com.clerk.api.session.SessionVerification
import com.clerk.api.session.revoke
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.user.User
import com.clerk.api.user.activeSessions
import com.clerk.api.user.createBackupCodes
import com.clerk.api.user.createEmailAddress
import com.clerk.api.user.createExternalAccount
import com.clerk.api.user.createPasskey
import com.clerk.api.user.createPhoneNumber
import com.clerk.api.user.createTotp
import com.clerk.api.user.delete
import com.clerk.api.user.disableTotp
import com.clerk.api.user.get
import com.clerk.api.user.update
import com.clerk.api.user.updatePassword
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.auth.biometriccredential.BiometricCredentialEnrollmentViewModel
import com.clerk.ui.organizationprofile.actions.OrganizationProfileActionConfirmationViewModel
import com.clerk.ui.organizationprofile.actions.OrganizationProfileConfirmationAction
import com.clerk.ui.signin.password.reset.ResetPasswordViewModel
import com.clerk.ui.userprofile.MainDispatcherRule
import com.clerk.ui.userprofile.account.UserProfileAccountViewModel
import com.clerk.ui.userprofile.connectedaccount.AddConnectedAccountViewModel
import com.clerk.ui.userprofile.email.AddEmailViewModel
import com.clerk.ui.userprofile.email.EmailViewModel
import com.clerk.ui.userprofile.mfa.MfaAddSmsViewModel
import com.clerk.ui.userprofile.phone.UserProfileAddPhoneViewModel
import com.clerk.ui.userprofile.security.UserProfileSecurityViewModel
import com.clerk.ui.userprofile.security.biometriccredential.UserProfileBiometricCredentialViewModel
import com.clerk.ui.userprofile.security.mfa.UserProfileMfaViewModel
import com.clerk.ui.userprofile.security.passkey.UserProfilePasskeyViewModel
import com.clerk.ui.userprofile.security.password.UserProfileChangePasswordViewModel
import com.clerk.ui.userprofile.totp.UserProfileMfaTotpViewModel
import com.clerk.ui.userprofile.update.UpdateProfileViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private enum class Observed {
  SUCCEEDED,
  UNCHANGED,
  ERROR,
}

private class GatedCall(val name: String, val run: GatedCallFixture.() -> () -> Observed) {
  override fun toString() = name
}

private val reverificationRequired: ClerkResult.Failure<ClerkErrorResponse> =
  ClerkResult.apiFailure(
    ClerkErrorResponse(
      errors =
        listOf(
          Error(
            code = "session_reverification_required",
            longMessage = "You need to provide additional verification to perform this operation",
          )
        )
    )
  )

@OptIn(ExperimentalCoroutinesApi::class)
private class GatedCallFixture(val dispatcher: TestDispatcher) {
  var requests = 0
  val user = mockk<User>(relaxed = true)
  val session = mockk<Session>(relaxed = true)
  val phoneNumber = mockk<PhoneNumber>(relaxed = true)
  val emailAddress = mockk<EmailAddress>(relaxed = true)

  fun <T : Any> gated(value: T): ClerkResult<T, ClerkErrorResponse> =
    if (requests++ == 0) reverificationRequired else ClerkResult.success(value)
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class ReverifiedCallsTest(private val call: Any) {
  private val dispatcher = UnconfinedTestDispatcher()
  @get:Rule val dispatcherRule = MainDispatcherRule(dispatcher)

  private val fixture = GatedCallFixture(dispatcher)
  private var outcome = ReverificationOutcome.VERIFIED
  private val presented = mutableListOf<SessionVerification.Level>()

  @Before
  fun setUp() {
    stubGatedCallDependencies(fixture)
    mockkObject(Reverifier)
    every { Reverifier.shared } returns
      Reverifier(
        currentIdentity = { SignedInIdentity(clientId = "client_1", sessionId = "sess_1") },
        present = { level, _ ->
          presented += level
          outcome
        },
      )
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun verifyingRetriesTheCallOnce() =
    runTest(dispatcher) {
      val observe = (call as GatedCall).run(fixture)

      awaitUntil { observe() == Observed.SUCCEEDED }
      assertEquals(2, fixture.requests)
      assertEquals(listOf(SessionVerification.Level.SECOND_FACTOR), presented)
    }

  @Test
  fun closingTheScreenSendsOneRequestAndShowsNoError() =
    runTest(dispatcher) {
      outcome = ReverificationOutcome.CANCELLED

      val observe = (call as GatedCall).run(fixture)

      awaitUntil { presented.size == 1 && observe() == Observed.UNCHANGED }
      assertEquals(Observed.UNCHANGED, observe())
      assertEquals(1, fixture.requests)
      assertEquals(1, presented.size)
    }

  companion object {
    @JvmStatic @Parameterized.Parameters(name = "{0}") fun calls(): List<Any> = gatedCalls
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun stubGatedCallDependencies(fixture: GatedCallFixture) {
  mockkObject(Clerk)
  every { Clerk.user } returns fixture.user
  every { Clerk.session } returns null
  every { Clerk.isEmailImmutable } returns false
  mockkStatic("com.clerk.api.user.UserKt")
  mockkStatic("com.clerk.api.emailaddress.EmailAddressKt")
  mockkStatic("com.clerk.api.phonenumber.PhoneNumberKt")
  mockkStatic("com.clerk.api.session.SessionKt")
  mockkStatic("com.clerk.api.passkeys.PasskeyKt")
  mockkStatic("com.clerk.api.externalaccount.ExternalAccountKt")
  mockkStatic("com.clerk.api.organizations.OrganizationKt")
}

private fun awaitUntil(condition: () -> Boolean) {
  val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
  while (!condition()) {
    check(System.nanoTime() < deadline) { "Timed out waiting for the call to finish" }
    Thread.sleep(5)
  }
}

private fun <S> stateOf(
  current: () -> S,
  succeeded: (S) -> Boolean,
  unchanged: (S) -> Boolean,
): () -> Observed = {
  val state = current()
  when {
    succeeded(state) -> Observed.SUCCEEDED
    unchanged(state) -> Observed.UNCHANGED
    else -> Observed.ERROR
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
private val gatedCalls: List<GatedCall> =
  listOf(
    GatedCall("delete the account") {
      coEvery { user.delete() } coAnswers { gated(mockk(relaxed = true)) }
      mockkObject(BiometricCredentials)
      every { BiometricCredentials.forgetLocalCredentialsAfterAccountDeletion(any()) } returns 0
      val viewModel = UserProfileAccountViewModel(workDispatcher = dispatcher)
      viewModel.deleteAccount()
      stateOf(
        { viewModel.deleteAccountStateFlow.value },
        { it == UserProfileAccountViewModel.DeleteAccountState.Success },
        { it == UserProfileAccountViewModel.DeleteAccountState.Idle },
      )
    },
    GatedCall("set the primary email address") {
      coEvery { user.update(any()) } coAnswers { gated(user) }
      val viewModel = EmailViewModel()
      viewModel.setAsPrimary(emailAddress)
      stateOf(
        { viewModel.state.value },
        { it == EmailViewModel.State.SetAsPrimary.Success },
        { it == EmailViewModel.State.Idle },
      )
    },
    GatedCall("remove an email address") {
      coEvery { emailAddress.delete() } coAnswers { gated(mockk(relaxed = true)) }
      val viewModel = EmailViewModel()
      viewModel.remove(emailAddress)
      stateOf(
        { viewModel.state.value },
        { it == EmailViewModel.State.Remove.Success },
        { it == EmailViewModel.State.Idle },
      )
    },
    GatedCall("add an email address") {
      coEvery { user.createEmailAddress(any()) } coAnswers { gated(emailAddress) }
      val viewModel = AddEmailViewModel()
      viewModel.addEmail("new+clerk_test@example.com")
      stateOf(
        { viewModel.state.value },
        { it is AddEmailViewModel.State.Success },
        { it == AddEmailViewModel.State.Idle },
      )
    },
    GatedCall("add a phone number") {
      coEvery { user.createPhoneNumber(any()) } coAnswers { gated(phoneNumber) }
      val viewModel = UserProfileAddPhoneViewModel()
      viewModel.addPhoneNumber("+15555550100")
      stateOf(
        { viewModel.state.value },
        { it is UserProfileAddPhoneViewModel.State.Success },
        { it == UserProfileAddPhoneViewModel.State.Idle },
      )
    },
    GatedCall("set the primary phone number") {
      coEvery { user.update(any()) } coAnswers { gated(user) }
      val viewModel = UserProfileAddPhoneViewModel()
      viewModel.setAsPrimary(phoneNumber)
      stateOf(
        { viewModel.state.value },
        { it == UserProfileAddPhoneViewModel.State.SetPhoneAsPrimarySuccess },
        { it == UserProfileAddPhoneViewModel.State.Idle },
      )
    },
    GatedCall("remove a phone number") {
      coEvery { phoneNumber.delete() } coAnswers { gated(mockk(relaxed = true)) }
      val viewModel = UserProfileAddPhoneViewModel()
      viewModel.deletePhoneNumber(phoneNumber)
      stateOf(
        { viewModel.state.value },
        { it == UserProfileAddPhoneViewModel.State.DeletedPhoneNumber },
        { it == UserProfileAddPhoneViewModel.State.Idle },
      )
    },
    GatedCall("sign out of another device") {
      coEvery { user.activeSessions() } returns ClerkResult.success(listOf(session))
      every { session.id } returns "sess_other"
      coEvery { session.revoke() } coAnswers { gated(session) }
      val viewModel = UserProfileSecurityViewModel()
      var error: String? = null
      viewModel.signOut(session) { error = it ?: "error" }
      stateOf(
        { error to viewModel.state.value },
        { (error, state) ->
          error == null && state == UserProfileSecurityViewModel.State.Success(emptyList())
        },
        { (error, state) ->
          error == null && state == UserProfileSecurityViewModel.State.Success(listOf(session))
        },
      )
    },
    GatedCall("change the password") {
      coEvery { user.updatePassword(any()) } coAnswers { gated(user) }
      val viewModel = UserProfileChangePasswordViewModel()
      viewModel.resetPassword("old-password", "new-password", signOutOfOtherSessions = false)
      stateOf(
        { viewModel.state.value },
        { it == UserProfileChangePasswordViewModel.State.Success },
        { it == UserProfileChangePasswordViewModel.State.Idle },
      )
    },
    GatedCall("add a passkey") {
      coEvery { user.createPasskey() } coAnswers { gated(mockk(relaxed = true)) }
      val viewModel = UserProfilePasskeyViewModel()
      viewModel.createPasskey()
      stateOf(
        { viewModel.state.value },
        { it == UserProfilePasskeyViewModel.State.Success },
        { it == UserProfilePasskeyViewModel.State.Idle },
      )
    },
    GatedCall("remove a passkey") {
      val passkey = mockk<Passkey>(relaxed = true)
      coEvery { passkey.delete() } coAnswers { gated(mockk(relaxed = true)) }
      val viewModel = UserProfilePasskeyViewModel()
      viewModel.deletePasskey(passkey)
      stateOf(
        { viewModel.state.value },
        { it == UserProfilePasskeyViewModel.State.Success },
        { it == UserProfilePasskeyViewModel.State.Idle },
      )
    },
    GatedCall("make a phone the default second factor") {
      coEvery { phoneNumber.makeDefaultSecondFactor() } coAnswers { gated(phoneNumber) }
      val viewModel = UserProfileMfaViewModel()
      viewModel.makeDefaultSecondFactor(phoneNumber)
      stateOf(
        { viewModel.state.value },
        { it == UserProfileMfaViewModel.State.Success },
        { it == UserProfileMfaViewModel.State.Idle },
      )
    },
    GatedCall("regenerate backup codes") {
      coEvery { user.createBackupCodes() } coAnswers
        {
          gated(mockk<BackupCodeResource> { every { codes } returns listOf("code") })
        }
      val viewModel = UserProfileMfaViewModel()
      viewModel.regenerateBackupCodes()
      stateOf(
        { viewModel.state.value },
        { it == UserProfileMfaViewModel.State.BackupCodesGenerated(listOf("code")) },
        { it == UserProfileMfaViewModel.State.Idle },
      )
    },
    GatedCall("remove the authenticator app") {
      coEvery { user.disableTotp() } coAnswers { gated(mockk(relaxed = true)) }
      val viewModel = UserProfileMfaViewModel()
      viewModel.deleteTotp()
      stateOf(
        { viewModel.state.value },
        { it == UserProfileMfaViewModel.State.TotpDeleted },
        { it == UserProfileMfaViewModel.State.Idle },
      )
    },
    GatedCall("stop using a phone for SMS codes") {
      coEvery { phoneNumber.delete() } coAnswers { gated(mockk(relaxed = true)) }
      val viewModel = UserProfileMfaViewModel()
      viewModel.deletePhoneNumber(phoneNumber)
      stateOf(
        { viewModel.state.value },
        { it == UserProfileMfaViewModel.State.PhoneNumberDeleted },
        { it == UserProfileMfaViewModel.State.Idle },
      )
    },
    GatedCall("use a phone for SMS codes") {
      coEvery { phoneNumber.setReservedForSecondFactor(true) } coAnswers { gated(phoneNumber) }
      val viewModel = MfaAddSmsViewModel()
      viewModel.reserveForSecondFactor(phoneNumber)
      stateOf(
        { viewModel.state.value },
        { it == MfaAddSmsViewModel.State.Success(phoneNumber) },
        { it == MfaAddSmsViewModel.State.Idle },
      )
    },
    GatedCall("add the authenticator app") {
      coEvery { user.createTotp() } coAnswers { gated(mockk(relaxed = true)) }
      val viewModel = UserProfileMfaTotpViewModel()
      stateOf(
        { viewModel.state.value },
        { it is UserProfileMfaTotpViewModel.State.Success },
        { it == UserProfileMfaTotpViewModel.State.Cancelled },
      )
    },
    GatedCall("connect an account") {
      coEvery { user.createExternalAccount(any()) } coAnswers { gated(mockk(relaxed = true)) }
      val viewModel = AddConnectedAccountViewModel()
      viewModel.connectExternalAccount(OAuthProvider.GOOGLE)
      stateOf(
        { viewModel.state.value },
        { it == AddConnectedAccountViewModel.State.Success },
        { it == AddConnectedAccountViewModel.State.Idle },
      )
    },
    GatedCall("remove a connected account") {
      val externalAccount = mockk<ExternalAccount>(relaxed = true)
      coEvery { externalAccount.delete() } coAnswers { gated(mockk(relaxed = true)) }
      val viewModel = AddConnectedAccountViewModel()
      viewModel.removeConnectedAccount(externalAccount)
      stateOf(
        { viewModel.state.value },
        { it == AddConnectedAccountViewModel.State.ConnectedAccountRemoved },
        { it == AddConnectedAccountViewModel.State.Idle },
      )
    },
    GatedCall("change the username") {
      coEvery { user.update(any()) } coAnswers { gated(user) }
      coEvery { user.get() } returns ClerkResult.success(user)
      val viewModel = UpdateProfileViewModel()
      viewModel.save(firstName = null, lastName = null, username = "new_username")
      stateOf(
        { viewModel.state.value },
        { it == UpdateProfileViewModel.State.Success },
        { it == UpdateProfileViewModel.State.Idle },
      )
    },
    GatedCall("set a new password for the reset-password task") {
      coEvery { user.updatePassword(any()) } coAnswers { gated(user) }
      mockkObject(Client.Companion)
      coEvery { Client.get() } returns ClerkResult.success(mockk(relaxed = true))
      val viewModel = ResetPasswordViewModel()
      viewModel.completeSessionTask("new-password", signOutOtherSessions = false)
      stateOf(
        { viewModel.state.value },
        { it is AuthenticationViewState.Success.SessionTaskComplete },
        { it == AuthenticationViewState.Idle },
      )
    },
    GatedCall("delete the organization") {
      val organization = mockk<Organization>(relaxed = true)
      every { organization.name } returns "Acme"
      coEvery { organization.delete() } coAnswers { gated(mockk(relaxed = true)) }
      val viewModel =
        OrganizationProfileActionConfirmationViewModel(
          dispatcher = dispatcher,
          refreshClient = { ClerkResult.success(mockk(relaxed = true)) },
        )
      viewModel.setConfirmationText("Acme")
      viewModel.confirm(
        OrganizationProfileConfirmationAction.DeleteOrganization,
        organization,
        null,
      )
      stateOf(
        { viewModel.state.value },
        { it.isComplete },
        { !it.isLoading && !it.isComplete && it.errorMessage == null },
      )
    },
    GatedCall("turn on biometric sign-in after signing in") {
      mockkObject(BiometricCredentials)
      coEvery { BiometricCredentials.enroll(any(), any(), any(), any(), any()) } coAnswers
        {
          gated<BiometricCredential>(mockk(relaxed = true))
        }
      val viewModel = BiometricCredentialEnrollmentViewModel()
      viewModel.enroll("Turn on biometric sign-in", null)
      stateOf(
        { viewModel.state.value },
        { it == BiometricCredentialEnrollmentViewModel.State.Enrolled },
        { it == BiometricCredentialEnrollmentViewModel.State.Idle },
      )
    },
    GatedCall("turn on biometric sign-in in the profile") {
      var enrolled = false
      mockkObject(BiometricCredentials)
      every { BiometricCredentials.deviceSupportsBiometricAuthentication } returns true
      coEvery { BiometricCredentials.enroll(any(), any(), any(), any(), any()) } coAnswers
        {
          gated<BiometricCredential>(mockk(relaxed = true)).also {
            enrolled = it is ClerkResult.Success
          }
        }
      coEvery { BiometricCredentials.currentUserAvailability() } answers
        {
          if (enrolled) {
            BiometricCredentialAvailability.Available
          } else {
            BiometricCredentialAvailability.Unavailable(
              BiometricCredentialAvailability.UnavailableReason.NO_LOCAL_CREDENTIAL
            )
          }
        }
      val viewModel = UserProfileBiometricCredentialViewModel(workDispatcher = dispatcher)
      viewModel.setBiometricSignInEnabled(true, "Turn on biometric sign-in", null)
      stateOf(
        { viewModel.state.value },
        { it.isEnabled && it.error == null },
        { !it.isEnabled && !it.isLoading && it.error == null },
      )
    },
    GatedCall("turn off biometric sign-in in the profile") {
      var revoked = false
      mockkObject(BiometricCredentials)
      every { BiometricCredentials.deviceSupportsBiometricAuthentication } returns true
      every { BiometricCredentials.currentUserLocalAvailability() } returns
        BiometricCredentialAvailability.Available
      coEvery { BiometricCredentials.revokeCurrentBiometricCredential() } coAnswers
        {
          gated(Unit).also { revoked = it is ClerkResult.Success }
        }
      coEvery { BiometricCredentials.currentUserAvailability() } answers
        {
          if (revoked) {
            BiometricCredentialAvailability.Unavailable(
              BiometricCredentialAvailability.UnavailableReason.NO_LOCAL_CREDENTIAL
            )
          } else {
            BiometricCredentialAvailability.Available
          }
        }
      val viewModel = UserProfileBiometricCredentialViewModel(workDispatcher = dispatcher)
      viewModel.refreshAvailability()
      viewModel.setBiometricSignInEnabled(false, "Turn off biometric sign-in", null)
      stateOf(
        { viewModel.state.value },
        { !it.isEnabled && it.error == null },
        { it.isEnabled && !it.isLoading && it.error == null },
      )
    },
  )

@OptIn(ExperimentalCoroutinesApi::class)
class ReverifiedCallEdgeCasesTest {
  private val dispatcher = UnconfinedTestDispatcher()
  @get:Rule val dispatcherRule = MainDispatcherRule(dispatcher)

  private val fixture = GatedCallFixture(dispatcher)
  private var duringPresent: () -> Unit = {}

  @Before
  fun setUp() {
    stubGatedCallDependencies(fixture)
    mockkObject(Reverifier)
    every { Reverifier.shared } returns
      Reverifier(
        currentIdentity = { SignedInIdentity(clientId = "client_1", sessionId = "sess_1") },
        present = { _, _ ->
          duringPresent()
          ReverificationOutcome.VERIFIED
        },
      )
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun theResetTaskIsNotSentAgainWhenTheScreenAlreadyResetThePassword() =
    runTest(dispatcher) {
      every { Clerk.session } returns sessionWithTasks(SessionTask("reset-password"))
      duringPresent = { every { Clerk.session } returns sessionWithTasks() }
      coEvery { fixture.user.updatePassword(any()) } coAnswers { fixture.gated(fixture.user) }
      mockkObject(Client.Companion)
      coEvery { Client.get() } returns ClerkResult.success(mockk(relaxed = true))

      val viewModel = ResetPasswordViewModel()
      viewModel.completeSessionTask("new-password", signOutOtherSessions = false)
      awaitUntil { viewModel.state.value is AuthenticationViewState.Success.SessionTaskComplete }

      assertEquals(1, fixture.requests)
    }

  @Test
  fun theResetTaskIsSentAgainWhenItIsStillPending() =
    runTest(dispatcher) {
      every { Clerk.session } returns sessionWithTasks(SessionTask("reset-password"))
      coEvery { fixture.user.updatePassword(any()) } coAnswers { fixture.gated(fixture.user) }
      mockkObject(Client.Companion)
      coEvery { Client.get() } returns ClerkResult.success(mockk(relaxed = true))

      val viewModel = ResetPasswordViewModel()
      viewModel.completeSessionTask("new-password", signOutOtherSessions = false)
      awaitUntil { viewModel.state.value is AuthenticationViewState.Success.SessionTaskComplete }

      assertEquals(2, fixture.requests)
    }

  @Test
  fun aDeviceTheScreenAlreadySignedOutCountsAsSignedOutOnTheRetry() =
    runTest(dispatcher) {
      val session = fixture.session
      every { session.id } returns "sess_other"
      coEvery { fixture.user.activeSessions() } returns ClerkResult.success(listOf(session))
      coEvery { session.revoke() } coAnswers
        {
          if (fixture.requests++ == 0) reverificationRequired else invalidActionForSession
        }
      var error: String? = null

      val viewModel = UserProfileSecurityViewModel()
      viewModel.signOut(session) { error = it }

      assertEquals(2, fixture.requests)
      assertEquals(null, error)
      assertEquals(UserProfileSecurityViewModel.State.Success(emptyList()), viewModel.state.value)
    }

  @Test
  fun invalidActionForSessionOnTheFirstTryStillShowsTheError() =
    runTest(dispatcher) {
      val session = fixture.session
      every { session.id } returns "sess_other"
      coEvery { fixture.user.activeSessions() } returns ClerkResult.success(listOf(session))
      coEvery { session.revoke() } returns invalidActionForSession
      var error: String? = null

      val viewModel = UserProfileSecurityViewModel()
      viewModel.signOut(session) { error = it }

      assertEquals("Session is already signed out", error)
    }

  @Test
  fun theAuthenticatorAppSheetClosesOnCancelAndStartsAgainWhenReopened() =
    runTest(dispatcher) {
      coEvery { fixture.user.createTotp() } coAnswers { fixture.gated(mockk(relaxed = true)) }
      every { Reverifier.shared } returns
        Reverifier(
          currentIdentity = { SignedInIdentity(clientId = "client_1", sessionId = "sess_1") },
          present = { _, _ -> ReverificationOutcome.CANCELLED },
        )

      val viewModel = UserProfileMfaTotpViewModel()
      assertEquals(UserProfileMfaTotpViewModel.State.Cancelled, viewModel.state.value)

      viewModel.acknowledgeCancellation()
      viewModel.createTotpIfIdle()

      assertEquals(2, fixture.requests)
      assertEquals(
        UserProfileMfaTotpViewModel.State.Success::class,
        viewModel.state.value::class,
      )
    }

  private val invalidActionForSession: ClerkResult.Failure<ClerkErrorResponse> =
    ClerkResult.apiFailure(
      ClerkErrorResponse(
        errors =
          listOf(
            Error(
              code = "invalid_action_for_session",
              longMessage = "Session is already signed out",
            )
          )
      )
    )

  private fun sessionWithTasks(vararg tasks: SessionTask): Session =
    mockk(relaxed = true) {
      every { id } returns "sess_1"
      every { currentTask } returns tasks.firstOrNull()
      every { this@mockk.tasks } returns tasks.toList()
    }
}
