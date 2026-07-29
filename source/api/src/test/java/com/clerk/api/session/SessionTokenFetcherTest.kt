package com.clerk.api.session

import com.auth0.android.jwt.JWT
import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SessionApi
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
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
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

    // Create SessionTokenFetcher with mocked JWTManager
    sessionTokenFetcher = SessionTokenFetcher(mockJWTManager)

    // Mock session properties
    every { mockSession.id } returns "session_123"
    every { mockSession.status } returns Session.SessionStatus.ACTIVE

    // Mock JWT manager to return our mock JWT
    every { mockJWTManager.createFromString(any()) } returns mockJWT

    // Mock ClerkApi
    mockkObject(ClerkApi)
    every { ClerkApi.session } returns mockClerkApiService

    // Mock Clerk state access
    mockkObject(Clerk)
    every { Clerk.session } returns mockSession
    every { Clerk.clearSessionAndUserState() } returns Unit

    // Mock SessionTokensCache
    SessionTokensCache.clear()
    mockkObject(SessionTokensCache)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `getToken returns cached token if valid and cache not skipped`() = runTest {
    // Given
    val cacheKey = "session_123-organization-"
    val futureTime = Date(System.currentTimeMillis() + 120000) // 2 minutes from now

    every { mockTokenResource.jwt } returns "valid.jwt.token"
    every { mockJWT.expiresAt } returns futureTime
    coEvery { SessionTokensCache.getToken(cacheKey) } returns mockTokenResource

    // When
    val result = sessionTokenFetcher.getToken(mockSession)

    // Then
    assertEquals(mockTokenResource, result)
    coVerify { SessionTokensCache.getToken(cacheKey) }
    coVerify(exactly = 0) { mockClerkApiService.tokens(any()) }
  }

  @Test
  fun `getToken fetches from network if cache is empty`() = runTest {
    // Given
    val cacheKey = "session_123-organization-"
    val setTokenSlot = slot<TokenResource>()

    coEvery { SessionTokensCache.getToken(cacheKey) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.success(mockTokenResource)
    coEvery { SessionTokensCache.storeIfFresher(cacheKey, capture(setTokenSlot), any()) } returns
      SessionTokensCache.StoreResult(mockTokenResource, true)

    // When
    val result = sessionTokenFetcher.getToken(mockSession)

    // Then
    assertEquals(mockTokenResource, result)
    coVerify { SessionTokensCache.getToken(cacheKey) }
    coVerify { mockClerkApiService.tokens("session_123") }
    coVerify { SessionTokensCache.storeIfFresher(cacheKey, mockTokenResource, any()) }
    assertEquals(mockTokenResource, setTokenSlot.captured)
  }

  @Test
  fun `getToken fetches from network if cached token is expired`() = runTest {
    // Given
    val cacheKey = "session_123-organization-"
    val pastTime = Date(System.currentTimeMillis() - 60000) // 1 minute ago
    val freshToken = mockk<TokenResource>(relaxed = true)

    every { mockTokenResource.jwt } returns "expired.jwt.token"
    every { mockJWT.expiresAt } returns pastTime
    coEvery { SessionTokensCache.getToken(cacheKey) } returns mockTokenResource
    coEvery { mockClerkApiService.tokens("session_123") } returns ClerkResult.success(freshToken)
    coEvery { SessionTokensCache.storeIfFresher(cacheKey, freshToken, any()) } returns
      SessionTokensCache.StoreResult(freshToken, true)

    // When
    val result = sessionTokenFetcher.getToken(mockSession)

    // Then
    assertEquals(freshToken, result)
    coVerify { SessionTokensCache.getToken(cacheKey) }
    coVerify { mockClerkApiService.tokens("session_123") }
    coVerify { SessionTokensCache.storeIfFresher(cacheKey, freshToken, any()) }
  }

  @Test
  fun `getToken uses template in API call when provided`() = runTest {
    // Given
    val template = "custom_template"
    val cacheKey = "session_123-template-custom_template"
    val options = GetTokenOptions(template = template)

    coEvery { SessionTokensCache.getToken(cacheKey) } returns null
    coEvery { mockClerkApiService.tokens("session_123", template) } returns
      ClerkResult.success(mockTokenResource)
    coEvery { SessionTokensCache.storeIfFresher(cacheKey, mockTokenResource, any()) } returns
      SessionTokensCache.StoreResult(mockTokenResource, true)

    // When
    val result = sessionTokenFetcher.getToken(mockSession, options)

    // Then
    assertEquals(mockTokenResource, result)
    coVerify { mockClerkApiService.tokens("session_123", template) }
    coVerify { SessionTokensCache.storeIfFresher(cacheKey, mockTokenResource, any()) }
  }

  @Test
  fun `getToken bypasses cached result when skipCache is true`() = runTest {
    // Given
    val options = GetTokenOptions(skipCache = true)
    val cacheKey = "session_123-organization-"

    coEvery { SessionTokensCache.getToken(cacheKey) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.success(mockTokenResource)
    coEvery { SessionTokensCache.storeIfFresher(cacheKey, mockTokenResource, any()) } returns
      SessionTokensCache.StoreResult(mockTokenResource, true)

    // When
    val result = sessionTokenFetcher.getToken(mockSession, options)

    // Then
    assertEquals(mockTokenResource, result)
    coVerify { SessionTokensCache.getToken(cacheKey) }
    coVerify { mockClerkApiService.tokens("session_123") }
    coVerify { SessionTokensCache.storeIfFresher(cacheKey, mockTokenResource, any()) }
  }

  @Test
  fun `session minter passes previous token and forces origin for forced refresh`() = runTest {
    // Given
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
    every {
      SessionTokensCache.storeIfFresher(cacheKey, mockTokenResource, any())
    } returns SessionTokensCache.StoreResult(mockTokenResource, true)

    // When
    val result = sessionTokenFetcher.getToken(mockSession, GetTokenOptions(skipCache = true))

    // Then
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
    // Given
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

    // When
    val result = sessionTokenFetcher.getToken(mockSession)

    // Then
    assertNull(result)
    coVerify { mockClerkApiService.tokens("session_123") }
    coVerify(exactly = 0) { SessionTokensCache.storeIfFresher(any(), any(), any()) }
    verify(exactly = 0) { Clerk.clearSessionAndUserState() }
  }

  @Test
  fun `getToken clears local session state when token endpoint returns unauthorized`() = runTest {
    // Given
    val error = Error(code = "session_revoked", message = "Session revoked")
    val errorResponse = ClerkErrorResponse(errors = listOf(error), clerkTraceId = "trace_unauth")

    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.httpFailure(code = 401, error = errorResponse)

    // When
    val result = sessionTokenFetcher.getToken(mockSession)

    // Then
    assertNull(result)
    verify(exactly = 1) { Clerk.clearSessionAndUserState() }
  }

  @Test
  fun `getToken clears local session state when token endpoint returns authentication invalid`() =
    runTest {
      // Given
      val error = Error(code = "authentication_invalid", message = "Invalid authentication")
      val errorResponse = ClerkErrorResponse(errors = listOf(error), clerkTraceId = "trace_unauth")

      coEvery { SessionTokensCache.getToken(any()) } returns null
      coEvery { mockClerkApiService.tokens("session_123") } returns
        ClerkResult.httpFailure(code = 401, error = errorResponse)

      // When
      val result = sessionTokenFetcher.getToken(mockSession)

      // Then
      assertNull(result)
      verify(exactly = 1) { Clerk.clearSessionAndUserState() }
    }

  @Test
  fun `getToken does not clear local session state for non-session unauthorized errors`() =
    runTest {
      // Given
      val error = Error(code = "not_authorized", message = "Unauthorized")
      val errorResponse = ClerkErrorResponse(errors = listOf(error), clerkTraceId = "trace_unauth")

      coEvery { SessionTokensCache.getToken(any()) } returns null
      coEvery { mockClerkApiService.tokens("session_123") } returns
        ClerkResult.httpFailure(code = 401, error = errorResponse)

      // When
      val result = sessionTokenFetcher.getToken(mockSession)

      // Then
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

    // When
    val result = sessionTokenFetcher.getToken(mockSession, options)

    // Then
    assertEquals(mockTokenResource, result)
    // Should fetch from network because token expires within buffer
    coVerify { mockClerkApiService.tokens("session_123") }
  }

  @Test
  fun `getToken handles JWT parsing exception gracefully`() = runTest {
    // Given
    val cacheKey = "session_123-organization-"

    every { mockTokenResource.jwt } returns "invalid.jwt.token"
    every { mockJWTManager.createFromString(any()) } throws RuntimeException("Invalid JWT")
    coEvery { SessionTokensCache.getToken(cacheKey) } returns mockTokenResource
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.success(mockTokenResource)
    coEvery { SessionTokensCache.storeIfFresher(cacheKey, mockTokenResource, any()) } returns
      SessionTokensCache.StoreResult(mockTokenResource, true)

    // When
    val result = sessionTokenFetcher.getToken(mockSession)

    // Then
    assertEquals(mockTokenResource, result)
    // Should fetch from network because JWT parsing failed
    coVerify { mockClerkApiService.tokens("session_123") }
  }

  @Test
  fun `getToken handles concurrent requests properly`() = runTest {
    // Given
    val cacheKey = "session_123-organization-"

    coEvery { SessionTokensCache.getToken(cacheKey) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } coAnswers
      {
        delay(100) // Simulate network delay
        ClerkResult.success(mockTokenResource)
      }
    coEvery { SessionTokensCache.storeIfFresher(cacheKey, mockTokenResource, any()) } returns
      SessionTokensCache.StoreResult(mockTokenResource, true)

    // When - Launch multiple concurrent requests
    val deferred1 = async { sessionTokenFetcher.getToken(mockSession) }
    val deferred2 = async { sessionTokenFetcher.getToken(mockSession) }
    val deferred3 = async { sessionTokenFetcher.getToken(mockSession) }

    val result1 = deferred1.await()
    val result2 = deferred2.await()
    val result3 = deferred3.await()

    // Then - All should return the same token instance
    assertSame(mockTokenResource, result1)
    assertSame(mockTokenResource, result2)
    assertSame(mockTokenResource, result3)

    // API should only be called once despite concurrent requests
    coVerify(exactly = 1) { mockClerkApiService.tokens("session_123") }
  }

  @Test
  fun `forced refreshes are not deduplicated`() = runTest {
    // Given
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
        SessionTokensCache.StoreResult(token, true)
      }

    // When
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

    // Then
    assertEquals(mockTokenResource, firstResult)
    assertEquals(secondToken, secondResult)
    coVerify(exactly = 2) { mockClerkApiService.tokens("session_123") }
  }

  @Test
  fun `getToken handles API exception gracefully`() = runTest {
    // Given
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } throws RuntimeException("Network error")

    // When
    val result = sessionTokenFetcher.getToken(mockSession)

    // Then
    assertNull(result)
    coVerify { mockClerkApiService.tokens("session_123") }
    coVerify(exactly = 0) { SessionTokensCache.storeIfFresher(any(), any(), any()) }
  }

  @Test
  fun `tokenCacheKey generates correct key without template`() {
    // Given
    every { mockSession.id } returns "session_456"

    // When
    val cacheKey = mockSession.tokenCacheKey(null)

    // Then
    assertEquals("session_456-organization-", cacheKey)
  }

  @Test
  fun `tokenCacheKey generates correct key with template`() {
    // Given
    every { mockSession.id } returns "session_456"
    val template = "admin_template"

    // When
    val cacheKey = mockSession.tokenCacheKey(template)

    // Then
    assertEquals("session_456-template-admin_template", cacheKey)
  }

  @Test
  fun `different sessions get different cache keys`() = runTest {
    // Given
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

    // When
    sessionTokenFetcher.getToken(session1)
    sessionTokenFetcher.getToken(session2)

    // Then
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
    // Given - a session with PENDING status
    every { mockSession.status } returns Session.SessionStatus.PENDING

    // When
    val result = sessionTokenFetcher.getToken(mockSession)

    // Then - should return null without making API call
    assertNull(result)
    coVerify(exactly = 0) { SessionTokensCache.getToken(any()) }
    coVerify(exactly = 0) { mockClerkApiService.tokens(any()) }
  }

  @Test
  fun `getToken proceeds normally for active session`() = runTest {
    // Given - a session with ACTIVE status
    val cacheKey = "session_123-organization-"

    every { mockSession.status } returns Session.SessionStatus.ACTIVE
    coEvery { SessionTokensCache.getToken(cacheKey) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.success(mockTokenResource)
    coEvery { SessionTokensCache.storeIfFresher(cacheKey, mockTokenResource, any()) } returns
      SessionTokensCache.StoreResult(mockTokenResource, true)

    // When
    val result = sessionTokenFetcher.getToken(mockSession)

    // Then - should fetch token normally
    assertEquals(mockTokenResource, result)
    coVerify { mockClerkApiService.tokens("session_123") }
  }
}
