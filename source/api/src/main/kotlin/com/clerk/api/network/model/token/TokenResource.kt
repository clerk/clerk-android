package com.clerk.api.network.model.token

import kotlinx.serialization.Serializable

/**
 * Represents information about a token.
 *
 * The `TokenResource` structure encapsulates a token, such as a JWT.
 */
@Serializable
data class TokenResource(
  /** The jwt represented as a `String`. */
  val jwt: String
)
