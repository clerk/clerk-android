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
import com.clerk.api.credentials.CredentialFlowException
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.api.UserApi
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
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

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

    mockkObject(Clerk)
    every { Clerk.applicationContext } returns WeakReference(context)
    every { Clerk.activeSession } returns session
    every { Clerk.session } returns session
    every { Clerk.user } returns mockk<User>(relaxed = true)
    every { Clerk.locale } returns MutableStateFlow(null)
    every { Clerk.clientInitialized } returns false

    mockkObject(ClerkApi)
    every { ClerkApi.user } returns userApi
    every { ClerkApi.signIn } returns signInApi

    RestoreCredentials.credentialManager = credentialManager
  }

  @After
  fun tearDown() {
    unmockkAll()
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
    assertFalse(result is ClerkResult.Success)
    coVerify(exactly = 0) { userApi.createPasskey(any()) }
  }

  private fun preparedPasskey(): Passkey {
    return Passkey(
      id = "passkey_1",
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
