package com.clerk.api.session

import com.auth0.android.jwt.JWT
import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.network.serialization.ClerkResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionTokenFetcherSignOutTest {

  private lateinit var sessionTokenFetcher: SessionTokenFetcher
  private lateinit var mockSession: Session
  private lateinit var mockTokenResource: TokenResource
  private lateinit var mockClerkApiService: SessionApi

  @Before
  fun setup() {
    mockSession = mockk(relaxed = true)
    mockTokenResource = mockk(relaxed = true)
    mockClerkApiService = mockk(relaxed = true)
    val mockJWTManager = mockk<JWTManager>(relaxed = true)
    every { mockJWTManager.createFromString(any()) } returns mockk<JWT>(relaxed = true)

    sessionTokenFetcher = SessionTokenFetcher(mockJWTManager)

    every { mockSession.id } returns "session_123"
    every { mockSession.status } returns Session.SessionStatus.ACTIVE

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
  fun `getToken refuses a held session after sign-out`() = runTest {
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.success(mockTokenResource)
    sessionTokenFetcher.signOut(listOf("session_123"))

    val result = sessionTokenFetcher.getTokenResult(mockSession)

    val failure = result as ClerkResult.Failure
    assertEquals(
      SessionTokenFetcher.TOKEN_REQUEST_SUPERSEDED_ERROR_CODE,
      failure.error?.errors?.firstOrNull()?.code,
    )
    coVerify(exactly = 0) { mockClerkApiService.tokens(any()) }
  }

  @Test
  fun `getToken refuses a held session after it was removed`() = runTest {
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.success(mockTokenResource)
    sessionTokenFetcher.removeSession("session_123")

    val result = sessionTokenFetcher.getTokenResult(mockSession)

    val failure = result as ClerkResult.Failure
    assertEquals(
      SessionTokenFetcher.TOKEN_REQUEST_SUPERSEDED_ERROR_CODE,
      failure.error?.errors?.firstOrNull()?.code,
    )
    coVerify(exactly = 0) { mockClerkApiService.tokens(any()) }
  }

  @Test
  fun `signOut keeps refusing a session that was removed earlier`() = runTest {
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.success(mockTokenResource)
    sessionTokenFetcher.removeSession("session_123")
    sessionTokenFetcher.signOut(listOf("session_456"))

    val result = sessionTokenFetcher.getTokenResult(mockSession)

    val failure = result as ClerkResult.Failure
    assertEquals(
      SessionTokenFetcher.TOKEN_REQUEST_SUPERSEDED_ERROR_CODE,
      failure.error?.errors?.firstOrNull()?.code,
    )
    coVerify(exactly = 0) { mockClerkApiService.tokens(any()) }
  }

  @Test
  fun `reset lifts the sign-out fence`() = runTest {
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.success(mockTokenResource)
    coEvery { SessionTokensCache.storeIfFresher(any(), mockTokenResource, any()) } returns
      SessionTokensCache.StoreResult(mockTokenResource, true)
    sessionTokenFetcher.removeSession("session_123")
    sessionTokenFetcher.reset()

    val result = sessionTokenFetcher.getToken(mockSession)

    assertSame(mockTokenResource, result)
  }

  @Test
  fun `getToken serves a signed-out session again once the client lists it`() = runTest {
    val activeSession = mockk<Session>(relaxed = true)
    every { activeSession.id } returns "session_123"
    every { activeSession.status } returns Session.SessionStatus.ACTIVE
    sessionTokenFetcher.signOut(listOf("session_123"))
    every { Clerk.clientFlow } returns MutableStateFlow(Client(sessions = listOf(activeSession)))
    coEvery { SessionTokensCache.getToken(any()) } returns null
    coEvery { mockClerkApiService.tokens("session_123") } returns
      ClerkResult.success(mockTokenResource)
    coEvery { SessionTokensCache.storeIfFresher(any(), mockTokenResource, any()) } returns
      SessionTokensCache.StoreResult(mockTokenResource, true)

    val result = sessionTokenFetcher.getToken(mockSession)

    assertSame(mockTokenResource, result)
  }
}
