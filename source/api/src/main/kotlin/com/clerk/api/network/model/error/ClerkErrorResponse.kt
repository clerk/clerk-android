package com.clerk.api.network.model.error

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Represents an error response from the Clerk API. */
@Serializable
data class ClerkErrorResponse(
  /** An array of `ClerkAPIError` objects, each describing an individual error. */
  val errors: List<Error>,
  /** An object containing additional information about the error response. */
  val meta: JsonObject? = null,
  /** A unique identifier for tracing the specific request, useful for debugging. */
  @SerialName("clerk_trace_id") val clerkTraceId: String? = null,
)

@Serializable
data class Error(
  /** A message that describes the error. */
  val message: String? = null,
  /** A more detailed message that describes the error. */
  @SerialName("long_message") val longMessage: String? = null,
  /** A string code that represents the error, such as `username_exists_code`. */
  val code: String? = null,
  /** Additional information about this specific error entry. */
  val meta: JsonObject? = null,
  /** Additional information about the error. */
)

fun ClerkErrorResponse.firstMessage(): String? = this.errors.firstOrNull()?.message
