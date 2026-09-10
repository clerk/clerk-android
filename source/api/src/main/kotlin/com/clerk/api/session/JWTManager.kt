package com.clerk.api.session

import com.auth0.android.jwt.JWT

/** Abstracts the creation of a JWT object. Used for testing purposes. */
internal interface JWTManager {
  /**
   * Creates a JWT object from a string.
   *
   * @param jwt The JWT string to create the JWT object from.
   * @return The JWT object.
   */
  fun createFromString(jwt: String): JWT
}

internal class JWTManagerImpl : JWTManager {
  override fun createFromString(jwt: String): JWT {
    return JWT(jwt)
  }
}

internal fun JWTManager.featuresClaim(jwt: String): String {
  return try {
    createFromString(jwt).getClaim("fea").asString().orEmpty()
  } catch (_: Exception) {
    ""
  }
}

internal fun JWTManager.plansClaim(jwt: String): String {
  return try {
    createFromString(jwt).getClaim("pla").asString().orEmpty()
  } catch (_: Exception) {
    ""
  }
}

internal fun JWTManager.factorVerificationAgeClaim(jwt: String): List<Int>? {
  return try {
    val claim = createFromString(jwt).getClaim("fva")
    val values =
      claim.asList(Integer::class.java)?.map { it.toInt() }
        ?: claim.asList(java.lang.Long::class.java)?.map { it.toInt() }
    values?.takeIf { it.size == 2 }
  } catch (_: Exception) {
    null
  }
}

internal fun JWTManager.issuedAtMillis(jwt: String): Long? {
  return try {
    createFromString(jwt).issuedAt?.time
  } catch (_: Exception) {
    null
  }
}

private const val MILLIS_PER_MINUTE = 60_000L

internal fun ageFactorVerification(
  factorVerificationAge: List<Int>,
  issuedAtMillis: Long?,
  nowMillis: Long,
): List<Int> {
  val elapsedMinutes =
    issuedAtMillis?.let { issuedAt ->
      ((nowMillis - issuedAt).coerceAtLeast(0L) / MILLIS_PER_MINUTE).toInt()
    } ?: 0
  if (elapsedMinutes == 0) {
    return factorVerificationAge
  }
  return factorVerificationAge.map { age -> if (age >= 0) age + elapsedMinutes else age }
}
