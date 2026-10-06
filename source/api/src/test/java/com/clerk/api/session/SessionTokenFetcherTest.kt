package com.clerk.api.session

import com.auth0.android.jwt.JWT
import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.AuthConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.network.serialization.ClerkResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionTokenFetcherTest {

  private lateinit var sessionTokenFetcher: SessionTokenFetcher
  private lateinit var mockSession: Session
  private lateinit var mockTokenResource: TokenResource
  private lateinit var mockJWT: JWT
  private lateinit var mockJWTManager: JWTManager
  private lateinit var mockClerkApiService: SessionApi

  @Before
  fun setup() {
    mockSession = mockk(relaxed = true)
    mockTokenResource = mockk(relaxed = true)
    mockJWT = mockk(relaxed = true)
    mockJWTManager = mockk(relaxed = true)
    mockClerkApiService = mockk(relaxed = true)

    sessionTokenFetcher = SessionTokenFetcher(mockJWTManager)

    every { mockSession.id } returns "session_123"
    every { mockSession.status } returns Session.SessionStatus.ACTIVE

    every { mockJWTManager.createFromString(any()) } returns mockJWT

    mockkObject(ClerkApi)
    every { ClerkApi.session } returns mockClerkApiService

    mockkObject(Clerk)
    every { Clerk.session } returns mockSession
    every { Clerk.clearSessionAndUserState() } returns Unit
    isolateFromGlobalClerkState()

    SessionTokensCache.clear()
    mockkObject(SessionTokensCache)
  }

  private fun isolateFromGlobalClerkState() {
    every { Clerk.environment } returns null
    every { Clerk.clientFlow } returns MutableStateFlow(null)
  }

  @After
  fun tearDown() {
    unmockkAll()
    SessionTokensCache.clear()
  }

  @Test
  fun `getToken returns cached token if valid and cache not skipped`() = runTest {
    val cacheKey = "session_123-organization-"
    val futureTime = Date(System.currentTimeMillis() + 120000) // 2 minutes from now

    every { mockTokenResource.jwt } returns "valid.jwt.token"
    every { mockJWT.expiresAt } returns futureTime
    coEvery { SessionTokensCache.getToken(cacheKey) } returns mockTokenResource

    val result = sessionTokenFetcher.getToken(mockSession)

    assertEquals(mockTokenResource, result)
    coVerify { SessionTokensCache.getToken(cacheKey) }
    coVerify(exactly = 0) { mockClerkApiService.tokens(any()) }
  }

  @Test
  fun `getToken fetches from network if cache is empty`() = runTest {
    val cacheKey = "session_123-organization-"
    val setTokenSlot = slot<TokenResource>()

    coEvery { SessionTokensCache.getToken(cacheKey) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.success(mockTokenResource)
    coEvery { SessionTokensCache.storeIfFresher(cacheKey, capture(setTokenSlot), any()) } returns
      SessionTokensCache.StoreResult(mockTokenResource, true)

    val result = sessionTokenFetcher.getToken(mockSession)

    assertEquals(mockTokenResource, result)
    coVerify { SessionTokensCache.getToken(cacheKey) }
    coVerify { mockClerkApiService.tokens("session_123") }
    coVerify { SessionTokensCache.storeIfFresher(cacheKey, mockTokenResource, any()) }
    assertEquals(mockTokenResource, setTokenSlot.captured)
  }

  @Test
  fun `getToken fetches from network if cached token is expired`() = runTest {
    val cacheKey = "session_123-organization-"
    val pastTime = Date(System.currentTimeMillis() - 60000) // 1 minute ago
    val freshToken = mockk<TokenResource>(relaxed = true)

    every { mockTokenResource.jwt } returns "expired.jwt.token"
    every { mockJWT.expiresAt } returns pastTime
    coEvery { SessionTokensCache.getToken(cacheKey) } returns mockTokenResource
    coEvery { mockClerkApiService.tokens("session_123") } returns ClerkResult.success(freshToken)
    coEvery { SessionTokensCache.storeIfFresher(cacheKey, freshToken, any()) } returns
      SessionTokensCache.StoreResult(freshToken, true)

    val result = sessionTokenFetcher.getToken(mockSession)

    assertEquals(freshToken, result)
    coVerify { SessionTokensCache.getToken(cacheKey) }
    coVerify { mockClerkApiService.tokens("session_123") }
    coVerify { SessionTokensCache.storeIfFresher(cacheKey, freshToken, any()) }
  }

  @Test
  fun `getToken uses template in API call when provided`() = runTest {
    val template = "custom_template"
    val cacheKey = "session_123-template-custom_template"
    val options = GetTokenOptions(template = template)

    coEvery { SessionTokensCache.getToken(cacheKey) } returns null
    coEvery { mockClerkApiService.tokens("session_123", template) } returns
      ClerkResult.success(mockTokenResource)
    coEvery { SessionTokensCache.storeIfFresher(cacheKey, mockTokenResource, any()) } returns
      SessionTokensCache.StoreResult(mockTokenResource, true)

    val result = sessionTokenFetcher.getToken(mockSession, options)

    assertEquals(mockTokenResource, result)
    coVerify { mockClerkApiService.tokens("session_123", template) }
    coVerify { SessionTokensCache.storeIfFresher(cacheKey, mockTokenResource, any()) }
  }

  @Test
  fun `getToken bypasses cached result when skipCache is true`() = runTest {
    val options = GetTokenOptions(skipCache = true)
    val cacheKey = "session_123-organization-"
    val freshToken = mockk<TokenResource>(relaxed = true)

    val stillValidCachedToken = mockTokenResource
    every { stillValidCachedToken.jwt } returns "valid.jwt.token"
    every { mockJWT.expiresAt } returns Date(System.currentTimeMillis() + 120000)
    coEvery { SessionTokensCache.getToken(cacheKey) } returns stillValidCachedToken
    coEvery { mockClerkApiService.tokens("session_123") } returns ClerkResult.success(freshToken)
    coEvery { SessionTokensCache.storeIfFresher(cacheKey, freshToken, any()) } returns
      SessionTokensCache.StoreResult(freshToken, true)

    val result = sessionTokenFetcher.getToken(mockSession, options)

    assertSame(freshToken, result)
    coVerify(exactly = 1) { mockClerkApiService.tokens("session_123") }
    coVerify { SessionTokensCache.storeIfFresher(cacheKey, freshToken, any()) }
  }

  @Test
  fun `session minter passes previous token and forces origin for forced refresh`() = runTest {
    val cacheKey = "session_123-organization-org_123"
    val previousToken = TokenResource("previous.token.value")
    val environment = mockk<Environment>()
    every { environment.authConfig } returns
      AuthConfig(singleSessionMode = false, sessionMinter = true)
    every { Clerk.environment } returns environment
    every { mockSession.lastActiveOrganizationId } returns "org_123"
    every { mockSession.lastActiveToken } returns previousToken
    every { SessionTokensCache.hydrate(cacheKey, previousToken) } returns Unit
    every { SessionTokensCache.getToken(cacheKey) } returns null
    coEvery {
      mockClerkApiService.tokens(
        sessionId = "session_123",
        organizationId = "org_123",
        token = previousToken.jwt,
        forceOrigin = "true",
      )
    } returns ClerkResult.success(mockTokenResource)
    every { SessionTokensCache.storeIfFresher(cacheKey, mockTokenResource, any()) } returns
      SessionTokensCache.StoreResult(mockTokenResource, true)

    val result = sessionTokenFetcher.getToken(mockSession, GetTokenOptions(skipCache = true))

    assertEquals(mockTokenResource, result)
    coVerify {
      mockClerkApiService.tokens(
        sessionId = "session_123",
        organizationId = "org_123",
        token = previousToken.jwt,
        forceOrigin = "true",
      )
    }
  }

  @Test
  fun `getToken returns null when API call fails`() = runTest {
    val error =
      Error(
        code = "network_error",
        message = "Network error",
        longMessage = "Network error occurred",
      )
    val errorResponse = ClerkErrorResponse(errors = listOf(error), clerkTraceId = "trace_123")

    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.apiFailure(errorResponse)

    val result = sessionTokenFetcher.getToken(mockSession)

    assertNull(result)
    coVerify { mockClerkApiService.tokens("session_123") }
    coVerify(exactly = 0) { SessionTokensCache.storeIfFresher(any(), any(), any()) }
    verify(exactly = 0) { Clerk.clearSessionAndUserState() }
  }

  @Test
  fun `getToken clears local session state when token endpoint returns unauthorized`() = runTest {
    val error = Error(code = "session_revoked", message = "Session revoked")
    val errorResponse = ClerkErrorResponse(errors = listOf(error), clerkTraceId = "trace_unauth")

    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.httpFailure(code = 401, error = errorResponse)

    val result = sessionTokenFetcher.getToken(mockSession)

    assertNull(result)
    verify(exactly = 1) { Clerk.clearSessionAndUserState() }
  }

  @Test
  fun `getToken clears local session state when token endpoint returns authentication invalid`() =
    runTest {
      val error = Error(code = "authentication_invalid", message = "Invalid authentication")
      val errorResponse = ClerkErrorResponse(errors = listOf(error), clerkTraceId = "trace_unauth")

      coEvery { SessionTokensCache.getToken(any()) } returns null
      coEvery { mockClerkApiService.tokens("session_123") } returns
        ClerkResult.httpFailure(code = 401, error = errorResponse)

      val result = sessionTokenFetcher.getToken(mockSession)

      assertNull(result)
      verify(exactly = 1) { Clerk.clearSessionAndUserState() }
    }

  @Test
  fun `getToken does not clear local session state for non-session unauthorized errors`() =
    runTest {
      val error = Error(code = "not_authorized", message = "Unauthorized")
      val errorResponse = ClerkErrorResponse(errors = listOf(error), clerkTraceId = "trace_unauth")

      coEvery { SessionTokensCache.getToken(any()) } returns null
      coEvery { mockClerkApiService.tokens("session_123") } returns
        ClerkResult.httpFailure(code = 401, error = errorResponse)

      val result = sessionTokenFetcher.getToken(mockSession)

      assertNull(result)
      verify(exactly = 0) { Clerk.clearSessionAndUserState() }
    }

  @Test
  fun `getToken uses custom expiration buffer`() = runTest {
    // Given
    val customBuffer = 120L // 2 minutes
    val options = GetTokenOptions(expirationBuffer = customBuffer)
    val cacheKey = "session_123-organization-"
    // Token expires in 90 seconds (less than 2-minute buffer)
    val soonExpiredTime = Date(System.currentTimeMillis() + 90000)

    every { mockTokenResource.jwt } returns "soon.expired.token"
    every { mockJWT.expiresAt } returns soonExpiredTime
    coEvery { SessionTokensCache.getToken(cacheKey) } returns mockTokenResource
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.success(mockTokenResource)
    coEvery { SessionTokensCache.storeIfFresher(cacheKey, mockTokenResource, any()) } returns
      SessionTokensCache.StoreResult(mockTokenResource, true)

    val result = sessionTokenFetcher.getToken(mockSession, options)

    assertEquals(mockTokenResource, result)
    // Should fetch from network because token expires within buffer
    coVerify { mockClerkApiService.tokens("session_123") }
  }

  @Test
  fun `getToken handles JWT parsing exception gracefully`() = runTest {
    val cacheKey = "session_123-organization-"

    every { mockTokenResource.jwt } returns "invalid.jwt.token"
    every { mockJWTManager.createFromString(any()) } throws RuntimeException("Invalid JWT")
    coEvery { SessionTokensCache.getToken(cacheKey) } returns mockTokenResource
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.success(mockTokenResource)
    coEvery { SessionTokensCache.storeIfFresher(cacheKey, mockTokenResource, any()) } returns
      SessionTokensCache.StoreResult(mockTokenResource, true)

    val result = sessionTokenFetcher.getToken(mockSession)

    assertEquals(mockTokenResource, result)
    // Should fetch from network because JWT parsing failed
    coVerify { mockClerkApiService.tokens("session_123") }
  }

  @Test
  fun `getToken handles concurrent requests properly`() = runTest {
    val cacheKey = "session_123-organization-"

    coEvery { SessionTokensCache.getToken(cacheKey) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } coAnswers
      {
        delay(100) // Simulate network delay
        ClerkResult.success(mockTokenResource)
      }
    coEvery { SessionTokensCache.storeIfFresher(cacheKey, mockTokenResource, any()) } returns
      SessionTokensCache.StoreResult(mockTokenResource, true)

    val deferred1 = async { sessionTokenFetcher.getToken(mockSession) }
    val deferred2 = async { sessionTokenFetcher.getToken(mockSession) }
    val deferred3 = async { sessionTokenFetcher.getToken(mockSession) }

    val result1 = deferred1.await()
    val result2 = deferred2.await()
    val result3 = deferred3.await()

    assertSame(mockTokenResource, result1)
    assertSame(mockTokenResource, result2)
    assertSame(mockTokenResource, result3)

    coVerify(exactly = 1) { mockClerkApiService.tokens("session_123") }
  }

  @Test
  fun `concurrent waiters share a null token result without retrying`() = runTest {
    val requestStarted = CompletableDeferred<Unit>()
    val releaseRequest = CompletableDeferred<Unit>()
    val errorResponse = ClerkErrorResponse(errors = emptyList(), clerkTraceId = "trace_shared")
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } coAnswers
      {
        requestStarted.complete(Unit)
        releaseRequest.await()
        ClerkResult.apiFailure(errorResponse)
      }

    val first = async { sessionTokenFetcher.getToken(mockSession) }
    requestStarted.await()
    val second = async { sessionTokenFetcher.getToken(mockSession) }
    yield()
    releaseRequest.complete(Unit)

    assertNull(first.await())
    assertNull(second.await())
    coVerify(exactly = 1) { mockClerkApiService.tokens("session_123") }
  }

  @Test
  fun `forced refreshes return their own responses while cache retains canonical token`() =
    runTest {
      val cacheKey = "session_123-organization-"
      val firstCallStarted = CompletableDeferred<Unit>()
      val releaseFirstCall = CompletableDeferred<Unit>()
      val callCount = AtomicInteger()
      val secondToken = mockk<TokenResource>(relaxed = true)
      coEvery { SessionTokensCache.getToken(cacheKey) } returns null
      coEvery { mockClerkApiService.tokens("session_123") } coAnswers
        {
          if (callCount.getAndIncrement() == 0) {
            firstCallStarted.complete(Unit)
            releaseFirstCall.await()
            ClerkResult.success(mockTokenResource)
          } else {
            ClerkResult.success(secondToken)
          }
        }
      coEvery { SessionTokensCache.storeIfFresher(cacheKey, any(), any()) } answers
        {
          val token = secondArg<TokenResource>()
          if (token === secondToken) {
            SessionTokensCache.StoreResult(secondToken, true)
          } else {
            SessionTokensCache.StoreResult(secondToken, false)
          }
        }

      val first = async {
        sessionTokenFetcher.getToken(mockSession, GetTokenOptions(skipCache = true))
      }
      firstCallStarted.await()
      val second = async {
        sessionTokenFetcher.getToken(mockSession, GetTokenOptions(skipCache = true))
      }
      val secondResult = second.await()
      releaseFirstCall.complete(Unit)
      val firstResult = first.await()

      // Then - each forced request returns its own mint for JS parity, even when the cache rejects
      // the first request's late response as stale.
      assertEquals(mockTokenResource, firstResult)
      assertEquals(secondToken, secondResult)
      coVerify(exactly = 2) { mockClerkApiService.tokens("session_123") }
    }

  @Test
  fun `reverification ignores old snapshots and forces origin until refresh succeeds`() = runTest {
    val cacheKey = "session_123-organization-org_123"
    val claims = """{"sid":"session_123","org_id":"org_123","exp":4102444800,"fva":[20,20]}"""
    val payload =
      java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(claims.toByteArray())
    val oldToken = TokenResource("eyJhbGciOiJub25lIn0.$payload.")
    val freshClaims = claims.replace("[20,20]", "[0,0]")
    val freshPayload =
      java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(freshClaims.toByteArray())
    val freshToken = TokenResource("eyJhbGciOiJub25lIn0.$freshPayload.")
    val environment = mockk<Environment>()
    every { environment.authConfig } returns
      AuthConfig(singleSessionMode = false, sessionMinter = true)
    every { Clerk.environment } returns environment
    every { mockSession.lastActiveOrganizationId } returns "org_123"
    every { mockSession.lastActiveToken } returns oldToken
    every { SessionTokensCache.getToken(cacheKey) } returns null
    coEvery {
      mockClerkApiService.tokens("session_123", "org_123", oldToken.jwt, "true")
    } returnsMany listOf(ClerkResult.httpFailure(500), ClerkResult.success(freshToken))
    every { SessionTokensCache.storeIfFresher(cacheKey, freshToken, any()) } returns
      SessionTokensCache.StoreResult(freshToken, true)

    sessionTokenFetcher.invalidateSession("session_123")

    assertNull(sessionTokenFetcher.getToken(mockSession))
    assertEquals(freshToken, sessionTokenFetcher.getToken(mockSession))
    assertEquals(listOf(0, 0), JWTManagerImpl().factorVerificationAgeClaim(freshToken.jwt))
    every { SessionTokensCache.getToken(cacheKey) } returns freshToken
    every { mockJWT.expiresAt } returns Date(System.currentTimeMillis() + 120000)
    assertEquals(freshToken, sessionTokenFetcher.getToken(mockSession))
    verify(exactly = 0) { SessionTokensCache.hydrate(any(), any()) }
    coVerify(exactly = 2) {
      mockClerkApiService.tokens("session_123", "org_123", oldToken.jwt, "true")
    }
  }

  @Test
  fun `reverification releases waiters and rejects pre-verification responses`() = runTest {
    val requestStarted = CompletableDeferred<Unit>()
    val releaseResponse = CompletableDeferred<Unit>()
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } coAnswers
      {
        requestStarted.complete(Unit)
        withContext(NonCancellable) { releaseResponse.await() }
        ClerkResult.success(mockTokenResource)
      }
    val owner = async { sessionTokenFetcher.getToken(mockSession) }
    requestStarted.await()
    val waiter = async { sessionTokenFetcher.getToken(mockSession) }
    yield()

    sessionTokenFetcher.invalidateSession("session_123")
    assertNull(waiter.await())
    val freshToken = TokenResource("fresh.token.value")
    coEvery { mockClerkApiService.tokens("session_123") } returns ClerkResult.success(freshToken)
    assertEquals(freshToken, sessionTokenFetcher.getToken(mockSession))
    releaseResponse.complete(Unit)

    assertNull(owner.await())
    coVerify(exactly = 0) { SessionTokensCache.storeIfFresher(any(), mockTokenResource, any()) }
  }

  @Test
  fun `reverification does not invalidate another sessions in flight request`() = runTest {
    val requestStarted = CompletableDeferred<Unit>()
    val releaseResponse = CompletableDeferred<Unit>()
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } coAnswers
      {
        requestStarted.complete(Unit)
        releaseResponse.await()
        ClerkResult.success(mockTokenResource)
      }
    val request = async { sessionTokenFetcher.getToken(mockSession) }
    requestStarted.await()
    sessionTokenFetcher.invalidateSession("session_other")
    releaseResponse.complete(Unit)
    assertEquals(mockTokenResource, request.await())
  }

  @Test
  fun `reset fences a late forced refresh response from the previous runtime`() = runTest {
    val requestStarted = CompletableDeferred<Unit>()
    val releaseResponse = CompletableDeferred<Unit>()
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } coAnswers
      {
        requestStarted.complete(Unit)
        withContext(NonCancellable) { releaseResponse.await() }
        ClerkResult.success(mockTokenResource)
      }

    val request = async {
      sessionTokenFetcher.getToken(mockSession, GetTokenOptions(skipCache = true))
    }
    requestStarted.await()

    sessionTokenFetcher.reset()
    releaseResponse.complete(Unit)

    assertNull(request.await())
    coVerify(exactly = 0) { SessionTokensCache.storeIfFresher(any(), any(), any()) }
  }

  @Test
  fun `reset releases shared token waiters with null`() = runTest {
    val requestStarted = CompletableDeferred<Unit>()
    val releaseResponse = CompletableDeferred<Unit>()
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } coAnswers
      {
        requestStarted.complete(Unit)
        withContext(NonCancellable) { releaseResponse.await() }
        ClerkResult.success(mockTokenResource)
      }

    val owner = async { sessionTokenFetcher.getToken(mockSession) }
    requestStarted.await()
    val waiter = async { sessionTokenFetcher.getToken(mockSession) }
    yield()

    sessionTokenFetcher.reset()

    assertNull(waiter.await())
    releaseResponse.complete(Unit)
    assertNull(owner.await())
    coVerify(exactly = 0) { SessionTokensCache.storeIfFresher(any(), any(), any()) }
  }

  @Test
  fun `getToken handles API exception gracefully`() = runTest {
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } throws RuntimeException("Network error")

    val result = sessionTokenFetcher.getToken(mockSession)

    assertNull(result)
    coVerify { mockClerkApiService.tokens("session_123") }
    coVerify(exactly = 0) { SessionTokensCache.storeIfFresher(any(), any(), any()) }
  }

  @Test
  fun `tokenCacheKey generates correct key without template`() {
    every { mockSession.id } returns "session_456"

    val cacheKey = mockSession.tokenCacheKey(null)

    assertEquals("session_456-organization-", cacheKey)
  }

  @Test
  fun `tokenCacheKey generates correct key with template`() {
    every { mockSession.id } returns "session_456"
    val template = "admin_template"

    val cacheKey = mockSession.tokenCacheKey(template)

    assertEquals("session_456-template-admin_template", cacheKey)
  }

  @Test
  fun `different sessions get different cache keys`() = runTest {
    val session1 = mockk<Session>(relaxed = true)
    val session2 = mockk<Session>(relaxed = true)
    every { session1.id } returns "session_1"
    every { session2.id } returns "session_2"

    coEvery { SessionTokensCache.getToken("session_1-organization-") } returns null
    coEvery { SessionTokensCache.getToken("session_2-organization-") } returns null
    coEvery { mockClerkApiService.tokens("session_1") } returns
      ClerkResult.success(mockTokenResource)
    coEvery { mockClerkApiService.tokens("session_2") } returns
      ClerkResult.success(mockTokenResource)
    coEvery { SessionTokensCache.storeIfFresher(any(), any(), any()) } answers
      {
        val token = secondArg<TokenResource>()
        SessionTokensCache.StoreResult(token, true)
      }

    sessionTokenFetcher.getToken(session1)
    sessionTokenFetcher.getToken(session2)

    coVerify { mockClerkApiService.tokens("session_1") }
    coVerify { mockClerkApiService.tokens("session_2") }
    coVerify {
      SessionTokensCache.storeIfFresher("session_1-organization-", mockTokenResource, any())
    }
    coVerify {
      SessionTokensCache.storeIfFresher("session_2-organization-", mockTokenResource, any())
    }
  }

  @Test
  fun `getToken returns null for pending session`() = runTest {
    every { mockSession.status } returns Session.SessionStatus.PENDING

    val result = sessionTokenFetcher.getToken(mockSession)

    assertNull(result)
    coVerify(exactly = 0) { SessionTokensCache.getToken(any()) }
    coVerify(exactly = 0) { mockClerkApiService.tokens(any()) }
  }

  @Test
  fun `getToken uses pending status from current client snapshot`() = runTest {
    // Given - the caller holds a stale active session while the current snapshot is pending
    val pendingSession = mockk<Session>(relaxed = true)
    every { pendingSession.id } returns "session_123"
    every { pendingSession.status } returns Session.SessionStatus.PENDING
    every { Clerk.clientFlow } returns MutableStateFlow(Client(sessions = listOf(pendingSession)))

    val result = sessionTokenFetcher.getToken(mockSession)

    assertNull(result)
    coVerify(exactly = 0) { mockClerkApiService.tokens(any()) }
  }

  @Test
  fun `getToken uses active status from current client snapshot`() = runTest {
    // Given - the caller holds a stale pending session while the current snapshot is active
    val activeSession = mockk<Session>(relaxed = true)
    every { mockSession.status } returns Session.SessionStatus.PENDING
    every { activeSession.id } returns "session_123"
    every { activeSession.status } returns Session.SessionStatus.ACTIVE
    every { Clerk.clientFlow } returns MutableStateFlow(Client(sessions = listOf(activeSession)))
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.success(mockTokenResource)
    coEvery { SessionTokensCache.storeIfFresher(any(), mockTokenResource, any()) } returns
      SessionTokensCache.StoreResult(mockTokenResource, true)

    val result = sessionTokenFetcher.getToken(mockSession)

    assertEquals(mockTokenResource, result)
    coVerify(exactly = 1) { mockClerkApiService.tokens("session_123") }
  }

  @Test
  fun `getToken proceeds normally for active session`() = runTest {
    val cacheKey = "session_123-organization-"

    every { mockSession.status } returns Session.SessionStatus.ACTIVE
    coEvery { SessionTokensCache.getToken(cacheKey) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.success(mockTokenResource)
    coEvery { SessionTokensCache.storeIfFresher(cacheKey, mockTokenResource, any()) } returns
      SessionTokensCache.StoreResult(mockTokenResource, true)

    val result = sessionTokenFetcher.getToken(mockSession)

    assertEquals(mockTokenResource, result)
    coVerify { mockClerkApiService.tokens("session_123") }
  }
}
