package com.clerk.api.restorecredentials

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CreateRestoreCredentialRequest
import androidx.credentials.CreateRestoreCredentialResponse
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetRestoreCredentialOption
import androidx.credentials.RestoreCredential
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException
import com.clerk.api.Clerk
import com.clerk.api.auth.AuthEvent
import com.clerk.api.credentials.CredentialFlowException
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.api.UserApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.deleted.DeletedObject
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.passkeys.Passkey
import com.clerk.api.session.Session
import com.clerk.api.signin.SignIn
import com.clerk.api.user.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RestoreCredentialsTest {

  private lateinit var context: Context
  private lateinit var credentialManager: RestoreCredentialManager
  private lateinit var userApi: UserApi
  private lateinit var signInApi: SignInApi
  private lateinit var session: Session

  @Before
  fun setup() {
    context = RuntimeEnvironment.getApplication()
    credentialManager = mockk()
    userApi = mockk()
    signInApi = mockk()
    session =
      Session(
        id = "session_1",
        status = Session.SessionStatus.ACTIVE,
        expireAt = 0,
        lastActiveAt = 0,
        createdAt = 0,
        updatedAt = 0,
      )

    Clerk.stateStore.reset()
    mockkObject(Clerk)
    every { Clerk.applicationContext } returns WeakReference(context)
    every { Clerk.passkeyFirstFactorIsEnabled } returns true
    every { Clerk.activeSession } returns session
    every { Clerk.session } returns session
    every { Clerk.user } returns mockk<User>(relaxed = true)
    every { Clerk.locale } returns MutableStateFlow(null)
    every { Clerk.clientInitialized } returns false

    mockkObject(ClerkApi)
    every { ClerkApi.user } returns userApi
    every { ClerkApi.signIn } returns signInApi
    coEvery { userApi.updatePasskey(any(), any(), any()) } returns
      ClerkResult.success(preparedPasskey())
    coEvery { userApi.deletePasskey(any(), any()) } returns ClerkResult.success(DeletedObject())

    RestoreCredentials.credentialManager = credentialManager
    RestoreCredentialStore.reset()
  }

  @After
  fun tearDown() {
    RestoreCredentialStore.reset()
    RestoreCredentialStore.directory = { context.noBackupFilesDir }
    unmockkAll()
    Clerk.stateStore.reset()
    RestoreCredentials.credentialManager = RestoreCredentialManagerImpl()
  }

  @Test
  fun `create registers and verifies a cloud-backed restore credential`() = runTest {
    val request = slot<CreateRestoreCredentialRequest>()
    val preparedPasskey = preparedPasskey()
    val credentialResponse = CreateRestoreCredentialResponse(REGISTRATION_RESPONSE_JSON)

    coEvery { userApi.createPasskey(any()) } returns ClerkResult.success(preparedPasskey)
    coEvery { credentialManager.createCredential(eq(context), capture(request)) } returns
      credentialResponse
    coEvery {
      userApi.attemptPasskeyVerification(
        passkeyId = preparedPasskey.id,
        strategy = "passkey",
        publicKeyCredential = REGISTRATION_RESPONSE_JSON,
        sessionId = session.id,
      )
    } returns ClerkResult.success(preparedPasskey)

    val result = RestoreCredentials.create()

    assertTrue(result is ClerkResult.Success)
    assertEquals(REGISTRATION_REQUEST_JSON, request.captured.requestJson)
    assertTrue(request.captured.isCloudBackupEnabled)
    coVerify(exactly = 1) {
      userApi.attemptPasskeyVerification(
        passkeyId = preparedPasskey.id,
        strategy = "passkey",
        publicKeyCredential = REGISTRATION_RESPONSE_JSON,
        sessionId = session.id,
      )
    }
  }

  @Test
  fun `create retries without cloud backup when encrypted backup is unavailable`() = runTest {
    val preparedPasskey = preparedPasskey()
    val credentialResponse = CreateRestoreCredentialResponse(REGISTRATION_RESPONSE_JSON)
    val cloudBackupValues = mutableListOf<Boolean>()

    coEvery { userApi.createPasskey(any()) } returns ClerkResult.success(preparedPasskey)
    coEvery { credentialManager.createCredential(eq(context), any()) } answers
      {
        val request = secondArg<CreateRestoreCredentialRequest>()
        cloudBackupValues += request.isCloudBackupEnabled
        if (request.isCloudBackupEnabled) {
          throw E2eeUnavailableException("Encrypted backup unavailable")
        }
        credentialResponse
      }
    coEvery { userApi.attemptPasskeyVerification(any(), any(), any(), any()) } returns
      ClerkResult.success(preparedPasskey)

    val result = RestoreCredentials.create()

    assertTrue(result is ClerkResult.Success)
    assertEquals(listOf(true, false), cloudBackupValues)
  }

  @Test
  fun `signIn requests only the restore credential and attempts passkey verification`() = runTest {
    every { Clerk.activeSession } returns null
    every { Clerk.session } returns null
    every { Clerk.user } returns null
    val request = slot<GetCredentialRequest>()
    val attemptParams = slot<Map<String, String>>()
    val restoreCredential = mockk<RestoreCredential>()
    val pendingSignIn = pendingSignIn()
    val completedSignIn = SignIn(id = pendingSignIn.id, status = SignIn.Status.COMPLETE)

    every { restoreCredential.authenticationResponseJson } returns AUTHENTICATION_RESPONSE_JSON
    coEvery { signInApi.createSignIn(any()) } returns ClerkResult.success(pendingSignIn)
    coEvery { credentialManager.getCredential(eq(context), capture(request)) } returns
      GetCredentialResponse(restoreCredential)
    coEvery { signInApi.attemptFirstFactor(pendingSignIn.id, capture(attemptParams)) } returns
      ClerkResult.success(completedSignIn)

    val result = RestoreCredentials.signIn()

    assertTrue(result is ClerkResult.Success)
    assertSame(completedSignIn, (result as ClerkResult.Success).value)
    val options = request.captured.credentialOptions
    assertEquals(1, options.size)
    assertTrue(options.single() is GetRestoreCredentialOption)
    assertEquals(
      AUTHENTICATION_REQUEST_JSON,
      (options.single() as GetRestoreCredentialOption).requestJson,
    )
    assertEquals("passkey", attemptParams.captured["strategy"])
    assertEquals(AUTHENTICATION_RESPONSE_JSON, attemptParams.captured["public_key_credential"])
  }

  @Test
  fun `signIn classifies a missing restore credential without attempting verification`() = runTest {
    every { Clerk.activeSession } returns null
    every { Clerk.session } returns null
    every { Clerk.user } returns null
    val pendingSignIn = pendingSignIn()

    coEvery { signInApi.createSignIn(any()) } returns ClerkResult.success(pendingSignIn)
    coEvery { credentialManager.getCredential(eq(context), any()) } throws
      NoCredentialException("No restore credential")

    val result = RestoreCredentials.signIn()

    assertTrue(result is ClerkResult.Failure)
    assertTrue(
      (result as ClerkResult.Failure).throwable is CredentialFlowException.NoSavedCredential
    )
    coVerify(exactly = 0) { signInApi.attemptFirstFactor(any(), any()) }
  }

  @Test
  fun `clear deletes only the restore credential state`() = runTest {
    val request = slot<ClearCredentialStateRequest>()
    coEvery { credentialManager.clearCredentialState(eq(context), capture(request)) } returns Unit

    val result = RestoreCredentials.clear()

    assertTrue(result is ClerkResult.Success)
    assertEquals(
      ClearCredentialStateRequest.TYPE_CLEAR_RESTORE_CREDENTIAL,
      request.captured.requestType,
    )
  }

  @Test
  fun `create rethrows cancellation instead of reporting a failure`() = runTest {
    coEvery { userApi.createPasskey(any()) } returns ClerkResult.success(preparedPasskey())
    coEvery { credentialManager.createCredential(eq(context), any()) } throws
      CancellationException("cancelled")

    assertCancels { RestoreCredentials.create() }
    coVerify(exactly = 0) { userApi.attemptPasskeyVerification(any(), any(), any(), any()) }
  }

  @Test
  fun `signIn clears its sign-in attempt and rethrows cancellation`() = runTest {
    val signIn = pendingSignIn()
    val client = Client(id = "client_1", signIn = signIn)
    every { Clerk.activeSession } returns null
    every { Clerk.session } returns null
    every { Clerk.user } returns null
    Clerk.updateClient(client)
    coEvery { signInApi.createSignIn(any()) } returns ClerkResult.success(signIn)
    coEvery { credentialManager.getCredential(eq(context), any()) } throws
      CancellationException("cancelled")

    assertCancels { RestoreCredentials.signIn() }

    assertEquals(client.copy(signIn = null), Clerk.client)
    coVerify(exactly = 0) { signInApi.attemptFirstFactor(any(), any()) }
  }

  @Test
  fun `signIn rethrows cancellation when clearing its sign-in attempt fails`() = runTest {
    val signIn = pendingSignIn()
    val cleanupFailure = IllegalStateException("cleanup failed")
    every { Clerk.activeSession } returns null
    every { Clerk.session } returns null
    every { Clerk.user } returns null
    Clerk.updateClient(Client(id = "client_1", signIn = signIn))
    every { Clerk.mutateClient(any()) } throws cleanupFailure
    coEvery { signInApi.createSignIn(any()) } returns ClerkResult.success(signIn)
    coEvery { credentialManager.getCredential(eq(context), any()) } throws
      CancellationException("cancelled")

    val cancellation = assertCancels { RestoreCredentials.signIn() }

    assertEquals(listOf(cleanupFailure), cancellation.suppressedIncludingRecoveredCause())
  }

  @Test
  fun `clear rethrows cancellation instead of reporting a failure`() = runTest {
    coEvery { credentialManager.clearCredentialState(eq(context), any()) } throws
      CancellationException("cancelled")

    assertCancels { RestoreCredentials.clear() }
  }

  @Test
  @Config(sdk = [27])
  fun `create reports restore credentials unavailable before Android 9`() = runTest {
    val result = RestoreCredentials.create()

    assertTrue(result is ClerkResult.Failure)
    assertTrue(
      (result as ClerkResult.Failure).throwable is CredentialFlowException.ProviderUnavailable
    )
    coVerify(exactly = 0) { userApi.createPasskey(any()) }
    coVerify(exactly = 0) { credentialManager.createCredential(any(), any()) }
  }

  @Test
  fun `create rejects signed-out clients before preparing a server credential`() = runTest {
    every { Clerk.activeSession } returns null
    every { Clerk.session } returns null
    every { Clerk.user } returns null

    val result = RestoreCredentials.create()

    assertTrue(result is ClerkResult.Failure)
    val throwable = (result as ClerkResult.Failure).throwable
    assertTrue(throwable is IllegalStateException)
    assertEquals(
      "A signed-in user is required to create a restore credential.",
      throwable?.message,
    )
    coVerify(exactly = 0) { userApi.createPasskey(any()) }
  }

  @Test
  fun `create names the restore credential passkey`() = runTest {
    stubSuccessfulCreate()

    RestoreCredentials.create()

    coVerify(exactly = 1) {
      userApi.updatePasskey(
        passkeyId = "passkey_1",
        name = "Android restore key",
        sessionId = any(),
      )
    }
  }

  @Test
  fun `createIfMissing creates one restore credential per user on this device`() = runTest {
    signedInAs("user_1")
    stubSuccessfulCreate()

    val first = RestoreCredentials.createIfMissing()
    val second = RestoreCredentials.createIfMissing()

    assertTrue(first is ClerkResult.Success)
    assertTrue(second is ClerkResult.Success)
    coVerify(exactly = 1) { userApi.createPasskey(any()) }
  }

  @Test
  fun `concurrent createIfMissing calls create one restore credential`() = runTest {
    signedInAs("user_1")
    stubSuccessfulCreate()

    val first = async { RestoreCredentials.createIfMissing() }
    val second = async { RestoreCredentials.createIfMissing() }
    first.await()
    second.await()

    coVerify(exactly = 1) { userApi.createPasskey(any()) }
  }

  @Test
  fun `the device restore credential belongs to the most recent user`() = runTest {
    stubSuccessfulCreate()

    signedInAs("user_1")
    RestoreCredentials.createIfMissing()
    signedInAs("user_2")
    RestoreCredentials.createIfMissing()

    coVerify(exactly = 2) { userApi.createPasskey(any()) }
    coVerify(exactly = 0) { userApi.deletePasskey(any(), any()) }
    val state = RestoreCredentialStore.read()
    assertEquals("user_2", state.deviceKeyUserId)
    assertEquals(RestorePasskey(userId = "user_2", passkeyId = "passkey_2"), state.createdPasskey)
  }

  @Test
  fun `re-creating for the same user deletes the passkey this device created before`() = runTest {
    signedInAs("user_1")
    stubSuccessfulCreate()
    coEvery { credentialManager.clearCredentialState(eq(context), any()) } returns Unit

    RestoreCredentials.createIfMissing()
    RestoreCredentials.clear()
    RestoreCredentials.createIfMissing()

    coVerify(exactly = 2) { userApi.createPasskey(any()) }
    coVerify(exactly = 1) { userApi.deletePasskey(passkeyId = "passkey_1", sessionId = any()) }
    assertEquals(
      RestorePasskey(userId = "user_1", passkeyId = "passkey_2"),
      RestoreCredentialStore.read().createdPasskey,
    )
  }

  @Test
  fun `createIfMissing skips instances where passkeys are not a first factor`() = runTest {
    signedInAs("user_1")
    every { Clerk.passkeyIsEnabled } returns true
    every { Clerk.passkeyFirstFactorIsEnabled } returns false

    val result = RestoreCredentials.createIfMissing()

    assertTrue(result is ClerkResult.Success)
    coVerify(exactly = 0) { userApi.createPasskey(any()) }
    coVerify(exactly = 0) { credentialManager.createCredential(any(), any()) }
  }

  @Test
  fun `createIfMissing skips creation when it cannot record the restore credential`() = runTest {
    signedInAs("user_1")
    stubSuccessfulCreate()
    val notADirectory = File.createTempFile("restore", "state").apply { deleteOnExit() }
    RestoreCredentialStore.directory = { notADirectory }

    val first = RestoreCredentials.createIfMissing()
    val second = RestoreCredentials.createIfMissing()

    assertTrue(first is ClerkResult.Failure)
    assertTrue(second is ClerkResult.Failure)
    coVerify(exactly = 0) { userApi.createPasskey(any()) }
  }

  @Test
  fun `a failed createIfMissing does not emit an auth error and can be retried`() = runTest {
    signedInAs("user_1")
    val events = mutableListOf<AuthEvent>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      Clerk.auth.events.collect(events::add)
    }
    coEvery { userApi.createPasskey(any()) } returns
      ClerkResult.apiFailure(ClerkErrorResponse(errors = emptyList()))

    val result = RestoreCredentials.createIfMissing()
    RestoreCredentials.createIfMissing()

    assertTrue(result is ClerkResult.Failure)
    assertTrue(events.none { it is AuthEvent.Error })
    assertNull(RestoreCredentialStore.read().deviceKeyUserId)
    coVerify(exactly = 2) { userApi.createPasskey(any()) }
  }

  @Test
  fun `clear during createIfMissing stops it before it records the restore credential`() = runTest {
    signedInAs("user_1")
    stubSuccessfulCreate()
    coEvery { credentialManager.createCredential(eq(context), any()) } coAnswers
      {
        awaitCancellation()
      }
    coEvery { credentialManager.clearCredentialState(eq(context), any()) } returns Unit

    val creation = async { RestoreCredentials.createIfMissing() }
    runCurrent()
    val cleared = RestoreCredentials.clear()

    assertTrue(cleared is ClerkResult.Success)
    assertTrue(creation.await() is ClerkResult.Failure)
    assertNull(RestoreCredentialStore.read().deviceKeyUserId)
    coVerify(exactly = 0) { userApi.attemptPasskeyVerification(any(), any(), any(), any()) }
    coVerify(exactly = 1) { credentialManager.clearCredentialState(eq(context), any()) }
  }

  @Test
  fun `createIfMissing skips the user who signed in with the restore credential`() = runTest {
    signedOut()
    stubRestoreCredential()
    coEvery { signInApi.attemptFirstFactor("sign_in_1", any()) } returns
      ClerkResult.success(
        SignIn(
          id = "sign_in_1",
          status = SignIn.Status.COMPLETE,
          createdSessionId = "session_restored",
        )
      )
    stubSuccessfulCreate()

    RestoreCredentials.signInOnFirstLaunch()
    signedInAs("user_1", sessionId = "session_restored")
    RestoreCredentials.createIfMissing()

    coVerify(exactly = 0) { userApi.createPasskey(any()) }
    assertEquals("user_1", RestoreCredentialStore.read().deviceKeyUserId)
  }

  @Test
  fun `a restore credential sign-in that needed MFA does not create another one`() = runTest {
    signedOut()
    stubRestoreCredential()
    coEvery { signInApi.attemptFirstFactor("sign_in_1", any()) } returns
      ClerkResult.success(SignIn(id = "sign_in_1", status = SignIn.Status.NEEDS_SECOND_FACTOR))
    stubSuccessfulCreate()

    RestoreCredentials.signInOnFirstLaunch()
    RestoreCredentials.onSignInCompleted(
      SignIn(id = "sign_in_1", status = SignIn.Status.COMPLETE, createdSessionId = "session_mfa")
    )
    signedInAs("user_1", sessionId = "session_mfa")
    RestoreCredentials.createIfMissing()

    coVerify(exactly = 0) { userApi.createPasskey(any()) }
    assertEquals("user_1", RestoreCredentialStore.read().deviceKeyUserId)
  }

  @Test
  fun `another sign-in completing after a restore credential sign-in still creates one`() =
    runTest {
      signedOut()
      stubRestoreCredential()
      coEvery { signInApi.attemptFirstFactor("sign_in_1", any()) } returns
        ClerkResult.success(SignIn(id = "sign_in_1", status = SignIn.Status.NEEDS_SECOND_FACTOR))
      stubSuccessfulCreate()

      RestoreCredentials.signInOnFirstLaunch()
      RestoreCredentials.onSignInCompleted(
        SignIn(id = "sign_in_2", status = SignIn.Status.COMPLETE, createdSessionId = "session_2")
      )
      signedInAs("user_2", sessionId = "session_2")
      RestoreCredentials.createIfMissing()

      coVerify(exactly = 1) { userApi.createPasskey(any()) }
    }

  @Test
  fun `signInOnFirstLaunch does not try again after finding no restore credential`() = runTest {
    signedOut()
    coEvery { signInApi.createSignIn(any()) } returns ClerkResult.success(pendingSignIn())
    coEvery { credentialManager.getCredential(eq(context), any()) } throws
      NoCredentialException("No restore credential")

    val first = RestoreCredentials.signInOnFirstLaunch()
    val second = RestoreCredentials.signInOnFirstLaunch()

    assertTrue(
      (first as ClerkResult.Failure).throwable is CredentialFlowException.NoSavedCredential
    )
    assertTrue(second is ClerkResult.Failure)
    coVerify(exactly = 1) { signInApi.createSignIn(any()) }
    coVerify(exactly = 1) { credentialManager.getCredential(any(), any()) }
  }

  @Test
  fun `signInOnFirstLaunch does not try again after signing in`() = runTest {
    signedOut()
    stubRestoreCredential()
    coEvery { signInApi.attemptFirstFactor("sign_in_1", any()) } returns
      ClerkResult.success(SignIn(id = "sign_in_1", status = SignIn.Status.COMPLETE))

    RestoreCredentials.signInOnFirstLaunch()
    RestoreCredentials.signInOnFirstLaunch()

    coVerify(exactly = 1) { signInApi.createSignIn(any()) }
  }

  @Test
  fun `signInOnFirstLaunch tries again after a network failure`() = runTest {
    signedOut()
    coEvery { signInApi.createSignIn(any()) } returns
      ClerkResult.unknownFailure(IOException("offline"))

    RestoreCredentials.signInOnFirstLaunch()
    RestoreCredentials.signInOnFirstLaunch()

    coVerify(exactly = 2) { signInApi.createSignIn(any()) }
  }

  @Test
  fun `clear keeps the first-launch marker`() = runTest {
    signedOut()
    coEvery { signInApi.createSignIn(any()) } returns ClerkResult.success(pendingSignIn())
    coEvery { credentialManager.getCredential(eq(context), any()) } throws
      NoCredentialException("No restore credential")
    coEvery { credentialManager.clearCredentialState(eq(context), any()) } returns Unit

    RestoreCredentials.signInOnFirstLaunch()
    RestoreCredentials.clear()
    RestoreCredentials.signInOnFirstLaunch()

    coVerify(exactly = 1) { signInApi.createSignIn(any()) }
  }

  private fun signedInAs(userId: String, sessionId: String = session.id) {
    val signedInSession = session.copy(id = sessionId)
    every { Clerk.activeSession } returns signedInSession
    every { Clerk.session } returns signedInSession
    every { Clerk.user } returns mockk<User>(relaxed = true) { every { id } returns userId }
  }

  private fun signedOut() {
    every { Clerk.activeSession } returns null
    every { Clerk.session } returns null
    every { Clerk.user } returns null
  }

  private fun stubRestoreCredential() {
    val restoreCredential = mockk<RestoreCredential>()
    every { restoreCredential.authenticationResponseJson } returns AUTHENTICATION_RESPONSE_JSON
    coEvery { signInApi.createSignIn(any()) } returns ClerkResult.success(pendingSignIn())
    coEvery { credentialManager.getCredential(eq(context), any()) } returns
      GetCredentialResponse(restoreCredential)
  }

  private fun stubSuccessfulCreate() {
    coEvery { userApi.createPasskey(any()) } returnsMany
      listOf(
        ClerkResult.success(preparedPasskey("passkey_1")),
        ClerkResult.success(preparedPasskey("passkey_2")),
      )
    coEvery { credentialManager.createCredential(eq(context), any()) } returns
      CreateRestoreCredentialResponse(REGISTRATION_RESPONSE_JSON)
    coEvery { userApi.attemptPasskeyVerification(any(), any(), any(), any()) } returns
      ClerkResult.success(preparedPasskey())
  }

  private suspend fun assertCancels(block: suspend () -> ClerkResult<*, *>): CancellationException {
    val result =
      try {
        block()
      } catch (e: CancellationException) {
        return e
      }
    throw AssertionError("Expected CancellationException, got $result")
  }

  // kotlinx.coroutines stack-trace recovery may rethrow a copy whose cause is the original
  // exception, so suppressed exceptions can sit one level down.
  private fun Throwable.suppressedIncludingRecoveredCause(): List<Throwable> =
    generateSequence(this) { it.cause }.flatMap { it.suppressed.asSequence() }.toList()

  private fun preparedPasskey(id: String = "passkey_1"): Passkey {
    return Passkey(
      id = id,
      name = "Restore credential",
      verification = Verification(nonce = REGISTRATION_REQUEST_JSON),
      createdAt = 0,
      updatedAt = 0,
    )
  }

  private fun pendingSignIn(): SignIn {
    return SignIn(
      id = "sign_in_1",
      status = SignIn.Status.NEEDS_FIRST_FACTOR,
      firstFactorVerification = Verification(nonce = AUTHENTICATION_REQUEST_JSON),
    )
  }

  private companion object {
    const val REGISTRATION_REQUEST_JSON = """{"challenge":"challenge","user":{"id":"dXNlcl8x"}}"""
    const val REGISTRATION_RESPONSE_JSON =
      """{"id":"credential_1","type":"public-key","response":{}}"""
    const val AUTHENTICATION_REQUEST_JSON = """{"challenge":"challenge"}"""
    const val AUTHENTICATION_RESPONSE_JSON =
      """{"id":"credential_1","type":"public-key","response":{}}"""
  }
}
