package com.clerk.api.session

import com.clerk.api.network.model.token.TokenResource
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TokenFreshnessTest {
  @After
  fun tearDown() {
    SessionTokensCache.clear()
  }

  @Test
  fun `keeps token with higher origin issued at`() {
    val existing = token(originIssuedAt = 200, issuedAt = 200)
    val incoming = token(originIssuedAt = 100, issuedAt = 300)

    val result = TokenFreshness.pickFreshest(existing, incoming)

    assertEquals(existing, result)
  }

  @Test
  fun `uses issued at to break equal origin issued at`() {
    val existing = token(originIssuedAt = 200, issuedAt = 300)
    val incoming = token(originIssuedAt = 200, issuedAt = 400)

    val result = TokenFreshness.pickFreshest(existing, incoming)

    assertEquals(incoming, result)
  }

  @Test
  fun `replaces an expired existing token`() {
    val existing = token(originIssuedAt = 300, issuedAt = 300, expiresAt = 900)
    val incoming = token(originIssuedAt = 100, issuedAt = 100, expiresAt = 2_000)

    val result = TokenFreshness.pickFreshest(existing, incoming, nowMillis = 1_000_000)

    assertEquals(incoming, result)
  }

  @Test
  fun `accepts incoming token when organization changes`() {
    val existing = token(organizationId = "org_one", originIssuedAt = 300, issuedAt = 300)
    val incoming = token(organizationId = "org_two", originIssuedAt = 100, issuedAt = 100)

    val result = TokenFreshness.pickFreshest(existing, incoming)

    assertEquals(incoming, result)
  }

  @Test
  fun `keeps decodable existing token when incoming cannot be decoded`() {
    val existing = token(originIssuedAt = 100, issuedAt = 100)
    val incoming = TokenResource("malformed")

    val result = TokenFreshness.pickFreshest(existing, incoming)

    assertEquals(existing, result)
  }

  @Test
  fun `cache cannot be rolled back by a stale response`() {
    val cacheKey = "session-organization-"
    val stale = token(originIssuedAt = 100, issuedAt = 100, signature = "stale")
    val fresh = token(originIssuedAt = 200, issuedAt = 200, signature = "fresh")

    val firstStore = SessionTokensCache.storeIfFresher(cacheKey, fresh)
    val staleStore = SessionTokensCache.storeIfFresher(cacheKey, stale)
    val duplicateStore = SessionTokensCache.storeIfFresher(cacheKey, fresh)

    assertTrue(firstStore.didChangeCanonicalToken)
    assertFalse(staleStore.didChangeCanonicalToken)
    assertFalse(duplicateStore.didChangeCanonicalToken)
    assertEquals(fresh, SessionTokensCache.getToken(cacheKey))
  }

  @Test
  fun `out of order cache writes retain the freshest response`() = runTest {
    val cacheKey = "session-organization-"
    val stale = token(originIssuedAt = 100, issuedAt = 100, signature = "stale")
    val fresh = token(originIssuedAt = 200, issuedAt = 200, signature = "fresh")
    val releaseStaleResponse = CompletableDeferred<Unit>()
    val staleResponse = async {
      releaseStaleResponse.await()
      SessionTokensCache.storeIfFresher(cacheKey, stale)
    }

    SessionTokensCache.storeIfFresher(cacheKey, fresh)
    releaseStaleResponse.complete(Unit)
    staleResponse.await()

    assertEquals(fresh, SessionTokensCache.getToken(cacheKey))
  }

  @Test
  fun `hydration preserves canonical token on a timestamp tie`() {
    val cacheKey = "session-organization-"
    val canonical = token(originIssuedAt = 100, issuedAt = 100, signature = "canonical")
    val snapshot = token(originIssuedAt = 100, issuedAt = 100, signature = "snapshot")
    SessionTokensCache.setToken(cacheKey, canonical)

    SessionTokensCache.hydrate(cacheKey, snapshot)

    assertEquals(canonical, SessionTokensCache.getToken(cacheKey))
  }

  private fun token(
    sessionId: String = "session",
    organizationId: String? = null,
    originIssuedAt: Long?,
    issuedAt: Long,
    expiresAt: Long = 4_000_000_000,
    signature: String = "signature",
  ): TokenResource {
    val headerClaims =
      buildList {
          add("\"alg\":\"none\"")
          add("\"typ\":\"JWT\"")
          originIssuedAt?.let { add("\"oiat\":$it") }
        }
        .joinToString(",")
    val payloadClaims =
      buildList {
          add("\"sid\":\"$sessionId\"")
          add("\"iat\":$issuedAt")
          add("\"exp\":$expiresAt")
          organizationId?.let { add("\"org_id\":\"$it\"") }
        }
        .joinToString(",")
    return TokenResource("${encode("{$headerClaims}")}.${encode("{$payloadClaims}")}.$signature")
  }

  private fun encode(value: String): String =
    Base64.getUrlEncoder()
      .withoutPadding()
      .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
}
