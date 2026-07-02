package com.clerk.api.billing

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingErrorMappingTest {

  @Test
  fun `already_subscribed error maps to AlreadySubscribedVia with the processor from meta`() {
    val failure =
      ClerkResult.apiFailure(
        ClerkErrorResponse(
          errors =
            listOf(
              Error(
                message = "Already subscribed",
                longMessage = "You already subscribe to this plan via another processor.",
                code = ERROR_CODE_ALREADY_SUBSCRIBED,
              )
            ),
          meta = buildJsonObject { put(META_KEY_ALREADY_SUBSCRIBED_VIA, "stripe") },
        )
      )

    val error = failure.toBillingError()

    assertTrue(error is BillingError.AlreadySubscribedVia)
    assertEquals("stripe", (error as BillingError.AlreadySubscribedVia).processor)
  }

  @Test
  fun `already_subscribed without meta maps to AlreadySubscribedVia with null processor`() {
    val failure =
      ClerkResult.apiFailure(
        ClerkErrorResponse(errors = listOf(Error(code = ERROR_CODE_ALREADY_SUBSCRIBED)))
      )

    val error = failure.toBillingError()

    assertTrue(error is BillingError.AlreadySubscribedVia)
    assertNull((error as BillingError.AlreadySubscribedVia).processor)
  }

  @Test
  fun `other api errors map to ServerRejected carrying the error response`() {
    val response =
      ClerkErrorResponse(
        errors = listOf(Error(message = "Invalid token", code = "store_purchase_invalid"))
      )
    val failure = ClerkResult.httpFailure(code = 422, error = response)

    val error = failure.toBillingError()

    assertTrue(error is BillingError.ServerRejected)
    val rejected = error as BillingError.ServerRejected
    assertEquals(response, rejected.error)
    assertEquals(422, rejected.code)
  }

  @Test
  fun `failures without a body map to ServerRejected with null error`() {
    val failure = ClerkResult.httpFailure<ClerkErrorResponse>(code = 500)

    val error = failure.toBillingError()

    assertTrue(error is BillingError.ServerRejected)
    assertNull((error as BillingError.ServerRejected).error)
    assertEquals(500, error.code)
  }
}
