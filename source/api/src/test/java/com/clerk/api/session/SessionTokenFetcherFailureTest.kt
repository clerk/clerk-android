package com.clerk.api.session

import com.auth0.android.jwt.JWT
import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.network.serialization.ClerkResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionTokenFetcherFailureTest {
  private val mockSession = mockk<Session>(relaxed = true)
  private val mockTokenResource = mockk<TokenResource>(relaxed = true)
  private val mockJWTManager = mockk<JWTManager>(relaxed = true)
  private val mockClerkApiService = mockk<SessionApi>(relaxed = true)
  private val sessionTokenFetcher = SessionTokenFetcher(mockJWTManager)

  @Before
  fun setup() {
    every { mockSession.id } returns "session_123"
    every { mockSession.status } returns Session.SessionStatus.ACTIVE
    every { mockJWTManager.createFromString(any()) } returns mockk<JWT>(relaxed = true)

    mockkObject(ClerkApi)
    every { ClerkApi.session } returns mockClerkApiService

    mockkObject(Clerk)
    every { Clerk.session } returns mockSession
    every { Clerk.clearSessionAndUserState() } returns Unit
    every { Clerk.environment } returns null
    every { Clerk.clientFlow } returns MutableStateFlow(null)

    SessionTokensCache.clear()
    mockkObject(SessionTokensCache)
  }

  @After
  fun tearDown() {
    unmockkAll()
    SessionTokensCache.clear()
  }

  @Test
  fun `getTokenResult returns the API failure`() = runTest {
    val errorResponse =
      ClerkErrorResponse(errors = listOf(Error(code = "network_error")), clerkTraceId = "trace_123")
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.apiFailure(errorResponse)

    val result = sessionTokenFetcher.getTokenResult(mockSession)

    assertSame(errorResponse, (result as ClerkResult.Failure).error)
  }

  @Test
  fun `getTokenResult returns the exception that interrupted the request`() = runTest {
    val exception = RuntimeException("Network error")
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } throws exception

    val result = sessionTokenFetcher.getTokenResult(mockSession)

    assertSame(exception, (result as ClerkResult.Failure).throwable)
  }

  @Test
  fun `getTokenResult rethrows cancellation`() = runTest {
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } throws
      CancellationException("caller cancelled")

    val thrown = runCatching { sessionTokenFetcher.getTokenResult(mockSession) }.exceptionOrNull()

    assertTrue(thrown is CancellationException)
  }

  @Test
  fun `getTokenResult reports a pending session`() = runTest {
    every { mockSession.status } returns Session.SessionStatus.PENDING

    val result = sessionTokenFetcher.getTokenResult(mockSession)

    assertEquals(
      SessionTokenFetcher.SESSION_PENDING_ERROR_CODE,
      (result as ClerkResult.Failure).error?.errors?.single()?.code,
    )
  }

  @Test
  fun `getTokenResult reports waiters released by reset as superseded`() = runTest {
    val requestStarted = CompletableDeferred<Unit>()
    val releaseResponse = CompletableDeferred<Unit>()
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } coAnswers
      {
        requestStarted.complete(Unit)
        withContext(NonCancellable) { releaseResponse.await() }
        ClerkResult.success(mockTokenResource)
      }

    val owner = async { sessionTokenFetcher.getTokenResult(mockSession) }
    requestStarted.await()
    val waiter = async { sessionTokenFetcher.getTokenResult(mockSession) }
    yield()

    sessionTokenFetcher.reset()
    releaseResponse.complete(Unit)

    listOf(waiter.await(), owner.await()).forEach { result ->
      assertEquals(
        SessionTokenFetcher.TOKEN_REQUEST_SUPERSEDED_ERROR_CODE,
        (result as ClerkResult.Failure).error?.errors?.single()?.code,
      )
    }
  }

  @Test
  fun `Session fetchToken surfaces the API failure instead of a placeholder`() = runTest {
    val errorResponse =
      ClerkErrorResponse(errors = listOf(Error(code = "session_expired")), clerkTraceId = "trace")
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.apiFailure(errorResponse)

    val result = mockSession.fetchToken(GetTokenOptions(skipCache = true))

    val failure = result as ClerkResult.Failure
    assertSame(errorResponse, failure.error)
    assertEquals("trace", failure.error?.clerkTraceId)
  }
}
