package com.clerk.api.session

import com.auth0.android.jwt.JWT
import com.clerk.api.Clerk
import com.clerk.api.auth.Auth
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.network.serialization.ClerkResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
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
class SessionTokenFetcherCancellationTest {
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

    SessionTokensCache.clear()
    mockkObject(SessionTokensCache)
  }

  @After
  fun tearDown() {
    unmockkAll()
    SessionTokensCache.clear()
  }

  @Test
  fun `waiter fetches again when the owner of the shared request is cancelled`() = runTest {
    val calls = AtomicInteger(0)
    val ownerRequestStarted = CompletableDeferred<Unit>()
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { SessionTokensCache.storeIfFresher(any(), any(), any()) } answers
      {
        SessionTokensCache.StoreResult(secondArg(), true)
      }
    coEvery { mockClerkApiService.tokens("session_123") } coAnswers
      {
        if (calls.incrementAndGet() == 1) {
          ownerRequestStarted.complete(Unit)
          CompletableDeferred<Unit>().await()
        }
        ClerkResult.success(mockTokenResource)
      }

    val owner = async { sessionTokenFetcher.getTokenResult(mockSession) }
    ownerRequestStarted.await()
    val waiter = async { sessionTokenFetcher.getTokenResult(mockSession) }
    yield()

    owner.cancel()

    val result = waiter.await()
    assertSame(mockTokenResource, (result as ClerkResult.Success).value)
    assertEquals(2, calls.get())
  }

  @Test
  fun `cancelled waiter still throws cancellation`() = runTest {
    val ownerRequestStarted = CompletableDeferred<Unit>()
    val releaseResponse = CompletableDeferred<Unit>()
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } coAnswers
      {
        ownerRequestStarted.complete(Unit)
        withContext(NonCancellable) { releaseResponse.await() }
        ClerkResult.success(mockTokenResource)
      }

    val owner = async { sessionTokenFetcher.getTokenResult(mockSession) }
    ownerRequestStarted.await()
    val waiter = async { sessionTokenFetcher.getTokenResult(mockSession) }
    yield()

    waiter.cancel()
    val thrown = runCatching { waiter.await() }.exceptionOrNull()
    releaseResponse.complete(Unit)

    assertTrue(thrown is CancellationException)
    assertTrue(owner.await() is ClerkResult.Success)
  }

  @Test
  fun `Auth getToken keeps the network failure type and throwable`() = runTest {
    val networkError = IOException("offline")
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.unknownFailure(networkError)

    val result = Auth().getToken(GetTokenOptions(skipCache = true))

    val failure = result as ClerkResult.Failure
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, failure.errorType)
    assertSame(networkError, failure.throwable)
  }
}
