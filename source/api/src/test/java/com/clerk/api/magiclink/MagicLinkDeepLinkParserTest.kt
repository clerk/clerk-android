package com.clerk.api.magiclink

import android.net.Uri
import com.clerk.api.network.serialization.ClerkResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MagicLinkDeepLinkParserTest {
  @Test
  fun `parses flow_id and approval_token from query`() {
    val uri = Uri.parse("clerk://callback?flow_id=flow_123&approval_token=approval_123")

    val result = parseMagicLinkCallback(uri)

    assertTrue(result is ClerkResult.Success)
    val value = (result as ClerkResult.Success).value
    assertEquals("flow_123", value.flowId)
    assertEquals("approval_123", value.approvalToken)
  }

  @Test
  fun `parses flow_id and approval_token from fragment`() {
    val uri = Uri.parse("clerk://callback#flow_id=flow_123&approval_token=approval_123")

    val result = parseMagicLinkCallback(uri)

    assertTrue(result is ClerkResult.Success)
    val value = (result as ClerkResult.Success).value
    assertEquals("flow_123", value.flowId)
    assertEquals("approval_123", value.approvalToken)
  }

  @Test
  fun `missing flow_id returns deterministic reason code`() {
    val uri = Uri.parse("clerk://callback?approval_token=approval_123")

    val result = parseMagicLinkCallback(uri)

    assertTrue(result is ClerkResult.Failure)
    val reason = (result as ClerkResult.Failure).error?.reasonCode
    assertEquals(NativeMagicLinkReason.MISSING_FLOW_ID.code, reason)
  }
}
