package com.clerk.api.auth

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext

private object AuthErrorReportingScope : AbstractCoroutineContextElement(Key) {
  object Key : CoroutineContext.Key<AuthErrorReportingScope>
}

internal suspend fun <T : Any> Auth.reportingFailures(
  block: suspend () -> ClerkResult<T, ClerkErrorResponse>
): ClerkResult<T, ClerkErrorResponse> {
  if (currentCoroutineContext()[AuthErrorReportingScope.Key] != null) return block()

  val result = withContext(AuthErrorReportingScope) { block() }
  if (result is ClerkResult.Failure) emitAuthError(result)
  return result
}

internal suspend fun <T> withoutAuthErrorReporting(block: suspend () -> T): T =
  withContext(AuthErrorReportingScope) { block() }
