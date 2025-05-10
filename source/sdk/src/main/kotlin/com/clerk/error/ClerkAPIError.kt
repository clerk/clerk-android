package com.clerk.error

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** An object that represents an error returned by the Clerk API. */
@Serializable
data class ClerkAPIError(
  /** A string code that represents the error, such as `username_exists_code`. */
  val code: String,
  /** A message that describes the error. */
  val message: String? = null,
  /** A more detailed message that describes the error. */
  val longMessage: String? = null,
  /** Additional information about the error. */
  val meta: JsonElement? = null,
  /** A unique identifier for tracing the specific request, useful for debugging. */
  val clerkTraceId: String? = null,
)

/**
 * Represents the body of Clerk API error responses.
 *
 * The `ClerkErrorResponse` structure encapsulates multiple API errors that may occur during a
 * request. It also includes a unique trace ID for debugging purposes.
 */
@Serializable
data class ClerkErrorResponse(
  /** An array of `ClerkAPIError` objects, each describing an individual error. */
  val errors: List<ClerkAPIError>,
  /** A unique identifier for tracing the specific request, useful for debugging. */
  val clerkTraceId: String? = null,
)
