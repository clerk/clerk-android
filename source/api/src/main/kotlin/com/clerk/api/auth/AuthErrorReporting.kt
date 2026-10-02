package com.clerk.api.auth

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext

/**
 * Marks a coroutine as running inside an auth entry point that reports its own failure.
 *
 * Public auth entry points nest: `Clerk.auth.signUpWithGoogleOneTap()` calls
 * `SignIn.authenticateWithGoogleOneTap()`, which calls `SignUp.create()`. Every one of them reports
 * failures to [Auth.events], so only the outermost call may emit, or one failure would surface as
 * several [AuthEvent.Error] events.
 */
private object AuthErrorReportingScope : AbstractCoroutineContextElement(Key) {
  object Key : CoroutineContext.Key<AuthErrorReportingScope>
}

/**
 * Runs an auth operation and emits [AuthEvent.Error] on this [Auth] if it fails.
 *
 * Every public sign-in and sign-up entry point, on [Auth] or on [com.clerk.api.signin.SignIn] and
 * [com.clerk.api.signup.SignUp], routes through this so both API layers report failures the same
 * way. Calls nested inside another reporting call do not emit; the outermost call does.
 */
internal suspend fun <T : Any> Auth.reportingFailures(
  block: suspend () -> ClerkResult<T, ClerkErrorResponse>
): ClerkResult<T, ClerkErrorResponse> {
  if (currentCoroutineContext()[AuthErrorReportingScope.Key] != null) return block()

  val result = withContext(AuthErrorReportingScope) { block() }
  if (result is ClerkResult.Failure) emitAuthError(result)
  return result
}

/**
 * Runs [block] without emitting [AuthEvent.Error] for any auth calls it makes.
 *
 * Use this for SDK-internal work whose result is reported by another call, such as an OAuth
 * callback whose outcome is returned to the suspended `authenticateWithRedirect` caller.
 */
internal suspend fun <T> withoutAuthErrorReporting(block: suspend () -> T): T =
  withContext(AuthErrorReportingScope) { block() }
