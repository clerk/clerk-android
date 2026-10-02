package com.clerk.api.network.serialization

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs [block] and turns any non-cancellation [Exception] it throws into
 * [ClerkResult.unknownFailure], after passing it to [onException].
 *
 * Use this instead of [runCatching] or `catch (e: Exception)` around code that can suspend:
 * [CancellationException] is rethrown so structured concurrency keeps working.
 */
internal inline fun <T : Any, E : Any> catchingClerkResult(
  onException: (Exception) -> Unit = {},
  block: () -> ClerkResult<T, E>,
): ClerkResult<T, E> =
  try {
    block()
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    onException(e)
    ClerkResult.unknownFailure(e)
  }

/**
 * Builds a failure for a problem detected on the device (invalid arguments, missing local state) in
 * the same [ClerkErrorResponse] shape the API uses, so callers can read [errorMessage] and the
 * error code the same way they do for server errors.
 */
internal fun localFailure(
  code: String,
  longMessage: String,
  message: String = longMessage,
): ClerkResult.Failure<ClerkErrorResponse> =
  ClerkResult.apiFailure(
    ClerkErrorResponse(
      errors = listOf(Error(message = message, longMessage = longMessage, code = code))
    )
  )

/** Error codes for failures built with [localFailure]. */
internal object LocalFailureCodes {
  /** A required argument was missing or a combination of arguments was invalid. */
  const val INVALID_ARGUMENTS = "invalid_arguments"

  /** The resource does not carry the data the operation needs (for example a missing id). */
  const val MISSING_RESOURCE_DATA = "missing_resource_data"
}
