package com.clerk.api.billing

import com.android.billingclient.api.ProductDetails
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubscriptionOfferResolutionTest {

  @Test
  fun `resolves the offer belonging to the requested base plan`() {
    val monthly = offer(basePlan = "monthly")
    val annual = offer(basePlan = "annual")
    val details = productDetails(monthly, annual)

    assertEquals(monthly, resolveSubscriptionOffer(details, "monthly"))
    assertEquals(annual, resolveSubscriptionOffer(details, "annual"))
  }

  @Test
  fun `prefers the base offer over discounted offers`() {
    val discounted = offer(basePlan = "monthly", offer = "intro")
    val base = offer(basePlan = "monthly")
    val details = productDetails(discounted, base)

    assertEquals(base, resolveSubscriptionOffer(details, "monthly"))
  }

  @Test
  fun `falls back to the first eligible offer when the base offer is absent`() {
    val first = offer(basePlan = "monthly", offer = "intro")
    val second = offer(basePlan = "monthly", offer = "winback")
    val details = productDetails(first, second)

    assertEquals(first, resolveSubscriptionOffer(details, "monthly"))
  }

  @Test
  fun `returns null when no offer belongs to the base plan`() {
    val details = productDetails(offer(basePlan = "annual"))

    assertNull(resolveSubscriptionOffer(details, "monthly"))
  }

  @Test
  fun `returns null when the product has no subscription offers`() {
    val details = mockk<ProductDetails> { every { subscriptionOfferDetails } returns null }

    assertNull(resolveSubscriptionOffer(details, "monthly"))
  }

  private fun productDetails(
    vararg offers: ProductDetails.SubscriptionOfferDetails
  ): ProductDetails = mockk { every { subscriptionOfferDetails } returns offers.toList() }

  private fun offer(
    basePlan: String,
    offer: String? = null,
  ): ProductDetails.SubscriptionOfferDetails = mockk {
    every { basePlanId } returns basePlan
    every { offerId } returns offer
  }
}
