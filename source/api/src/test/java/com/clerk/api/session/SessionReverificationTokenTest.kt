package com.clerk.api.session

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.AuthConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.network.serialization.ClerkResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionReverificationTokenTest {
  private val fetcher = SessionTokenFetcher()
  private val session =
    Session(
      id = "session_123",
      status = Session.SessionStatus.ACTIVE,
      lastActiveOrganizationId = "org_123",
      expireAt = 4_000_000_000_000,
      lastActiveAt = 0,
      createdAt = 0,
      updatedAt = 0,
    )
  private val client = MutableStateFlow(Client(sessions = listOf(session)))
  private val api = mockk<SessionApi>()

  @Before
  fun setup() {
    SessionTokensCache.clear()
    mockkObject(Clerk, ClerkApi)
    every { Clerk.clientFlow } returns client
    every { ClerkApi.session } returns api
    val environment = mockk<Environment>()
    every { environment.authConfig } returns
      AuthConfig(singleSessionMode = false, sessionMinter = true)
    every { Clerk.environment } returns environment
  }

  @After
  fun tearDown() {
    unmockkAll()
    SessionTokensCache.clear()
  }

  @Test
  fun `newer client token replaces the cached token after reverification`() = runTest {
    verifyNewerClientToken(sessionMinter = true)
  }

  @Test
  fun `newer client token without origin header replaces the cached token`() = runTest {
    verifyNewerClientToken(sessionMinter = false)
  }

  private suspend fun verifyNewerClientToken(sessionMinter: Boolean) {
    val environment = mockk<Environment>()
    every { environment.authConfig } returns
      AuthConfig(singleSessionMode = false, sessionMinter = sessionMinter)
    every { Clerk.environment } returns environment
    val oldToken = token(100, originIssuedAt = 100.takeIf { sessionMinter }?.toLong())
    val verifiedToken = token(200, originIssuedAt = 200.takeIf { sessionMinter }?.toLong())
    val updatedToken =
      token(300, originIssuedAt = 300.takeIf { sessionMinter }?.toLong(), permission = "write")
    updateSnapshot(oldToken)
    coEvery { api.tokens(session.id, "org_123", any(), any()) } returns
      ClerkResult.success(verifiedToken)
    fetcher.invalidateSession(session.id)
    assertEquals(verifiedToken, fetcher.getToken(session))

    updateSnapshot(updatedToken)

    assertEquals(updatedToken, fetcher.getToken(session))
    assertEquals(updatedToken, SessionTokensCache.getToken(session.tokenCacheKey(null)))
    coVerify(exactly = 1) { api.tokens(session.id, "org_123", any(), any()) }
  }

  @Test
  fun `older and equal origin snapshots cannot replace the post-verification token`() = runTest {
    val verifiedToken = token(200)
    coEvery { api.tokens(session.id, "org_123", any(), any()) } returns
      ClerkResult.success(verifiedToken)
    fetcher.invalidateSession(session.id)
    assertEquals(verifiedToken, fetcher.getToken(session))

    // Edge renewal can advance iat without refreshing the underlying origin claims.
    for (snapshot in listOf(token(400, originIssuedAt = 100), token(400, originIssuedAt = 200))) {
      updateSnapshot(snapshot)
      assertEquals(verifiedToken, fetcher.getToken(session))
    }
    coVerify(exactly = 1) { api.tokens(session.id, "org_123", any(), any()) }
  }

  @Test
  fun `expired verified token is renewed without falling back to an older origin snapshot`() =
    runTest {
      val verifiedToken = token(200, expiresAt = 300)
      val renewedToken = token(400, originIssuedAt = 200)
      coEvery { api.tokens(session.id, "org_123", null, "true") } returns
        ClerkResult.success(verifiedToken)
      fetcher.invalidateSession(session.id)
      assertEquals(verifiedToken, fetcher.getToken(session))
      updateSnapshot(token(500, originIssuedAt = 100))
      coEvery { api.tokens(session.id, "org_123", verifiedToken.jwt, null) } returns
        ClerkResult.success(renewedToken)

      assertEquals(renewedToken, fetcher.getToken(session))
      assertEquals(renewedToken, SessionTokensCache.getToken(session.tokenCacheKey(null)))
      coVerify(exactly = 1) { api.tokens(session.id, "org_123", verifiedToken.jwt, null) }
    }

  @Test
  fun `verified token with origin header rejects a snapshot without one`() = runTest {
    verifyMixedOriginSnapshot(verifiedHasOrigin = true, expired = false)
  }

  @Test
  fun `verified token without origin header rejects a snapshot with one`() = runTest {
    verifyMixedOriginSnapshot(verifiedHasOrigin = false, expired = false)
  }

  @Test
  fun `expired verified token with origin header is renewed instead of adopting a snapshot without one`() =
    runTest {
      verifyMixedOriginSnapshot(verifiedHasOrigin = true, expired = true)
    }

  @Test
  fun `expired verified token without origin header is renewed instead of adopting a snapshot with one`() =
    runTest {
      verifyMixedOriginSnapshot(verifiedHasOrigin = false, expired = true)
    }

  private suspend fun verifyMixedOriginSnapshot(verifiedHasOrigin: Boolean, expired: Boolean) {
    val verifiedToken =
      token(
        200,
        originIssuedAt = 200L.takeIf { verifiedHasOrigin },
        expiresAt = if (expired) 300 else 4_000_000_000,
      )
    coEvery { api.tokens(session.id, "org_123", null, "true") } returns
      ClerkResult.success(verifiedToken)
    fetcher.invalidateSession(session.id)
    assertEquals(verifiedToken, fetcher.getToken(session))

    updateSnapshot(token(300, originIssuedAt = 300L.takeUnless { verifiedHasOrigin }))
    val renewedToken = token(400, originIssuedAt = 400L.takeIf { verifiedHasOrigin })
    coEvery { api.tokens(session.id, "org_123", verifiedToken.jwt, null) } returns
      ClerkResult.success(renewedToken)

    val expectedToken = if (expired) renewedToken else verifiedToken
    assertEquals(expectedToken, fetcher.getToken(session))
    assertEquals(expectedToken, SessionTokensCache.getToken(session.tokenCacheKey(null)))
    coVerify(exactly = 1) { api.tokens(session.id, "org_123", null, "true") }
    coVerify(exactly = if (expired) 1 else 0) {
      api.tokens(session.id, "org_123", verifiedToken.jwt, null)
    }
  }

  @Test
  fun `repeated reverification requires a new origin response before trusting snapshots`() =
    runTest {
      val firstVerifiedToken = token(200)
      coEvery { api.tokens(session.id, "org_123", null, "true") } returns
        ClerkResult.success(firstVerifiedToken)
      fetcher.invalidateSession(session.id)
      assertEquals(firstVerifiedToken, fetcher.getToken(session))
      val laterSnapshot = token(300)
      updateSnapshot(laterSnapshot)
      assertEquals(laterSnapshot, fetcher.getToken(session))

      fetcher.invalidateSession(session.id)
      val secondVerifiedToken = token(400)
      coEvery { api.tokens(session.id, "org_123", laterSnapshot.jwt, "true") } returns
        ClerkResult.success(secondVerifiedToken)
      assertEquals(secondVerifiedToken, fetcher.getToken(session))
      assertEquals(secondVerifiedToken, fetcher.getToken(session))
      coVerify(exactly = 1) { api.tokens(session.id, "org_123", laterSnapshot.jwt, "true") }
    }

  private fun updateSnapshot(token: TokenResource) {
    client.value = Client(sessions = listOf(session.copy(lastActiveToken = token)))
  }

  private fun token(
    issuedAt: Long,
    originIssuedAt: Long? = issuedAt,
    expiresAt: Long = 4_000_000_000,
    permission: String = "read",
  ): TokenResource {
    val header = originIssuedAt?.let { """{"alg":"none","oiat":$it}""" } ?: """{"alg":"none"}"""
    val payload =
      """{"sid":"session_123","org_id":"org_123","iat":$issuedAt,"exp":$expiresAt,"org_permissions":["$permission"]}"""
    return TokenResource("${encode(header)}.${encode(payload)}.")
  }

  private fun encode(value: String): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
}
