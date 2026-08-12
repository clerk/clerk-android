package com.clerk.api.session

import com.auth0.android.jwt.JWT
import com.clerk.api.network.model.token.TokenResource

/** Chooses the canonical token when session minter responses arrive out of order. */
internal object TokenFreshness {
  private data class DecodedToken(val resource: TokenResource, val jwt: JWT)

  internal enum class TieBreaker {
    EXISTING,
    INCOMING,
  }

  internal fun pickFreshest(
    existing: TokenResource?,
    incoming: TokenResource,
    nowMillis: Long = System.currentTimeMillis(),
    tieBreaker: TieBreaker = TieBreaker.INCOMING,
  ): TokenResource {
    existing ?: return incoming

    val existingJwt = decode(existing.jwt)
    val incomingJwt = decode(incoming.jwt)
    return when {
      existingJwt != null && incomingJwt != null ->
        pickFreshestDecoded(
          existing = DecodedToken(existing, existingJwt),
          incoming = DecodedToken(incoming, incomingJwt),
          nowMillis = nowMillis,
          tieBreaker = tieBreaker,
        )
      existingJwt != null -> existing
      else -> incoming
    }
  }

  internal fun matches(token: TokenResource, sessionId: String, organizationId: String?): Boolean {
    val jwt = decode(token.jwt)
    val tokenSessionId = jwt?.getClaim("sid")?.asString()
    return if (jwt == null || tokenSessionId == null) {
      false
    } else {
      tokenSessionId == sessionId && jwt.organizationId().orEmpty() == organizationId.orEmpty()
    }
  }

  private fun pickFreshestDecoded(
    existing: DecodedToken,
    incoming: DecodedToken,
    nowMillis: Long,
    tieBreaker: TieBreaker,
  ): TokenResource =
    if (!haveMatchingContext(existing.jwt, incoming.jwt)) {
      incoming.resource
    } else {
      pickByExpiration(existing, incoming, nowMillis)
        ?: pickByOriginIssuedAt(existing, incoming, tieBreaker)
    }

  private fun haveMatchingContext(existing: JWT, incoming: JWT): Boolean =
    existing.getClaim("sid").asString() == incoming.getClaim("sid").asString() &&
      existing.organizationId().orEmpty() == incoming.organizationId().orEmpty()

  private fun pickByExpiration(
    existing: DecodedToken,
    incoming: DecodedToken,
    nowMillis: Long,
  ): TokenResource? {
    val existingIsExpired = existing.jwt.expiresAt?.time?.let { it <= nowMillis }
    val incomingIsExpired = incoming.jwt.expiresAt?.time?.let { it <= nowMillis }
    return when {
      existingIsExpired == true && incomingIsExpired == false -> incoming.resource
      existingIsExpired == false && incomingIsExpired == true -> existing.resource
      else -> null
    }
  }

  private fun pickByOriginIssuedAt(
    existing: DecodedToken,
    incoming: DecodedToken,
    tieBreaker: TieBreaker,
  ): TokenResource {
    val existingOriginIssuedAt = existing.jwt.originIssuedAt()
    val incomingOriginIssuedAt = incoming.jwt.originIssuedAt()
    return when {
      existingOriginIssuedAt == null && incomingOriginIssuedAt == null ->
        pickByIssuedAt(existing, incoming, tieBreaker)
      existingOriginIssuedAt != null && incomingOriginIssuedAt == null -> existing.resource
      existingOriginIssuedAt == null -> incoming.resource
      existingOriginIssuedAt > checkNotNull(incomingOriginIssuedAt) -> existing.resource
      incomingOriginIssuedAt > existingOriginIssuedAt -> incoming.resource
      else -> pickByIssuedAt(existing, incoming, tieBreaker)
    }
  }

  private fun pickByIssuedAt(
    existing: DecodedToken,
    incoming: DecodedToken,
    tieBreaker: TieBreaker,
  ): TokenResource {
    val existingIssuedAt = existing.jwt.issuedAt?.time ?: 0
    val incomingIssuedAt = incoming.jwt.issuedAt?.time ?: 0
    return when {
      existingIssuedAt > incomingIssuedAt -> existing.resource
      incomingIssuedAt > existingIssuedAt -> incoming.resource
      tieBreaker == TieBreaker.EXISTING -> existing.resource
      else -> incoming.resource
    }
  }

  private fun decode(token: String): JWT? =
    try {
      JWT(token)
    } catch (_: Exception) {
      null
    }

  private fun JWT.originIssuedAt(): Long? = header["oiat"]?.toLongOrNull()

  private fun JWT.organizationId(): String? =
    getClaim("org_id").asString()
      ?: runCatching { getClaim("o").asObject(Map::class.java)?.get("id") as? String }.getOrNull()
}
