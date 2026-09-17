package com.clerk.api.session

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.AuthConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.user.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.util.Base64
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionReverificationTokenTest {
  private val fetcher = SessionTokenFetcher.shared
  private val session =
    Session(
      id = "session_123",
      status = Session.SessionStatus.ACTIVE,
      user = mockk<User>(relaxed = true) { every { id } returns "user_123" },
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
    fetcher.reset()
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
    fetcher.reset()
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

    val updatedSession = updateSnapshot(updatedToken)

    assertTrue(updatedSession.has(feature = "write"))
    assertTrue(updatedSession.checkAuthorization(feature = "write"))
    assertFalse(updatedSession.has(feature = "read"))
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

  @Test
  fun `authorization uses verified claims instead of rejected snapshots`() = runTest {
    val now = System.currentTimeMillis() / 1_000
    val verifiedToken = token(now)
    coEvery { api.tokens(session.id, "org_123", any(), any()) } returns
      ClerkResult.success(verifiedToken)
    fetcher.invalidateSession(session.id)
    assertEquals(verifiedToken, fetcher.getToken(session))

    for (originIssuedAt in listOf(now - 1, now, null)) {
      val snapshot =
        updateSnapshot(
          token(
            now + 1,
            originIssuedAt = originIssuedAt,
            permission = "stale",
            factorAges = "[20,20]",
          )
        )
      assertEquals(verifiedToken, fetcher.getToken(snapshot))
      assertTrue(snapshot.has(reverification = ReverificationConfig.Strict))
      assertTrue(snapshot.checkAuthorization(reverification = ReverificationConfig.StrictMfa))
      assertTrue(snapshot.has(feature = "read"))
      assertFalse(snapshot.has(feature = "stale"))
    }
    coVerify(exactly = 1) { api.tokens(session.id, "org_123", any(), any()) }
  }

  @Test
  fun `authorization rejects origin snapshot after verification with a headerless token`() =
    runTest {
      val now = System.currentTimeMillis() / 1_000
      val verifiedToken = token(now, originIssuedAt = null)
      coEvery { api.tokens(session.id, "org_123", any(), any()) } returns
        ClerkResult.success(verifiedToken)
      fetcher.invalidateSession(session.id)
      assertEquals(verifiedToken, fetcher.getToken(session))

      val snapshot = updateSnapshot(token(now + 1, permission = "stale", factorAges = "[20,20]"))
      assertEquals(verifiedToken, fetcher.getToken(snapshot))
      assertTrue(snapshot.has(reverification = ReverificationConfig.Strict))
      assertTrue(snapshot.checkAuthorization(feature = "read"))
      assertFalse(snapshot.has(feature = "stale"))
    }

  @Test
  fun `authorization rejects snapshots until a post-verification token is fetched`() = runTest {
    val now = System.currentTimeMillis() / 1_000
    val snapshot = updateSnapshot(token(now - 20 * 60)).copy(factorVerificationAge = listOf(0, 0))
    assertFalse(snapshot.has(reverification = ReverificationConfig.Strict))
    fetcher.invalidateSession(session.id)

    assertFalse(snapshot.has(reverification = ReverificationConfig.Strict))
    assertFalse(snapshot.checkAuthorization(feature = "read"))
    coVerify(exactly = 0) { api.tokens(session.id, "org_123", any(), any()) }

    val verifiedToken = token(now + 1)
    coEvery { api.tokens(session.id, "org_123", any(), any()) } returns
      ClerkResult.success(verifiedToken)
    assertEquals(verifiedToken, fetcher.getToken(snapshot))
    assertTrue(snapshot.has(reverification = ReverificationConfig.Strict))
    assertTrue(snapshot.checkAuthorization(feature = "read"))
  }

  @Test
  fun `authorization requires verified token factor ages after reverification`() = runTest {
    val now = System.currentTimeMillis() / 1_000
    val snapshot = session.copy(factorVerificationAge = listOf(0, 0))
    val verifiedToken = token(now, factorAges = "null")
    coEvery { api.tokens(session.id, "org_123", any(), any()) } returns
      ClerkResult.success(verifiedToken)
    fetcher.invalidateSession(session.id)
    assertEquals(verifiedToken, fetcher.getToken(snapshot))

    assertFalse(snapshot.has(reverification = ReverificationConfig.Strict))
    assertFalse(snapshot.checkAuthorization(reverification = ReverificationConfig.StrictMfa))
    assertTrue(snapshot.has(feature = "read"))
  }

  @Test
  fun `reset restores authorization from session snapshots`() {
    val snapshot = updateSnapshot(token(System.currentTimeMillis() / 1_000))
    fetcher.invalidateSession(session.id)
    assertFalse(snapshot.has(feature = "read"))

    fetcher.reset()

    assertTrue(snapshot.has(feature = "read"))
    assertTrue(snapshot.checkAuthorization(reverification = ReverificationConfig.Strict))
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `reverification fences a completed shared request before its waiter resumes`() = runTest {
    val oldToken = token(System.currentTimeMillis() / 1_000, factorAges = "[20,20]")
    val response = CompletableDeferred<TokenResource>()
    coEvery { api.tokens(session.id, "org_123", any(), any()) } coAnswers
      {
        ClerkResult.success(response.await())
      }
    val owner = async(UnconfinedTestDispatcher(testScheduler)) { fetcher.getToken(session) }
    val waiter = async { fetcher.getToken(session) }
    testScheduler.runCurrent()

    // Complete the owner immediately, leaving the waiter's continuation queued.
    response.complete(oldToken)
    assertTrue(owner.isCompleted)
    assertEquals(oldToken, owner.await())
    assertFalse(waiter.isCompleted)
    fetcher.invalidateSession(session.id)

    assertNull(waiter.await())
    coVerify(exactly = 1) { api.tokens(session.id, "org_123", any(), any()) }
  }

  private fun updateSnapshot(token: TokenResource): Session {
    val updatedSession = session.copy(lastActiveToken = token)
    client.value = Client(sessions = listOf(updatedSession))
    return updatedSession
  }

  private fun token(
    issuedAt: Long,
    originIssuedAt: Long? = issuedAt,
    expiresAt: Long = 4_000_000_000,
    permission: String = "read",
    factorAges: String = "[0,0]",
  ): TokenResource {
    val header = originIssuedAt?.let { """{"alg":"none","oiat":$it}""" } ?: """{"alg":"none"}"""
    val payload =
      """
      {"sid":"session_123","org_id":"org_123","iat":$issuedAt,"exp":$expiresAt,
       "org_permissions":["$permission"],"fea":"u:$permission","fva":$factorAges}
      """
        .trimIndent()
    return TokenResource("${encode(header)}.${encode(payload)}.")
  }

  private fun encode(value: String): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
}
