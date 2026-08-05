package com.clerk.api.session

import android.util.Base64
import com.auth0.android.jwt.JWT
import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.model.client.Client
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
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
  private lateinit var backoff: SessionTokenBackoff
  private lateinit var fetchScope: CoroutineScope
  private lateinit var readableJwt: String
  private val testScheduler = TestCoroutineScheduler()
  private val testDispatcher = StandardTestDispatcher(testScheduler)
  private var clockMillis = 0L

  @Before
  fun setup() {
    mockSession = mockk(relaxed = true)
    mockTokenResource = mockk(relaxed = true)
    mockJWT = mockk(relaxed = true)
    mockJWTManager = mockk(relaxed = true)
    mockClerkApiService = mockk(relaxed = true)

    // Create SessionTokenFetcher with a mocked JWTManager, plus a backoff and a fetch scope
    // isolated to this test. The scope shares the test scheduler, so the shared request runs on
    // deterministic virtual time and its delays never touch the wall clock.
    backoff = SessionTokenBackoff { clockMillis }
    fetchScope = CoroutineScope(SupervisorJob() + testDispatcher)
    readableJwt = buildJwt(oiat = 1_000L)
    sessionTokenFetcher = SessionTokenFetcher(mockJWTManager, backoff, fetchScope)

    // Mock session properties
    every { mockSession.id } returns "session_123"
    every { mockSession.status } returns Session.SessionStatus.ACTIVE
    every { mockSession.lastActiveToken } returns null

    // Mock JWT manager to return our mock JWT
    every { mockJWTManager.createFromString(any()) } returns mockJWT

    // Mock ClerkApi
    mockkObject(ClerkApi)
    every { ClerkApi.session } returns mockClerkApiService

    // Mock Clerk state access
    mockkObject(Clerk)
    every { Clerk.session } returns mockSession
    every { Clerk.clearSessionAndUserState() } returns Unit
    every { Clerk.sessionMinterIsEnabled } returns false
    // No current client by default, so the seed fallback is absent unless a test opts in.
    every { Clerk.clientInitialized } returns false

    // Mock SessionTokensCache
    mockkObject(SessionTokensCache)
  }

  @After
  fun tearDown() {
    clockMillis = 0L
    fetchScope.cancel()
    unmockkAll()
  }

  @Test
  fun `getToken returns cached token if valid and cache not skipped`() = runTest(testDispatcher) {
    // Given
    val cacheKey = "session_123"
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
  fun `getToken fetches from network if cache is empty`() = runTest(testDispatcher) {
    // Given
    val cacheKey = "session_123"
    val setTokenSlot = slot<TokenResource>()

    coEvery { SessionTokensCache.getToken(cacheKey) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.success(mockTokenResource)
    coEvery {
      SessionTokensCache.setTokenIfCurrent(cacheKey, "session_123", any(), capture(setTokenSlot))
    } returns Unit

    // When
    val result = sessionTokenFetcher.getToken(mockSession)

    // Then
    assertEquals(mockTokenResource, result)
    coVerify { SessionTokensCache.getToken(cacheKey) }
    coVerify { mockClerkApiService.tokens("session_123") }
    coVerify {
      SessionTokensCache.setTokenIfCurrent(cacheKey, "session_123", any(), mockTokenResource)
    }
    assertEquals(mockTokenResource, setTokenSlot.captured)
  }

  @Test
  fun `getToken fetches from network if cached token is expired`() = runTest(testDispatcher) {
    // Given
    val cacheKey = "session_123"
    val pastTime = Date(System.currentTimeMillis() - 60000) // 1 minute ago
    val freshToken = mockk<TokenResource>(relaxed = true)

    every { mockTokenResource.jwt } returns "expired.jwt.token"
    every { mockJWT.expiresAt } returns pastTime
    coEvery { SessionTokensCache.getToken(cacheKey) } returns mockTokenResource
    coEvery { mockClerkApiService.tokens("session_123") } returns ClerkResult.success(freshToken)
    coEvery {
      SessionTokensCache.setTokenIfCurrent(cacheKey, "session_123", any(), freshToken)
    } returns Unit

    // When
    val result = sessionTokenFetcher.getToken(mockSession)

    // Then
    assertEquals(freshToken, result)
    coVerify { SessionTokensCache.getToken(cacheKey) }
    coVerify { mockClerkApiService.tokens("session_123") }
    coVerify { SessionTokensCache.setTokenIfCurrent(cacheKey, "session_123", any(), freshToken) }
  }

  @Test
  fun `getToken uses template in API call when provided`() = runTest(testDispatcher) {
    // Given
    val template = "custom_template"
    val cacheKey = "session_123-custom_template"
    val options = GetTokenOptions(template = template)

    coEvery { SessionTokensCache.getToken(cacheKey) } returns null
    coEvery { mockClerkApiService.tokens(userId = "session_123", templateType = template) } returns
      ClerkResult.success(mockTokenResource)
    coEvery {
      SessionTokensCache.setTokenIfCurrent(cacheKey, "session_123", any(), mockTokenResource)
    } returns Unit

    // When
    val result = sessionTokenFetcher.getToken(mockSession, options)

    // Then
    assertEquals(mockTokenResource, result)
    coVerify { mockClerkApiService.tokens(userId = "session_123", templateType = template) }
    coVerify {
      SessionTokensCache.setTokenIfCurrent(cacheKey, "session_123", any(), mockTokenResource)
    }
  }

  @Test
  fun `getToken skips cache when skipCache is true`() = runTest(testDispatcher) {
    // Given
    val options = GetTokenOptions(skipCache = true)
    val cacheKey = "session_123"

    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.success(mockTokenResource)
    coEvery {
      SessionTokensCache.setTokenIfCurrent(cacheKey, "session_123", any(), mockTokenResource)
    } returns Unit

    // When
    val result = sessionTokenFetcher.getToken(mockSession, options)

    // Then
    assertEquals(mockTokenResource, result)
    coVerify(exactly = 0) { SessionTokensCache.getToken(any()) }
    coVerify { mockClerkApiService.tokens("session_123") }
    coVerify {
      SessionTokensCache.setTokenIfCurrent(cacheKey, "session_123", any(), mockTokenResource)
    }
  }

  @Test
  fun `getToken returns null when API call fails`() = runTest(testDispatcher) {
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
    coVerify(exactly = 0) { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) }
    verify(exactly = 0) { Clerk.clearSessionAndUserState() }
  }

  @Test
  fun `getToken clears local session state when token endpoint returns unauthorized`() =
    runTest(testDispatcher) {
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
    runTest(testDispatcher) {
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
    runTest(testDispatcher) {
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
  fun `getToken uses custom expiration buffer`() = runTest(testDispatcher) {
    // Given
    val customBuffer = 120L // 2 minutes
    val options = GetTokenOptions(expirationBuffer = customBuffer)
    val cacheKey = "session_123"
    // Token expires in 90 seconds (less than 2-minute buffer)
    val soonExpiredTime = Date(System.currentTimeMillis() + 90000)

    every { mockTokenResource.jwt } returns "soon.expired.token"
    every { mockJWT.expiresAt } returns soonExpiredTime
    coEvery { SessionTokensCache.getToken(cacheKey) } returns mockTokenResource
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.success(mockTokenResource)
    coEvery {
      SessionTokensCache.setTokenIfCurrent(cacheKey, "session_123", any(), mockTokenResource)
    } returns Unit

    // When
    val result = sessionTokenFetcher.getToken(mockSession, options)

    // Then
    assertEquals(mockTokenResource, result)
    // Should fetch from network because token expires within buffer
    coVerify { mockClerkApiService.tokens("session_123") }
  }

  @Test
  fun `getToken handles JWT parsing exception gracefully`() = runTest(testDispatcher) {
    // Given
    val cacheKey = "session_123"

    every { mockTokenResource.jwt } returns "invalid.jwt.token"
    every { mockJWTManager.createFromString(any()) } throws RuntimeException("Invalid JWT")
    coEvery { SessionTokensCache.getToken(cacheKey) } returns mockTokenResource
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.success(mockTokenResource)
    coEvery {
      SessionTokensCache.setTokenIfCurrent(cacheKey, "session_123", any(), mockTokenResource)
    } returns Unit

    // When
    val result = sessionTokenFetcher.getToken(mockSession)

    // Then
    assertEquals(mockTokenResource, result)
    // Should fetch from network because JWT parsing failed
    coVerify { mockClerkApiService.tokens("session_123") }
  }

  @Test
  fun `getToken handles concurrent requests properly`() = runTest(testDispatcher) {
    // Given
    val cacheKey = "session_123"

    coEvery { SessionTokensCache.getToken(cacheKey) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } coAnswers
      {
        delay(100) // Simulate network delay
        ClerkResult.success(mockTokenResource)
      }
    coEvery {
      SessionTokensCache.setTokenIfCurrent(cacheKey, "session_123", any(), mockTokenResource)
    } returns Unit

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
  fun `getToken handles API exception gracefully`() = runTest(testDispatcher) {
    // Given
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } throws RuntimeException("Network error")

    // When
    val result = sessionTokenFetcher.getToken(mockSession)

    // Then
    assertNull(result)
    coVerify { mockClerkApiService.tokens("session_123") }
    coVerify(exactly = 0) { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) }
  }

  @Test
  fun `tokenCacheKey generates correct key without template`() {
    // Given
    every { mockSession.id } returns "session_456"

    // When
    val cacheKey = mockSession.tokenCacheKey(null)

    // Then
    assertEquals("session_456", cacheKey)
  }

  @Test
  fun `tokenCacheKey generates correct key with template`() {
    // Given
    every { mockSession.id } returns "session_456"
    val template = "admin_template"

    // When
    val cacheKey = mockSession.tokenCacheKey(template)

    // Then
    assertEquals("session_456-admin_template", cacheKey)
  }

  @Test
  fun `different sessions get different cache keys`() = runTest(testDispatcher) {
    // Given
    val session1 = mockk<Session>(relaxed = true)
    val session2 = mockk<Session>(relaxed = true)
    every { session1.id } returns "session_1"
    every { session2.id } returns "session_2"

    coEvery { SessionTokensCache.getToken("session_1") } returns null
    coEvery { SessionTokensCache.getToken("session_2") } returns null
    coEvery { mockClerkApiService.tokens("session_1") } returns
      ClerkResult.success(mockTokenResource)
    coEvery { mockClerkApiService.tokens("session_2") } returns
      ClerkResult.success(mockTokenResource)
    coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit

    // When
    sessionTokenFetcher.getToken(session1)
    sessionTokenFetcher.getToken(session2)

    // Then
    coVerify { mockClerkApiService.tokens("session_1") }
    coVerify { mockClerkApiService.tokens("session_2") }
    coVerify {
      SessionTokensCache.setTokenIfCurrent("session_1", "session_1", any(), mockTokenResource)
    }
    coVerify {
      SessionTokensCache.setTokenIfCurrent("session_2", "session_2", any(), mockTokenResource)
    }
  }

  @Test
  fun `getToken returns null for pending session`() = runTest(testDispatcher) {
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
  fun `getToken proceeds normally for active session`() = runTest(testDispatcher) {
    // Given - a session with ACTIVE status
    val cacheKey = "session_123"

    every { mockSession.status } returns Session.SessionStatus.ACTIVE
    coEvery { SessionTokensCache.getToken(cacheKey) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.success(mockTokenResource)
    coEvery {
      SessionTokensCache.setTokenIfCurrent(cacheKey, "session_123", any(), mockTokenResource)
    } returns Unit

    // When
    val result = sessionTokenFetcher.getToken(mockSession)

    // Then - should fetch token normally
    assertEquals(mockTokenResource, result)
    coVerify { mockClerkApiService.tokens("session_123") }
  }

  // region session minter wire contract

  @Test
  fun `getToken posts the plain bodyless request when the minter is disabled`() =
    runTest(testDispatcher) {
      every { Clerk.sessionMinterIsEnabled } returns false
      val cachedToken = mockk<TokenResource>(relaxed = true)
      every { cachedToken.jwt } returns readableJwt
      coEvery { SessionTokensCache.getToken("session_123") } returns cachedToken
      coEvery { mockClerkApiService.tokens("session_123") } returns
        ClerkResult.success(mockTokenResource)
      coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit

      sessionTokenFetcher.getToken(mockSession, GetTokenOptions(skipCache = true))

      coVerify(exactly = 1) { mockClerkApiService.tokens("session_123") }
      coVerify(exactly = 0) { mockClerkApiService.mintTokens(any(), any(), any()) }
    }

  @Test
  fun `getToken seeds the request with the cached token when the minter is enabled`() =
    runTest(testDispatcher) {
      every { Clerk.sessionMinterIsEnabled } returns true
      val cachedToken = mockk<TokenResource>(relaxed = true)
      every { cachedToken.jwt } returns readableJwt
      coEvery { SessionTokensCache.getToken("session_123") } returns cachedToken
      every { mockJWT.expiresAt } returns Date(System.currentTimeMillis() - 60_000)
      coEvery { mockClerkApiService.mintTokens(any(), any(), any()) } returns
        ClerkResult.success(mockTokenResource)
      coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit

      sessionTokenFetcher.getToken(mockSession)

      coVerify(exactly = 1) {
        mockClerkApiService.mintTokens(
          sessionId = "session_123",
          previousSessionToken = readableJwt,
          forceOrigin = null,
        )
      }
    }

  @Test
  fun `getToken falls back to lastActiveToken when nothing is cached`() = runTest(testDispatcher) {
    every { Clerk.sessionMinterIsEnabled } returns true
    stubCurrentSessionToken(readableJwt)
    coEvery { SessionTokensCache.getToken("session_123") } returns null
    coEvery { mockClerkApiService.mintTokens(any(), any(), any()) } returns
      ClerkResult.success(mockTokenResource)
    coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit

    sessionTokenFetcher.getToken(mockSession)

    coVerify(exactly = 1) {
      mockClerkApiService.mintTokens(
        sessionId = "session_123",
        previousSessionToken = readableJwt,
        forceOrigin = null,
      )
    }
  }

  @Test
  fun `getToken skips a blank cached seed and falls through to lastActiveToken`() =
    runTest(testDispatcher) {
      every { Clerk.sessionMinterIsEnabled } returns true
      val blankToken = mockk<TokenResource>(relaxed = true)
      every { blankToken.jwt } returns ""
      coEvery { SessionTokensCache.getToken("session_123") } returns blankToken
      stubCurrentSessionToken(readableJwt)
      coEvery { mockClerkApiService.mintTokens(any(), any(), any()) } returns
        ClerkResult.success(mockTokenResource)
      coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit

      sessionTokenFetcher.getToken(mockSession)

      coVerify(exactly = 1) {
        mockClerkApiService.mintTokens(
          sessionId = "session_123",
          previousSessionToken = readableJwt,
          forceOrigin = null,
        )
      }
    }

  @Test
  fun `getToken skips an undecodable cached seed and falls through to lastActiveToken`() =
    runTest(testDispatcher) {
      every { Clerk.sessionMinterIsEnabled } returns true
      val malformedToken = mockk<TokenResource>(relaxed = true)
      every { malformedToken.jwt } returns "not-a-jwt"
      coEvery { SessionTokensCache.getToken("session_123") } returns malformedToken
      stubCurrentSessionToken(readableJwt)
      coEvery { mockClerkApiService.mintTokens(any(), any(), any()) } returns
        ClerkResult.success(mockTokenResource)
      coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit

      sessionTokenFetcher.getToken(mockSession)

      coVerify(exactly = 1) {
        mockClerkApiService.mintTokens(
          sessionId = "session_123",
          previousSessionToken = readableJwt,
          forceOrigin = null,
        )
      }
    }

  @Test
  fun `getToken posts the plain bodyless request when there is nothing to attach`() =
    runTest(testDispatcher) {
      every { Clerk.sessionMinterIsEnabled } returns true
      every { mockSession.lastActiveToken } returns null
      coEvery { SessionTokensCache.getToken("session_123") } returns null
      coEvery { mockClerkApiService.tokens("session_123") } returns
        ClerkResult.success(mockTokenResource)
      coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit

      sessionTokenFetcher.getToken(mockSession)

      coVerify(exactly = 1) { mockClerkApiService.tokens("session_123") }
      coVerify(exactly = 0) { mockClerkApiService.mintTokens(any(), any(), any()) }
    }

  @Test
  fun `getToken never sends an undecodable seed`() = runTest(testDispatcher) {
    every { Clerk.sessionMinterIsEnabled } returns true
    val malformedToken = mockk<TokenResource>(relaxed = true)
    every { malformedToken.jwt } returns "not-a-jwt"
    coEvery { SessionTokensCache.getToken("session_123") } returns malformedToken
    every { mockSession.lastActiveToken } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.success(mockTokenResource)
    coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit

    sessionTokenFetcher.getToken(mockSession)

    coVerify(exactly = 1) { mockClerkApiService.tokens("session_123") }
    coVerify(exactly = 0) { mockClerkApiService.mintTokens(any(), any(), any()) }
  }

  @Test
  fun `getToken maps skipCache to force origin when the minter is enabled`() =
    runTest(testDispatcher) {
      every { Clerk.sessionMinterIsEnabled } returns true
      every { mockSession.lastActiveToken } returns null
      coEvery { SessionTokensCache.getToken("session_123") } returns null
      coEvery { mockClerkApiService.mintTokens(any(), any(), any()) } returns
        ClerkResult.success(mockTokenResource)
      coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit

      sessionTokenFetcher.getToken(mockSession, GetTokenOptions(skipCache = true))

      coVerify(exactly = 1) {
        mockClerkApiService.mintTokens(
          sessionId = "session_123",
          previousSessionToken = null,
          forceOrigin = true,
        )
      }
    }

  @Test
  fun `a cached template token never seeds the default request`() = runTest(testDispatcher) {
    every { Clerk.sessionMinterIsEnabled } returns true
    every { mockSession.lastActiveToken } returns null
    val templateToken = mockk<TokenResource>(relaxed = true)
    every { templateToken.jwt } returns readableJwt
    coEvery { SessionTokensCache.getToken("session_123-custom_template") } returns templateToken
    coEvery { SessionTokensCache.getToken("session_123") } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.success(mockTokenResource)
    coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit

    sessionTokenFetcher.getToken(mockSession)

    coVerify(exactly = 1) { mockClerkApiService.tokens("session_123") }
    coVerify(exactly = 0) { mockClerkApiService.mintTokens(any(), any(), any()) }
  }

  @Test
  fun `the templated route never carries minter fields`() = runTest(testDispatcher) {
    every { Clerk.sessionMinterIsEnabled } returns true
    val cachedToken = mockk<TokenResource>(relaxed = true)
    every { cachedToken.jwt } returns readableJwt
    coEvery { SessionTokensCache.getToken(any()) } returns cachedToken
    every { mockJWT.expiresAt } returns Date(System.currentTimeMillis() - 60_000)
    coEvery {
      mockClerkApiService.tokens(userId = any(), templateType = any())
    } returns ClerkResult.success(mockTokenResource)
    coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit

    sessionTokenFetcher.getToken(
      mockSession,
      GetTokenOptions(template = "custom_template", skipCache = true),
    )

    coVerify(exactly = 1) {
      mockClerkApiService.tokens(userId = "session_123", templateType = "custom_template")
    }
    coVerify(exactly = 0) { mockClerkApiService.mintTokens(any(), any(), any()) }
  }

  @Test
  fun `the response is fenced with the generation captured before the request`() =
    runTest(testDispatcher) {
      val generationBeforeRequest = SessionTokensCache.Generation(global = 0L, session = 0L)
      coEvery { SessionTokensCache.getToken(any()) } returns null
      every { SessionTokensCache.generation("session_123") } returns generationBeforeRequest
      coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit
      coEvery { mockClerkApiService.tokens("session_123") } coAnswers
        {
          // An org switch lands while the request is in flight; the fence must already be captured.
          every { SessionTokensCache.generation("session_123") } returns
            SessionTokensCache.Generation(global = 0L, session = 1L)
          ClerkResult.success(mockTokenResource)
        }

      assertEquals(mockTokenResource, sessionTokenFetcher.getToken(mockSession))

      coVerify(exactly = 1) {
        SessionTokensCache.setTokenIfCurrent(
          "session_123",
          "session_123",
          generationBeforeRequest,
          mockTokenResource,
        )
      }
    }

  @Test
  fun `a caller after an invalidation does not join the pre-switch request`() =
    runTest(testDispatcher) {
      val preSwitch = SessionTokensCache.Generation(global = 0L, session = 0L)
      val postSwitch = SessionTokensCache.Generation(global = 0L, session = 1L)
      val preSwitchToken = mockk<TokenResource>(relaxed = true)
      val postSwitchToken = mockk<TokenResource>(relaxed = true)
      coEvery { SessionTokensCache.getToken("session_123") } returns null
      coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit
      // Reads in order: first's caller-capture, first's in-compute join read, then second's
      // caller-capture (still pre-switch), then second's in-compute read (post-switch) and every
      // read after. The switch lands between second's caller-capture and its map entry, so a join
      // decision keyed on the caller-capture would wrongly join, one keyed on the in-compute read
      // must not.
      every { SessionTokensCache.generation("session_123") } returnsMany
        listOf(preSwitch, preSwitch, preSwitch, postSwitch)
      var callCount = 0
      coEvery { mockClerkApiService.tokens("session_123") } coAnswers
        {
          callCount += 1
          if (callCount == 1) {
            delay(100)
            ClerkResult.success(preSwitchToken)
          } else {
            ClerkResult.success(postSwitchToken)
          }
        }

      val first = async { sessionTokenFetcher.getToken(mockSession) }
      runCurrent()
      val second = async { sessionTokenFetcher.getToken(mockSession) }

      assertSame(postSwitchToken, second.await())
      assertSame(preSwitchToken, first.await())
      coVerify(exactly = 2) { mockClerkApiService.tokens("session_123") }
    }

  @Test
  fun `the seed ignores a retained stale session and reads current client state`() =
    runTest(testDispatcher) {
      every { Clerk.sessionMinterIsEnabled } returns true
      val staleSession = mockk<Session>(relaxed = true)
      every { staleSession.id } returns "session_123"
      every { staleSession.status } returns Session.SessionStatus.ACTIVE
      val staleToken = mockk<TokenResource>(relaxed = true)
      every { staleToken.jwt } returns buildJwt(oiat = 9_000L)
      every { staleSession.lastActiveToken } returns staleToken
      coEvery { SessionTokensCache.getToken("session_123") } returns null
      // The current client's session was sanitized by the switch, so it has no token to seed with.
      val currentSession = mockk<Session>(relaxed = true)
      every { currentSession.id } returns "session_123"
      every { currentSession.lastActiveToken } returns null
      val client = mockk<Client>(relaxed = true)
      every { client.sessions } returns listOf(currentSession)
      every { Clerk.clientInitialized } returns true
      every { Clerk.client } returns client
      coEvery { mockClerkApiService.tokens("session_123") } returns
        ClerkResult.success(mockTokenResource)
      coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit

      sessionTokenFetcher.getToken(staleSession)

      // Seed absent because current state has none; the stale instance's token is never sent.
      coVerify(exactly = 1) { mockClerkApiService.tokens("session_123") }
      coVerify(exactly = 0) { mockClerkApiService.mintTokens(any(), any(), any()) }
    }

  // endregion

  // region failure backoff

  @Test
  fun `a failed request is not retried inside the backoff window`() = runTest(testDispatcher) {
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.apiFailure(ClerkErrorResponse(errors = emptyList(), clerkTraceId = "trace"))

    assertNull(sessionTokenFetcher.getToken(mockSession))
    clockMillis = 4_999L
    assertNull(sessionTokenFetcher.getToken(mockSession))

    coVerify(exactly = 1) { mockClerkApiService.tokens("session_123") }
  }

  @Test
  fun `a request is retried once the backoff window closes`() = runTest(testDispatcher) {
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.apiFailure(ClerkErrorResponse(errors = emptyList(), clerkTraceId = "trace"))

    assertNull(sessionTokenFetcher.getToken(mockSession))
    clockMillis = 5_000L
    assertNull(sessionTokenFetcher.getToken(mockSession))

    coVerify(exactly = 2) { mockClerkApiService.tokens("session_123") }
  }

  @Test
  fun `a thrown request also opens the backoff window`() = runTest(testDispatcher) {
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } throws RuntimeException("Network error")

    assertNull(sessionTokenFetcher.getToken(mockSession))
    assertNull(sessionTokenFetcher.getToken(mockSession))

    coVerify(exactly = 1) { mockClerkApiService.tokens("session_123") }
  }

  @Test
  fun `a successful request clears the backoff window`() = runTest(testDispatcher) {
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.apiFailure(ClerkErrorResponse(errors = emptyList(), clerkTraceId = "trace"))

    assertNull(sessionTokenFetcher.getToken(mockSession))

    clockMillis = 5_000L
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.success(mockTokenResource)
    assertEquals(mockTokenResource, sessionTokenFetcher.getToken(mockSession))

    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.apiFailure(ClerkErrorResponse(errors = emptyList(), clerkTraceId = "trace"))
    assertNull(sessionTokenFetcher.getToken(mockSession))

    coVerify(exactly = 3) { mockClerkApiService.tokens("session_123") }
  }

  @Test
  fun `the backoff window is scoped to the cache key`() = runTest(testDispatcher) {
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.apiFailure(ClerkErrorResponse(errors = emptyList(), clerkTraceId = "trace"))
    coEvery {
      mockClerkApiService.tokens(userId = "session_123", templateType = "custom_template")
    } returns ClerkResult.success(mockTokenResource)

    assertNull(sessionTokenFetcher.getToken(mockSession))
    val templated =
      sessionTokenFetcher.getToken(mockSession, GetTokenOptions(template = "custom_template"))

    assertEquals(mockTokenResource, templated)
  }

  @Test
  fun `a forced request is never refused by the backoff window`() = runTest(testDispatcher) {
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.apiFailure(ClerkErrorResponse(errors = emptyList(), clerkTraceId = "trace"))

    assertNull(sessionTokenFetcher.getToken(mockSession))
    assertNull(sessionTokenFetcher.getToken(mockSession, GetTokenOptions(skipCache = true)))

    coVerify(exactly = 2) { mockClerkApiService.tokens("session_123") }
  }

  @Test
  fun `a forced request still widens the window that plain requests observe`() =
    runTest(testDispatcher) {
      coEvery { SessionTokensCache.getToken(any()) } returns null
      coEvery { mockClerkApiService.tokens("session_123") } returns
        ClerkResult.apiFailure(ClerkErrorResponse(errors = emptyList(), clerkTraceId = "trace"))

      assertNull(sessionTokenFetcher.getToken(mockSession, GetTokenOptions(skipCache = true)))
      assertNull(sessionTokenFetcher.getToken(mockSession))

      coVerify(exactly = 1) { mockClerkApiService.tokens("session_123") }
    }

  @Test
  fun `a failure that lands after an invalidation does not open a window`() =
    runTest(testDispatcher) {
      val atStart = SessionTokensCache.Generation(global = 0L, session = 0L)
      val afterInvalidation = SessionTokensCache.Generation(global = 0L, session = 1L)
      coEvery { SessionTokensCache.getToken(any()) } returns null
      every { SessionTokensCache.generation("session_123") } returns atStart
      coEvery { mockClerkApiService.tokens("session_123") } coAnswers
        {
          every { SessionTokensCache.generation("session_123") } returns afterInvalidation
          ClerkResult.apiFailure(ClerkErrorResponse(errors = emptyList(), clerkTraceId = "trace"))
        }

      assertNull(sessionTokenFetcher.getToken(mockSession))

      // The failure belonged to the pre-invalidation state, so the new state is not penalized.
      assertFalse(backoff.isBackingOff("session_123"))
    }

  @Test
  fun `a success that lands after an invalidation does not clear the new window`() =
    runTest(testDispatcher) {
      val atStart = SessionTokensCache.Generation(global = 0L, session = 0L)
      val afterInvalidation = SessionTokensCache.Generation(global = 0L, session = 1L)
      coEvery { SessionTokensCache.getToken(any()) } returns null
      coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit
      every { SessionTokensCache.generation("session_123") } returns atStart
      coEvery { mockClerkApiService.tokens("session_123") } coAnswers
        {
          // The new state opens its own failure window before the stale success returns.
          every { SessionTokensCache.generation("session_123") } returns afterInvalidation
          backoff.recordFailure("session_123")
          ClerkResult.success(mockTokenResource)
        }

      sessionTokenFetcher.getToken(mockSession)

      assertTrue(backoff.isBackingOff("session_123"))
    }

  // endregion

  @Test
  fun `concurrent requests through separate fetchers share one network call`() =
    runTest(testDispatcher) {
      val otherFetcher = SessionTokenFetcher(mockJWTManager, backoff, fetchScope)
      coEvery { SessionTokensCache.getToken("session_123") } returns null
      coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit
      coEvery { mockClerkApiService.tokens("session_123") } coAnswers
        {
          delay(100)
          ClerkResult.success(mockTokenResource)
        }

      val first = async { sessionTokenFetcher.getToken(mockSession) }
      val second = async { otherFetcher.getToken(mockSession) }

      assertSame(mockTokenResource, first.await())
      assertSame(mockTokenResource, second.await())
      coVerify(exactly = 1) { mockClerkApiService.tokens("session_123") }
    }

  @Test
  fun `a forced request does not join a plain request already in flight`() =
    runTest(testDispatcher) {
      coEvery { SessionTokensCache.getToken("session_123") } returns null
      coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit
      coEvery { mockClerkApiService.tokens("session_123") } coAnswers
        {
          delay(100)
          ClerkResult.success(mockTokenResource)
        }

      val plain = async { sessionTokenFetcher.getToken(mockSession) }
      runCurrent()
      val forced =
        async { sessionTokenFetcher.getToken(mockSession, GetTokenOptions(skipCache = true)) }

      assertSame(mockTokenResource, plain.await())
      assertSame(mockTokenResource, forced.await())
      coVerify(exactly = 2) { mockClerkApiService.tokens("session_123") }
    }

  @Test
  fun `a cancelled caller does not cancel the request the others are waiting on`() =
    runTest(testDispatcher) {
      coEvery { SessionTokensCache.getToken("session_123") } returns null
      coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit
      coEvery { mockClerkApiService.tokens("session_123") } coAnswers
        {
          delay(100)
          ClerkResult.success(mockTokenResource)
        }

      val abandoned = async { sessionTokenFetcher.getToken(mockSession) }
      runCurrent()
      val waiting = async { sessionTokenFetcher.getToken(mockSession) }
      runCurrent()
      abandoned.cancel()

      assertSame(mockTokenResource, waiting.await())
      coVerify(exactly = 1) { mockClerkApiService.tokens("session_123") }
    }

  @Test
  fun `resetForNewConfiguration cancels in-flight work and installs a fresh scope`() {
    val oldScope = SessionTokenFetcher.currentFetchScope
    val inFlight = oldScope.launch { awaitCancellation() }
    assertTrue(inFlight.isActive)

    SessionTokenFetcher.resetForNewConfiguration()

    // Everything begun under the old configuration is cancelled, so it can never POST or write
    // against the configuration installed next.
    assertTrue(inFlight.isCancelled)
    val newScope = SessionTokenFetcher.currentFetchScope
    assertNotSame(oldScope, newScope)
    assertTrue(newScope.isActive)
  }

  @Test
  fun `reset never exposes the cancelled scope and a later fetch runs on the fresh one`() {
    val oldScope = SessionTokenFetcher.currentFetchScope

    SessionTokenFetcher.resetForNewConfiguration()

    // The field only ever holds an active scope: the fresh one is published before the old one is
    // cancelled, so a caller reading it during reset never lands on a cancelled scope and its work
    // is not cut with a spurious CancellationException.
    val exposed = SessionTokenFetcher.currentFetchScope
    assertNotSame(oldScope, exposed)
    assertTrue(exposed.isActive)

    val afterReset = exposed.launch {}
    assertFalse(afterReset.isCancelled)
  }

  @Test
  fun `a token minted under a replaced configuration is discarded`() =
    runTest(testDispatcher) {
      coEvery { SessionTokensCache.getToken(any()) } returns null
      coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit
      coEvery { mockClerkApiService.tokens("session_123") } coAnswers
        {
          // The whole configuration is torn down while this request is in flight.
          SessionTokenFetcher.resetForNewConfiguration()
          ClerkResult.success(mockTokenResource)
        }

      val result = sessionTokenFetcher.getToken(mockSession)

      // The POST went out, but its result belongs to the replaced configuration, so it is neither
      // cached nor returned.
      assertNull(result)
      coVerify(exactly = 0) { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) }
    }

  @Test
  fun `a fresh caller does not join a cancelling task left in the map`() =
    runTest(testDispatcher) {
      val cancellingScope = CoroutineScope(SupervisorJob() + testDispatcher)
      val freshScope = CoroutineScope(SupervisorJob() + testDispatcher)
      val cancellingFetcher = SessionTokenFetcher(mockJWTManager, backoff, cancellingScope)
      val freshFetcher = SessionTokenFetcher(mockJWTManager, backoff, freshScope)
      coEvery { SessionTokensCache.getToken("session_123") } returns null
      coEvery { SessionTokensCache.setTokenIfCurrent(any(), any(), any(), any()) } returns Unit
      coEvery { mockClerkApiService.tokens("session_123") } coAnswers
        {
          delay(100)
          ClerkResult.success(mockTokenResource)
        }

      val abandoned = async { cancellingFetcher.getToken(mockSession) }
      runCurrent()
      // The task's deferred is now cancelling (isActive false) but still discoverable in the map,
      // the window a reset opens between clearing the map and the scope finishing cancelling.
      cancellingScope.cancel()

      val result = freshFetcher.getToken(mockSession)

      // The fresh caller must start its own request rather than join the cancelling task and get
      // its CancellationException.
      assertSame(mockTokenResource, result)
      coVerify(exactly = 2) { mockClerkApiService.tokens("session_123") }

      abandoned.cancel()
      cancellingScope.cancel()
      freshScope.cancel()
    }

  private fun buildJwt(oiat: Long): String {
    val header = """{"alg":"RS256","typ":"JWT","oiat":$oiat}"""
    val payload = """{"sub":"user_1","iat":$oiat}"""
    return "${header.base64Url()}.${payload.base64Url()}.signature"
  }

  /** Installs a current client whose "session_123" session carries [jwt] as its lastActiveToken. */
  private fun stubCurrentSessionToken(jwt: String) {
    val currentSession = mockk<Session>(relaxed = true)
    every { currentSession.id } returns "session_123"
    every { currentSession.lastActiveToken } returns TokenResource(jwt = jwt)
    val client = mockk<Client>(relaxed = true)
    every { client.sessions } returns listOf(currentSession)
    every { Clerk.clientInitialized } returns true
    every { Clerk.client } returns client
  }

  private fun String.base64Url(): String =
    Base64.encodeToString(toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}
