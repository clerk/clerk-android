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
