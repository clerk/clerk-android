package com.clerk.api.network.serialization

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import kotlin.coroutines.cancellation.CancellationException

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

internal object LocalFailureCodes {
  const val INVALID_ARGUMENTS = "invalid_arguments"

  const val MISSING_RESOURCE_DATA = "missing_resource_data"
}
