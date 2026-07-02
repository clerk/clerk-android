package com.clerk.api.billing

import java.security.MessageDigest

/**
 * Derives the obfuscated account identifier stamped on Google Play purchases.
 *
 * Google's `obfuscatedExternalAccountId` binds a store transaction to the purchasing user without
 * exposing the raw user identifier to the Play Store. Clerk uses a deterministic derivation — the
 * SHA-256 hex digest of the Clerk user ID, truncated to 64 characters (Google's documented limit) —
 * so the backend can recompute the same value from the session user and cross-check the binding
 * during purchase verification.
 */
internal object ObfuscatedAccountId {
  /** Google Play's maximum length for obfuscated account identifiers. */
  private const val MAX_LENGTH = 64

  /** Returns the obfuscated account identifier for the given Clerk [userId]. */
  fun fromUserId(userId: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(userId.toByteArray(Charsets.UTF_8))
    return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }.take(MAX_LENGTH)
  }
}
