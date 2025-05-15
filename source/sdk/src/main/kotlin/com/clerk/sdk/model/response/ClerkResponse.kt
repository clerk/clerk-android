package com.clerk.sdk.model.response

import com.clerk.sdk.model.error.ClerkErrorResponse

/** Represents a response from the Clerk API that can be either a success or an error. */
sealed class ClerkResponse<out T> {
  /** Represents a successful response from the Clerk API. */
  data class Success<T>(val data: T) : ClerkResponse<T>()

  /** Represents an error response from the Clerk API. */
  data class Error(val errorResponse: ClerkErrorResponse) : ClerkResponse<Nothing>()
}
