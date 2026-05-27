package com.clerk.api.log

import android.net.Uri
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SafeUriLogTest {

  @Test
  fun `describe includes URI shape but redacts parameter values`() {
    val uri =
      Uri.parse(
        "https://example.com/v1/verify?token=secret-token&flow_id=flow_123&approval_token=approval_123"
      )

    val description = SafeUriLog.describe(uri)

    assertTrue(description.contains("scheme=https"))
    assertTrue(description.contains("host=example.com"))
    assertTrue(description.contains("query_keys=[approval_token, flow_id, token]"))
    assertFalse(description.contains("secret-token"))
    assertFalse(description.contains("approval_123"))
  }

  @Test
  fun `describe parses fragment keys`() {
    val uri = Uri.parse("clerk://callback#flow_id=flow_123&approval_token=approval_123")

    val description = SafeUriLog.describe(uri)

    assertTrue(description.contains("fragment_keys=[approval_token, flow_id]"))
    assertFalse(description.contains("flow_123"))
    assertFalse(description.contains("approval_123"))
  }
}
