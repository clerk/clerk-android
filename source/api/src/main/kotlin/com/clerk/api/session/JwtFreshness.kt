package com.clerk.api.session

import com.auth0.android.jwt.JWT
import java.util.concurrent.TimeUnit

/**
 * Orders two session JWTs by how recently their claims were assembled.
 *
 * Minted tokens carry `oiat` (original issued at) in the protected header. It survives re-minting,
 * so it is the primary ordering claim; the payload's `iat` only breaks a tie between two tokens
 * assembled at the same instant. A token without `oiat` comes from a pre-minter code path and is
 * staler than any token that has one.
 *
 * This mirrors clerk-js `pickFreshestJwt`, so web and Android agree on which of two tokens wins.
 */
internal object JwtFreshness {

  private const val ORIGINAL_ISSUED_AT_HEADER = "oiat"

  /** Whether the JWT can be decoded at all. An undecodable JWT is never sent or ranked. */
  internal fun isReadable(jwt: String): Boolean = claimsOf(jwt) != null

  /**
   * Whether [incoming] should replace [existing].
   *
   * A full tie accepts [incoming]: two tokens assembled at the same instant can still differ in
   * other claims, so only a strictly fresher [existing] suppresses the write. An unreadable
   * [incoming] never wins over a readable [existing].
   */
  internal fun incomingWins(existing: String, incoming: String): Boolean {
    val existingClaims = claimsOf(existing)
    val incomingClaims = claimsOf(incoming)
    return when {
      existingClaims == null -> true
      incomingClaims == null -> false
      else -> incomingIsAtLeastAsFresh(existing = existingClaims, incoming = incomingClaims)
    }
  }

  private fun incomingIsAtLeastAsFresh(existing: Claims, incoming: Claims): Boolean {
    val existingOriginalIssuedAt = existing.originalIssuedAt
    val incomingOriginalIssuedAt = incoming.originalIssuedAt
    return when {
      existingOriginalIssuedAt == null && incomingOriginalIssuedAt == null -> true
      incomingOriginalIssuedAt == null -> false
      existingOriginalIssuedAt == null -> true
      existingOriginalIssuedAt != incomingOriginalIssuedAt ->
        existingOriginalIssuedAt < incomingOriginalIssuedAt
      else -> (existing.issuedAt ?: 0L) <= (incoming.issuedAt ?: 0L)
    }
  }

  private fun claimsOf(jwt: String): Claims? =
    runCatching {
        val decoded = JWT(jwt)
        Claims(
          originalIssuedAt = decoded.header[ORIGINAL_ISSUED_AT_HEADER]?.toLongOrNull(),
          issuedAt = decoded.issuedAt?.let { TimeUnit.MILLISECONDS.toSeconds(it.time) },
        )
      }
      .getOrNull()

  private data class Claims(val originalIssuedAt: Long?, val issuedAt: Long?)
}
