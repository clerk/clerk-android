package com.clerk.api.auth

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.ClientApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult
import com.clerk.api.sso.SSOService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthTest {

  @Before
  fun setup() {
    Clerk.updateClient(Client())
  }

  @After
  fun tearDown() {
    unmockkAll()
    Clerk.updateClient(Client())
  }

  @Test
  fun `signUpWithGoogleOneTap delegates to Google sign-up flow`() = runTest {
    mockkObject(SignUp.Companion)
    val signUp = mockk<SignUp>(relaxed = true)
    val oauthResult = OAuthResult(signUp = signUp)
    coEvery { SignUp.authenticateWithGoogleOneTap() } returns ClerkResult.success(oauthResult)

    val result = Auth().signUpWithGoogleOneTap()

    assertTrue(result is ClerkResult.Success)
    assertSame(oauthResult, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) { SignUp.authenticateWithGoogleOneTap() }
  }

  @Test
  fun `signUpWithOAuth delegates to sign-up redirect flow`() = runTest {
    mockkObject(SSOService)
    val signUp = mockk<SignUp>(relaxed = true)
    val oauthResult = OAuthResult(signUp = signUp)
    coEvery {
      SSOService.authenticateSignUpWithRedirect(
        strategy = "oauth_google",
        redirectUrl = any(),
        identifier = null,
        emailAddress = null,
        legalAccepted = null,
      )
    } returns ClerkResult.success(oauthResult)

    val result = Auth().signUpWithOAuth(OAuthProvider.GOOGLE)

    assertTrue(result is ClerkResult.Success)
    assertSame(oauthResult, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) {
      SSOService.authenticateSignUpWithRedirect(
        strategy = "oauth_google",
        redirectUrl = any(),
        identifier = null,
        emailAddress = null,
        legalAccepted = null,
      )
    }
  }

  @Test
  fun `signUpWithGoogleOneTap emits auth error event on failure`() = runTest {
    mockkObject(SignUp.Companion)
    val error =
      ClerkErrorResponse(
        errors =
          listOf(
            Error(
              code = "external_account_exists",
              message = "Account already exists",
              longMessage = "Account already exists. Use sign in instead.",
            )
          ),
        clerkTraceId = "trace_123",
      )
    coEvery { SignUp.authenticateWithGoogleOneTap() } returns ClerkResult.apiFailure(error)

    val auth = Auth()
    val events = mutableListOf<AuthEvent>()
    val eventJob =
      launch(start = CoroutineStart.UNDISPATCHED) { auth.events.take(1).toList(events) }

    val result = auth.signUpWithGoogleOneTap()

    withTimeout(1_000) { eventJob.join() }

    assertTrue(result is ClerkResult.Failure)
    assertEquals(error, (result as ClerkResult.Failure).error)
    assertTrue(events.single() is AuthEvent.Error)
    assertEquals(
      "Account already exists. Use sign in instead.",
      (events.single() as AuthEvent.Error).message,
    )
    coVerify(exactly = 1) { SignUp.authenticateWithGoogleOneTap() }
  }

  @Test
  fun `sessions exposes all sessions on the current client`() {
    val firstSession = testSession("sess_1")
    val secondSession = testSession("sess_2")
    Clerk.updateClient(
      Client(
        id = "client_123",
        sessions = listOf(firstSession, secondSession),
        lastActiveSessionId = firstSession.id,
      )
    )

    assertEquals(listOf(firstSession, secondSession), Auth().sessions)
  }

  @Test
  fun `signOut with session ID removes only that session locally`() = runTest {
    val firstSession = testSession("sess_1")
    val secondSession = testSession("sess_2")
    val sessionApi = mockk<SessionApi>()
    val clientApi = mockk<ClientApi>()
    mockkObject(ClerkApi)
    every { ClerkApi.session } returns sessionApi
    every { ClerkApi.client } returns clientApi
    coEvery { sessionApi.removeSession(firstSession.id) } returns ClerkResult.success(firstSession)
    coEvery { clientApi.get() } returns
      ClerkResult.apiFailure(ClerkErrorResponse(errors = emptyList()))
    Clerk.updateClient(
      Client(
        id = "client_123",
        sessions = listOf(firstSession, secondSession),
        lastActiveSessionId = firstSession.id,
      )
    )

    val result = Auth().signOut(sessionId = firstSession.id)

    assertTrue(result is ClerkResult.Success)
    assertEquals(listOf(secondSession), Clerk.client.sessions)
    assertEquals(secondSession.id, Clerk.client.lastActiveSessionId)
    assertEquals(secondSession, Clerk.sessionFlow.value)
    coVerify(exactly = 1) { sessionApi.removeSession(firstSession.id) }
  }

  @Test
  fun `setActive updates the active session locally`() = runTest {
    val firstSession = testSession("sess_1")
    val secondSession = testSession("sess_2")
    val clientApi = mockk<ClientApi>()
    mockkObject(ClerkApi)
    every { ClerkApi.client } returns clientApi
    coEvery { clientApi.setActive(secondSession.id, null) } returns
      ClerkResult.success(secondSession)
    coEvery { clientApi.get() } returns
      ClerkResult.apiFailure(ClerkErrorResponse(errors = emptyList()))
    Clerk.updateClient(
      Client(
        id = "client_123",
        sessions = listOf(firstSession, secondSession),
        lastActiveSessionId = firstSession.id,
      )
    )

    val result = Auth().setActive(sessionId = secondSession.id)

    assertTrue(result is ClerkResult.Success)
    assertEquals(secondSession.id, Clerk.client.lastActiveSessionId)
    assertEquals(secondSession, Clerk.sessionFlow.value)
    coVerify(exactly = 1) { clientApi.setActive(secondSession.id, null) }
  }

  private fun testSession(id: String): Session =
    Session(
      id = id,
      status = Session.SessionStatus.ACTIVE,
      expireAt = 10_000,
      lastActiveAt = 1_000,
      createdAt = 1_000,
      updatedAt = 1_000,
    )
}
