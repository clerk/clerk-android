package com.clerk.sdk.model.response

import com.clerk.sdk.model.error.ClerkErrorResponse

/** Represents a response from the Clerk API that can be either a success or an error. */
sealed class ClerkResponse<out T> {
  /** Represents a successful response from the Clerk API. */
  data class Success<T>(val data: T) : ClerkResponse<T>()

  /** Represents an error response from the Clerk API. */
  data class Error(val errorResponse: ClerkErrorResponse) : ClerkResponse<Nothing>()

  /**
   * Transforms this response by applying [onSuccess] function if this is a [Success] or [onError]
   * function if this is an [Error].
   *
   * @param onSuccess A function to apply on the successful value
   * @param onError A function to apply on the error value
   * @return The result of applying the appropriate function
   */
  /**
   * Transforms this response by applying [onSuccess] function if this is a [Success] or [onError]
   * function if this is an [Error].
   *
   * @param onSuccess A function to apply on the successful data
   * @param onError A function to apply on the error response
   * @return The result of applying the appropriate function
   */
  fun <R> fold(onSuccess: (T) -> R, onError: (ClerkErrorResponse) -> R): R {
    return when (this) {
      is Success -> onSuccess(data)
      is Error -> onError(errorResponse)
    }
  }

  /**
   * Transforms this response into another response by applying the given [transform] function if
   * this is a [Success]. If this is an [Error], the error is propagated.
   *
   * @param transform A function to transform the success value into another [ClerkResponse]
   * @return The transformed response if success, or the original error
   */
  suspend fun <R> flatMap(transform: suspend (T) -> ClerkResponse<R>): ClerkResponse<R> {
    return when (this) {
      is Success -> transform(data)
      is Error -> this
    }
  }

  /**
   * Executes the given [block] if this is a [Success]. Returns the original response unchanged to
   * enable chaining.
   */
  fun onSuccess(block: (T) -> Unit): ClerkResponse<T> {
    if (this is Success) block(data)
    return this
  }

  /**
   * Executes the given [block] if this is an [Error]. Returns the original response unchanged to
   * enable chaining.
   */
  fun onError(block: (ClerkErrorResponse) -> Unit): ClerkResponse<T> {
    if (this is Error) block(errorResponse)
    return this
  }
}
