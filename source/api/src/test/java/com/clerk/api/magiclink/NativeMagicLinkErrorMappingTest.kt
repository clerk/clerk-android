package com.clerk.api.magiclink

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NativeMagicLinkErrorMappingTest {
  @Test
  fun `maps known backend error codes deterministically`() {
    assertMapped("approval_token_consumed", NativeMagicLinkReason.APPROVAL_TOKEN_CONSUMED)
    assertMapped("approval_token_expired", NativeMagicLinkReason.APPROVAL_TOKEN_EXPIRED)
    assertMapped("approval_token_invalid", NativeMagicLinkReason.APPROVAL_TOKEN_INVALID)
    assertMapped("pkce_verification_failed", NativeMagicLinkReason.PKCE_VERIFICATION_FAILED)
    assertMapped("flow_not_approved", NativeMagicLinkReason.FLOW_NOT_APPROVED)
  }

  @Test
  fun `unknown backend errors preserve backend error code`() {
    val mapped =
      failure(code = "too_many_requests", longMessage = "Too many requests, retry later")
        .toNativeMagicLinkError(NativeMagicLinkReason.COMPLETE_FAILED)

    assertEquals("too_many_requests", mapped.reasonCode)
    assertEquals("Too many requests, retry later", mapped.message)
  }

  private fun assertMapped(apiCode: String, expected: NativeMagicLinkReason) {
    val mapped = failure(apiCode).toNativeMagicLinkError(NativeMagicLinkReason.COMPLETE_FAILED)
    assertEquals(expected.code, mapped.reasonCode)
  }

  private fun failure(
    code: String,
    longMessage: String? = null,
  ): ClerkResult.Failure<ClerkErrorResponse> {
    val errorResponse =
      ClerkErrorResponse(errors = listOf(Error(code = code, longMessage = longMessage)))
    return ClerkResult.apiFailure(errorResponse)
  }
}
