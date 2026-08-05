package com.clerk.api.session

import android.util.Base64
import com.clerk.api.network.model.token.TokenResource
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
class SessionTokensCacheTest {

  @Before
  fun setup() {
    SessionTokensCache.clear()
  }

  @After
  fun tearDown() {
    SessionTokensCache.clear()
  }

  @Test
  fun `setToken stores the token when nothing is cached`() {
    val token = tokenWith(oiat = 1_000L)

    SessionTokensCache.setToken("sess_1", token)

    assertEquals(token, SessionTokensCache.getToken("sess_1"))
  }

  @Test
  fun `setToken replaces the cached token with a newer one`() {
    val older = tokenWith(oiat = 1_000L)
    val newer = tokenWith(oiat = 2_000L)

    SessionTokensCache.setToken("sess_1", older)
    SessionTokensCache.setToken("sess_1", newer)

    assertEquals(newer, SessionTokensCache.getToken("sess_1"))
  }

  @Test
  fun `setToken keeps the cached token when the incoming one is older`() {
    val newer = tokenWith(oiat = 2_000L)
    val older = tokenWith(oiat = 1_000L)

    SessionTokensCache.setToken("sess_1", newer)
    SessionTokensCache.setToken("sess_1", older)

    assertEquals(newer, SessionTokensCache.getToken("sess_1"))
  }

  @Test
  fun `setToken accepts an incoming token issued at the same instant`() {
    val first = tokenWith(oiat = 2_000L, jwtId = "first")
    val second = tokenWith(oiat = 2_000L, jwtId = "second")

    SessionTokensCache.setToken("sess_1", first)
    SessionTokensCache.setToken("sess_1", second)

    assertEquals(second, SessionTokensCache.getToken("sess_1"))
  }

  @Test
  fun `setToken accepts the incoming token when neither carries oiat`() {
    // Matches clerk-js pickFreshestJwt: with no oiat on either side the guard cannot prove the
    // cached token is fresher, so the incoming one wins regardless of iat. iat only breaks a tie
    // under an equal oiat, never on its own.
    val cached = tokenWith(iat = 2_000L)
    val incoming = tokenWith(iat = 1_000L)

    SessionTokensCache.setToken("sess_1", cached)
    SessionTokensCache.setToken("sess_1", incoming)

    assertEquals(incoming, SessionTokensCache.getToken("sess_1"))
  }

  @Test
  fun `setToken prefers oiat over iat when ordering`() {
    // A re-minted token can carry a fresh iat while its oiat still predates what is cached.
    val cached = tokenWith(oiat = 2_000L, iat = 2_000L)
    val incoming = tokenWith(oiat = 1_000L, iat = 9_000L)

    SessionTokensCache.setToken("sess_1", cached)
    SessionTokensCache.setToken("sess_1", incoming)

    assertEquals(cached, SessionTokensCache.getToken("sess_1"))
  }

  @Test
  fun `setToken keeps a readable cached token over an unreadable incoming one`() {
    val cached = tokenWith(oiat = 2_000L)
    val unreadable = TokenResource(jwt = "not-a-jwt")

    SessionTokensCache.setToken("sess_1", cached)
    SessionTokensCache.setToken("sess_1", unreadable)

    assertEquals(cached, SessionTokensCache.getToken("sess_1"))
  }

  @Test
  fun `setToken replaces an unreadable cached token with a readable one`() {
    val unreadable = TokenResource(jwt = "not-a-jwt")
    val readable = tokenWith(oiat = 2_000L)

    SessionTokensCache.setToken("sess_1", unreadable)
    SessionTokensCache.setToken("sess_1", readable)

    assertEquals(readable, SessionTokensCache.getToken("sess_1"))
  }

  @Test
  fun `setToken keeps a token carrying oiat over one that has none`() {
    val minted = tokenWith(oiat = 1_000L, iat = 1_000L)
    val preMinter = tokenWith(iat = 9_000L)

    SessionTokensCache.setToken("sess_1", minted)
    SessionTokensCache.setToken("sess_1", preMinter)

    assertEquals(minted, SessionTokensCache.getToken("sess_1"))
  }

  @Test
  fun `setToken accepts a token carrying oiat over a cached one without it`() {
    val preMinter = tokenWith(iat = 9_000L)
    val minted = tokenWith(oiat = 1_000L, iat = 1_000L)

    SessionTokensCache.setToken("sess_1", preMinter)
    SessionTokensCache.setToken("sess_1", minted)

    assertEquals(minted, SessionTokensCache.getToken("sess_1"))
  }

  @Test
  fun `setToken breaks an equal oiat by iat`() {
    val fresherMint = tokenWith(oiat = 2_000L, iat = 5_000L)
    val staleMint = tokenWith(oiat = 2_000L, iat = 4_000L)

    SessionTokensCache.setToken("sess_1", fresherMint)
    SessionTokensCache.setToken("sess_1", staleMint)

    assertEquals(fresherMint, SessionTokensCache.getToken("sess_1"))
  }

  @Test
  fun `setToken accepts a fresher iat under an equal oiat`() {
    val staleMint = tokenWith(oiat = 2_000L, iat = 4_000L)
    val fresherMint = tokenWith(oiat = 2_000L, iat = 5_000L)

    SessionTokensCache.setToken("sess_1", staleMint)
    SessionTokensCache.setToken("sess_1", fresherMint)

    assertEquals(fresherMint, SessionTokensCache.getToken("sess_1"))
  }

  @Test
  fun `setTokenIfCurrent writes when the session was not invalidated`() {
    val generation = SessionTokensCache.generation("sess_1")
    val token = tokenWith(oiat = 1_000L)

    SessionTokensCache.setTokenIfCurrent("sess_1", "sess_1", generation, token)

    assertEquals(token, SessionTokensCache.getToken("sess_1"))
  }

  @Test
  fun `setTokenIfCurrent discards a response that raced a session invalidation`() {
    val generation = SessionTokensCache.generation("sess_1")
    SessionTokensCache.removeTokensForSession("sess_1")

    SessionTokensCache.setTokenIfCurrent("sess_1", "sess_1", generation, tokenWith(oiat = 1_000L))

    assertNull(SessionTokensCache.getToken("sess_1"))
  }

  @Test
  fun `setTokenIfCurrent discards a response that raced a full clear`() {
    val generation = SessionTokensCache.generation("sess_1")
    SessionTokensCache.clear()

    SessionTokensCache.setTokenIfCurrent("sess_1", "sess_1", generation, tokenWith(oiat = 1_000L))

    assertNull(SessionTokensCache.getToken("sess_1"))
  }

  @Test
  fun `setTokenIfCurrent is unaffected by another session's invalidation`() {
    val generation = SessionTokensCache.generation("sess_1")
    SessionTokensCache.removeTokensForSession("sess_2")
    val token = tokenWith(oiat = 1_000L)

    SessionTokensCache.setTokenIfCurrent("sess_1", "sess_1", generation, token)

    assertEquals(token, SessionTokensCache.getToken("sess_1"))
  }

  @Test
  fun `removeTokensForSession resets the shared failure schedule for that session`() {
    SessionTokenBackoff.shared.recordFailure("sess_1")
    SessionTokenBackoff.shared.recordFailure("sess_1-custom_template")
    SessionTokenBackoff.shared.recordFailure("sess_2")

    SessionTokensCache.removeTokensForSession("sess_1")

    assertFalse(SessionTokenBackoff.shared.isBackingOff("sess_1"))
    assertFalse(SessionTokenBackoff.shared.isBackingOff("sess_1-custom_template"))
    assertTrue(SessionTokenBackoff.shared.isBackingOff("sess_2"))
  }

  @Test
  fun `clear resets every failure schedule`() {
    SessionTokenBackoff.shared.recordFailure("sess_1")

    SessionTokensCache.clear()

    assertFalse(SessionTokenBackoff.shared.isBackingOff("sess_1"))
  }

  @Test
  fun `recordBackoffOutcomeIfCurrent records a failure while the generation still matches`() {
    val backoff = SessionTokenBackoff { 0L }
    val generation = SessionTokensCache.generation("sess_1")

    SessionTokensCache.recordBackoffOutcomeIfCurrent(
      cacheKey = "sess_1",
      sessionId = "sess_1",
      generation = generation,
      succeeded = false,
      backoff = backoff,
    )

    assertTrue(backoff.isBackingOff("sess_1"))
  }

  @Test
  fun `recordBackoffOutcomeIfCurrent skips a failure once the session was invalidated`() {
    val backoff = SessionTokenBackoff { 0L }
    val generation = SessionTokensCache.generation("sess_1")
    SessionTokensCache.removeTokensForSession("sess_1")

    SessionTokensCache.recordBackoffOutcomeIfCurrent(
      cacheKey = "sess_1",
      sessionId = "sess_1",
      generation = generation,
      succeeded = false,
      backoff = backoff,
    )

    assertFalse(backoff.isBackingOff("sess_1"))
  }

  @Test
  fun `recordBackoffOutcomeIfCurrent skips a stale success so the new window survives`() {
    val backoff = SessionTokenBackoff { 0L }
    val staleGeneration = SessionTokensCache.generation("sess_1")
    SessionTokensCache.removeTokensForSession("sess_1")
    backoff.recordFailure("sess_1")

    SessionTokensCache.recordBackoffOutcomeIfCurrent(
      cacheKey = "sess_1",
      sessionId = "sess_1",
      generation = staleGeneration,
      succeeded = true,
      backoff = backoff,
    )

    assertTrue(backoff.isBackingOff("sess_1"))
  }

  @Test
  fun `removeTokensForSession drops the default and templated entries for that session only`() {
    val token = tokenWith(oiat = 1_000L)
    SessionTokensCache.setToken("sess_1", token)
    SessionTokensCache.setToken("sess_1-custom_template", token)
    SessionTokensCache.setToken("sess_2", token)

    SessionTokensCache.removeTokensForSession("sess_1")

    assertNull(SessionTokensCache.getToken("sess_1"))
    assertFalse(SessionTokensCache.containsKey("sess_1-custom_template"))
    assertTrue(SessionTokensCache.containsKey("sess_2"))
    assertEquals(1, SessionTokensCache.size)
  }

  private fun tokenWith(oiat: Long? = null, iat: Long? = null, jwtId: String? = null) =
    TokenResource(jwt = buildJwt(oiat = oiat, iat = iat, jwtId = jwtId))

  private fun buildJwt(oiat: Long?, iat: Long?, jwtId: String?): String {
    val header =
      buildString {
        append("""{"alg":"RS256","typ":"JWT"""")
        if (oiat != null) append(""","oiat":$oiat""")
        append("}")
      }
    val payload =
      buildString {
        append("""{"sub":"user_1"""")
        if (iat != null) append(""","iat":$iat""")
        if (jwtId != null) append(""","jti":"$jwtId"""")
        append("}")
      }
    return "${header.base64Url()}.${payload.base64Url()}.signature"
  }

  private fun String.base64Url(): String =
    Base64.encodeToString(toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}
