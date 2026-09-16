package com.clerk.api.biometriccredential

import com.clerk.api.Clerk
import com.clerk.api.Constants.Strategy.TRUSTED_DEVICE
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.AuthConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.session.SessionTokenFetcher
import com.clerk.api.session.SessionTokensCache
import com.clerk.api.session.SessionVerification
import com.clerk.api.session.startVerification
import com.clerk.api.session.verifyWithBiometrics
import com.clerk.api.session.verifyWithPassword
import com.clerk.api.user.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BiometricReverificationTest {
  private val keyManager = mockk<BiometricCredentialKeyManager>(relaxed = true)
  private val store = mockk<BiometricCredentialLocalStore>(relaxed = true)
  private val api = mockk<SessionApi>()
  private val signInApi = mockk<SignInApi>(relaxed = true)
  private lateinit var previousKeyManager: BiometricCredentialKeyManager
  private lateinit var previousStore: BiometricCredentialLocalStore
  private lateinit var session: Session
  private lateinit var client: MutableStateFlow<Client?>
  private val calls = mutableListOf<String>()
  private val credential =
    BiometricCredentialLocalRecord(
      id = "td_123",
      localKeyId = "key_123",
      userId = "user_123",
      appIdentifier = "com.clerk.example",
      createdAt = 1,
      updatedAt = 1,
    )
  private val challenge =
    BiometricCredentialChallenge(
      challenge = "challenge",
      challengeId = "challenge_123",
      biometricCredentialId = "td_123",
      clientData =
        "{\"purpose\":\"trusted_device_reverification\", \"challenge_id\":\"challenge_123\"}",
      expiresAt = 1710000000000,
    )

  @Before
  fun setUp() {
    previousKeyManager = BiometricCredentials.keyManager
    previousStore = BiometricCredentials.credentialStore
    BiometricCredentials.keyManager = keyManager
    BiometricCredentials.credentialStore = store
    mockkObject(Clerk, ClerkApi)
    every { ClerkApi.session } returns api
    every { ClerkApi.signIn } returns signInApi
    every { Clerk.applicationId } returns credential.appIdentifier
    val environment = mockk<Environment>()
    every { environment.authConfig } returns
      AuthConfig(
        singleSessionMode = false,
        nativeSettings =
          AuthConfig.NativeSettings(apiEnabled = true, biometricSignInEnabled = true),
      )
    every { Clerk.environment } returns environment
    session =
      checkNotNull(decodedVerification(SessionVerification.Status.NEEDS_FIRST_FACTOR).session)
    val user = mockk<User> { every { id } returns credential.userId }
    val otherUser = mockk<User> { every { id } returns "user_other" }
    val otherSession = session.copy(id = "sess_other", user = otherUser)
    client =
      MutableStateFlow(
        Client(
          sessions = listOf(otherSession, session.copy(user = user)),
          lastActiveSessionId = otherSession.id,
        )
      )
    every { Clerk.clientFlow } returns client
    every { Clerk.session } returns otherSession
    every { Clerk.user } returns otherUser
    every { store.all() } returns listOf(credential)
    every { store.all(any()) } answers
      {
        val appIdentifier = firstArg<String>()
        store.all().filter { it.appIdentifier == appIdentifier }
      }
    every { keyManager.hasKey(any()) } returns true
    every { keyManager.isSupported(any()) } returns true
    coEvery { keyManager.sign(any(), any(), any(), any(), any()) } coAnswers
      {
        calls += "sign"
        BiometricCredentialKeySignature(firstArg(), "signature")
      }
    stubSessionApi()
    SessionTokenFetcher.shared.reset()
    SessionTokensCache.clear()
  }

  private fun stubSessionApi() {
    coEvery { api.startVerification(session.id, any()) } answers
      {
        calls += "start"
        ClerkResult.success(decodedVerification(SessionVerification.Status.NEEDS_FIRST_FACTOR))
      }
    coEvery { api.prepareFirstFactorVerification(session.id, any()) } answers
      {
        calls += "prepare-first"
        ClerkResult.success(prepared(SessionVerification.Level.FIRST_FACTOR))
      }
    coEvery { api.prepareSecondFactorVerification(session.id, any()) } answers
      {
        calls += "prepare-second"
        ClerkResult.success(prepared(SessionVerification.Level.SECOND_FACTOR))
      }
    coEvery { api.attemptFirstFactorVerification(session.id, any()) } answers
      {
        calls += "attempt-first"
        ClerkResult.success(decodedVerification(SessionVerification.Status.COMPLETE))
      }
    coEvery { api.attemptSecondFactorVerification(session.id, any()) } answers
      {
        calls += "attempt-second"
        ClerkResult.success(decodedVerification(SessionVerification.Status.COMPLETE))
      }
  }

  @After
  fun tearDown() {
    BiometricCredentials.keyManager = previousKeyManager
    BiometricCredentials.credentialStore = previousStore
    SessionTokenFetcher.shared.reset()
    SessionTokensCache.clear()
    unmockkAll()
  }

  @Test
  fun `returned session resolves its own user and verifies first factor`() = runTest {
    val started =
      assertIs<ClerkResult.Success<SessionVerification>>(
          session.startVerification(SessionVerification.Level.MULTI_FACTOR)
        )
        .value
    val returnedSession = checkNotNull(started.session)
    assertNull(returnedSession.user)
    SessionTokensCache.setToken("sess_123-organization-", TokenResource(jwt = "stale"))
    SessionTokensCache.setToken("sess_123-template-test", TokenResource(jwt = "stale-template"))
    SessionTokensCache.setToken("sess_other-organization-", TokenResource(jwt = "other"))

    val result =
      assertIs<ClerkResult.Success<SessionVerification>>(
        returnedSession.verifyWithBiometrics("Confirm payment", "Use biometrics")
      )

    assertEquals(SessionVerification.Status.COMPLETE, result.value.status)
    assertEquals(session.id, result.value.session?.id)
    assertEquals("sess_other", Clerk.session?.id)
    assertEquals(listOf("start", "prepare-first", "sign", "attempt-first"), calls)
    coVerify(exactly = 1) { api.prepareFirstFactorVerification(session.id, prepareParams()) }
    coVerify(exactly = 1) { api.attemptFirstFactorVerification(session.id, attemptParams()) }
    coVerify(exactly = 1) {
      keyManager.sign(
        challenge.clientData,
        credential.localKeyId,
        credential.policy,
        "Confirm payment",
        "Use biometrics",
      )
    }
    coVerify(exactly = 0) { signInApi.createSignIn(any()) }
    assertNull(SessionTokensCache.getToken("sess_123-organization-"))
    assertNull(SessionTokensCache.getToken("sess_123-template-test"))
    assertEquals("other", SessionTokensCache.getToken("sess_other-organization-")?.jwt)
  }

  @Test
  fun `password can be followed by biometric second factor on returned session`() = runTest {
    coEvery {
      api.attemptFirstFactorVerification(
        session.id,
        mapOf("strategy" to "password", "password" to "password"),
      )
    } answers
      {
        calls += "password"
        ClerkResult.success(decodedVerification(SessionVerification.Status.NEEDS_SECOND_FACTOR))
      }
    val first =
      assertIs<ClerkResult.Success<SessionVerification>>(session.verifyWithPassword("password"))
        .value
    val result =
      checkNotNull(first.session)
        .verifyWithBiometrics(level = SessionVerification.Level.SECOND_FACTOR)

    assertEquals(
      SessionVerification.Status.COMPLETE,
      assertIs<ClerkResult.Success<SessionVerification>>(result).value.status,
    )
    assertEquals(listOf("password", "prepare-second", "sign", "attempt-second"), calls)
    coVerify(exactly = 1) { api.prepareSecondFactorVerification(session.id, prepareParams()) }
    coVerify(exactly = 1) { api.attemptSecondFactorVerification(session.id, attemptParams()) }
  }

  @Test
  fun `missing matching user never falls back to active account`() = runTest {
    client.value = client.value?.copy(sessions = listOf(checkNotNull(Clerk.session), session))
    every { store.all() } returns listOf(credential.copy(userId = "user_other"))
    assertIs<ClerkResult.Failure<ClerkErrorResponse>>(session.verifyWithBiometrics())
    client.value = client.value?.copy(sessions = listOf(checkNotNull(Clerk.session)))
    assertIs<ClerkResult.Failure<ClerkErrorResponse>>(session.verifyWithBiometrics())
    assertTrue(calls.isEmpty())
  }

  @Test
  fun `another users credential is rejected before network or prompt`() = runTest {
    every { store.all() } returns listOf(credential.copy(userId = "user_other"))
    assertIs<ClerkResult.Failure<ClerkErrorResponse>>(session.verifyWithBiometrics())
    assertTrue(calls.isEmpty())
  }

  @Test
  fun `unsupported levels and inactive sessions fail locally`() = runTest {
    for (level in
      listOf(SessionVerification.Level.MULTI_FACTOR, SessionVerification.Level.UNKNOWN)) {
      assertIs<ClerkResult.Failure<ClerkErrorResponse>>(session.verifyWithBiometrics(level = level))
    }
    assertIs<ClerkResult.Failure<ClerkErrorResponse>>(
      session.copy(status = Session.SessionStatus.ENDED).verifyWithBiometrics()
    )
    assertTrue(calls.isEmpty())
  }

  @Test
  fun `disabled biometric settings prevent reverification`() = runTest {
    val environment = checkNotNull(Clerk.environment)
    every { environment.authConfig } returns
      AuthConfig(
        singleSessionMode = false,
        nativeSettings =
          AuthConfig.NativeSettings(apiEnabled = true, biometricSignInEnabled = false),
      )
    assertIs<ClerkResult.Failure<ClerkErrorResponse>>(session.verifyWithBiometrics())
    assertTrue(calls.isEmpty())
  }

  @Test
  fun `missing or mismatched challenges are never signed`() = runTest {
    for (factor in
      listOf(
        null,
        Verification(strategy = "passkey", biometricCredentialChallenge = challenge),
        Verification(
          strategy = TRUSTED_DEVICE,
          biometricCredentialChallenge = challenge.copy(biometricCredentialId = "td_other"),
        ),
      )) {
      coEvery { api.prepareFirstFactorVerification(any(), any()) } returns
        ClerkResult.success(
          prepared(SessionVerification.Level.FIRST_FACTOR).copy(firstFactorVerification = factor)
        )
      assertIs<ClerkResult.Failure<ClerkErrorResponse>>(session.verifyWithBiometrics())
    }
    coVerify(exactly = 0) { keyManager.sign(any(), any(), any(), any(), any()) }
    coVerify(exactly = 0) { api.attemptFirstFactorVerification(any(), any()) }
  }

  @Test
  fun `canceling prompt preserves credential and permits retry`() = runTest {
    val cancellation =
      BiometricCredentialKeyManagerException(
        BiometricCredentialKeyManagerException.Code.BIOMETRIC_AUTHENTICATION_CANCELED,
        "Canceled",
      )
    coEvery { keyManager.sign(any(), any(), any(), any(), any()) } throws cancellation
    assertSame(
      cancellation,
      assertIs<ClerkResult.Failure<ClerkErrorResponse>>(session.verifyWithBiometrics()).throwable,
    )
    coVerify(exactly = 0) { api.attemptFirstFactorVerification(any(), any()) }
    verify(exactly = 0) { store.delete(any()) }
    coEvery { keyManager.sign(any(), any(), any(), any(), any()) } returns
      BiometricCredentialKeySignature(challenge.clientData, "signature")
    assertIs<ClerkResult.Success<SessionVerification>>(session.verifyWithBiometrics())
  }

  @Test
  fun `coroutine cancellation is propagated without submitting or deleting`() = runTest {
    val promptStarted = CompletableDeferred<Unit>()
    coEvery { keyManager.sign(any(), any(), any(), any(), any()) } coAnswers
      {
        promptStarted.complete(Unit)
        awaitCancellation()
      }
    val task = async { session.verifyWithBiometrics() }
    promptStarted.await()
    task.cancel()
    assertFailsWith<CancellationException> { task.await() }
    coVerify(exactly = 0) { api.attemptFirstFactorVerification(any(), any()) }
    verify(exactly = 0) { store.delete(any()) }
  }

  @Test
  fun `revoked credentials are cleaned up but transient failures are preserved`() = runTest {
    val transient = ClerkResult.httpFailure<ClerkErrorResponse>(500)
    coEvery { api.prepareFirstFactorVerification(any(), any()) } returns transient
    assertSame(transient, session.verifyWithBiometrics())
    verify(exactly = 0) { store.delete(any()) }
    val revoked =
      ClerkResult.apiFailure(
        ClerkErrorResponse(
          errors =
            listOf(
              Error(
                code = "trusted_device_not_registered",
                meta = buildJsonObject { put("param_name", "trusted_device_id") },
              )
            )
        )
      )
    coEvery { api.prepareFirstFactorVerification(any(), any()) } returns revoked
    assertIs<ClerkResult.Failure<ClerkErrorResponse>>(session.verifyWithBiometrics())
    verify(exactly = 1) { store.delete(credential.id) }
    verify(exactly = 1) { keyManager.deleteKey(credential.localKeyId) }
  }

  @Test
  fun `invalidated local key is removed without attempting verification`() = runTest {
    val invalidated =
      BiometricCredentialKeyManagerException(
        BiometricCredentialKeyManagerException.Code.KEY_INVALIDATED,
        "Key invalidated",
      )
    coEvery { keyManager.sign(any(), any(), any(), any(), any()) } throws invalidated
    assertSame(
      invalidated,
      assertIs<ClerkResult.Failure<ClerkErrorResponse>>(session.verifyWithBiometrics()).throwable,
    )
    verify(exactly = 1) { store.delete(credential.id) }
    verify(exactly = 1) { keyManager.deleteKey(credential.localKeyId) }
    coVerify(exactly = 0) { api.attemptFirstFactorVerification(any(), any()) }
  }

  @Test
  fun `pending sessions can reverify and incomplete flows preserve token cache`() = runTest {
    val pendingSession = session.copy(status = Session.SessionStatus.PENDING)
    val incomplete = decodedVerification(SessionVerification.Status.NEEDS_SECOND_FACTOR)
    coEvery { api.attemptFirstFactorVerification(any(), any()) } returns
      ClerkResult.success(incomplete)
    SessionTokensCache.setToken("sess_123-organization-", TokenResource(jwt = "cached"))
    val result =
      assertIs<ClerkResult.Success<SessionVerification>>(pendingSession.verifyWithBiometrics())
    assertEquals(SessionVerification.Status.NEEDS_SECOND_FACTOR, result.value.status)
    assertEquals("cached", SessionTokensCache.getToken("sess_123-organization-")?.jwt)
  }

  @Test
  fun `server attempt failures preserve credentials and existing tokens`() = runTest {
    val failure = ClerkResult.httpFailure<ClerkErrorResponse>(500)
    coEvery { api.attemptSecondFactorVerification(any(), any()) } returns failure
    SessionTokensCache.setToken("sess_123-organization-", TokenResource(jwt = "cached"))
    assertSame(
      failure,
      session.verifyWithBiometrics(level = SessionVerification.Level.SECOND_FACTOR),
    )
    verify(exactly = 0) { store.delete(any()) }
    assertEquals("cached", SessionTokensCache.getToken("sess_123-organization-")?.jwt)
  }

  private fun prepareParams() =
    mapOf("strategy" to TRUSTED_DEVICE, "trusted_device_id" to credential.id)

  private fun attemptParams() =
    prepareParams() +
      mapOf(
        "client_data" to challenge.clientData,
        "signature" to "signature",
        "algorithm" to "ES256",
      )

  private fun prepared(level: SessionVerification.Level): SessionVerification {
    val factor = Verification(strategy = TRUSTED_DEVICE, biometricCredentialChallenge = challenge)
    return if (level == SessionVerification.Level.SECOND_FACTOR) {
      decodedVerification(SessionVerification.Status.NEEDS_SECOND_FACTOR)
        .copy(secondFactorVerification = factor)
    } else {
      decodedVerification(SessionVerification.Status.NEEDS_FIRST_FACTOR)
        .copy(firstFactorVerification = factor)
    }
  }

  private fun decodedVerification(status: SessionVerification.Status): SessionVerification =
    ClerkApi.json.decodeFromString<SessionVerification>(
      """{
        "object": "session_reverification", "status": "${status.name.lowercase()}", "level": "multi_factor",
        "session": { "id": "sess_123", "status": "active", "user": null,
          "expire_at": 1710600000000, "last_active_at": 1710000000000,
          "created_at": 1710000000000, "updated_at": 1710000000000 }
      }"""
    )
}
