package com.clerk.api.sso

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SSOExtensionsTest {

  @Test
  fun `signInToOAuthResult preserves unknown transport failure`() {
    val cause = IOException("Unable to resolve host api.clerk.com")
    val failure = ClerkResult.unknownFailure(cause)
    val result: ClerkResult<SignIn, ClerkErrorResponse> = failure

    val converted = result.signInToOAuthResult()

    assertSame(failure, converted)
    val convertedFailure = converted as ClerkResult.Failure
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, convertedFailure.errorType)
    assertSame(cause, convertedFailure.throwable)
  }

  @Test
  fun `signUpToOAuthResult preserves unknown transport failure`() {
    val cause = IOException("Unable to resolve host api.clerk.com")
    val failure = ClerkResult.unknownFailure(cause)
    val result: ClerkResult<SignUp, ClerkErrorResponse> = failure

    val converted = result.signUpToOAuthResult()

    assertSame(failure, converted)
    val convertedFailure = converted as ClerkResult.Failure
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, convertedFailure.errorType)
    assertSame(cause, convertedFailure.throwable)
  }
}
