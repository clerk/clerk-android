package com.clerk.network.model.error

import com.clerk.model.client.Client
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Represents an error response from the Clerk API. */
@Serializable
data class ClerkErrorResponse(
  /** An array of `ClerkAPIError` objects, each describing an individual error. */
  val errors: List<Error>,
  /** An object containing additional information about the error response. */
  val meta: Meta? = null,
  /** A unique identifier for tracing the specific request, useful for debugging. */
  @SerialName("clerk_trace_id") val clerkTraceId: String,
)

@Serializable
data class Error(
  /** A message that describes the error. */
  val message: String,
  /** A more detailed message that describes the error. */
  @SerialName("long_message") val longMessage: String,
  /** A string code that represents the error, such as `username_exists_code`. */
  val code: String,
  /** Additional information about the error. */
)

@Serializable data class Meta(val client: Client? = null)

fun ClerkErrorResponse.firstMessage(): String? = this.errors.firstOrNull()?.message
