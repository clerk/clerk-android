package com.clerk.api.billing

import com.android.billingclient.api.ProductDetails
import com.clerk.api.network.model.billing.BillingPlanPeriod
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubscriptionOfferResolutionTest {

  @Test
  fun `resolves the offer whose base phase matches the requested period`() {
    val monthly = offer(basePeriod = "P1M")
    val annual = offer(basePeriod = "P1Y")
    val details = productDetails(monthly, annual)

    assertEquals(monthly, resolveSubscriptionOffer(details, BillingPlanPeriod.MONTH))
    assertEquals(annual, resolveSubscriptionOffer(details, BillingPlanPeriod.ANNUAL))
  }

  @Test
  fun `falls back to the first offer when no base phase matches`() {
    val monthly = offer(basePeriod = "P1M")
    val details = productDetails(monthly)

    assertEquals(monthly, resolveSubscriptionOffer(details, BillingPlanPeriod.ANNUAL))
  }

  @Test
  fun `falls back to the first offer for an unknown period`() {
    val monthly = offer(basePeriod = "P1M")
    val details = productDetails(monthly)

    assertEquals(monthly, resolveSubscriptionOffer(details, BillingPlanPeriod.UNKNOWN))
  }

  @Test
  fun `returns null when the product has no subscription offers`() {
    val details = mockk<ProductDetails> { every { subscriptionOfferDetails } returns null }

    assertNull(resolveSubscriptionOffer(details, BillingPlanPeriod.MONTH))
  }

  @Test
  fun `plan periods map to Google Play ISO billing periods`() {
    assertEquals("P1M", BillingPlanPeriod.MONTH.isoBillingPeriod)
    assertEquals("P1Y", BillingPlanPeriod.ANNUAL.isoBillingPeriod)
    assertNull(BillingPlanPeriod.UNKNOWN.isoBillingPeriod)
  }

  private fun productDetails(
    vararg offers: ProductDetails.SubscriptionOfferDetails
  ): ProductDetails = mockk { every { subscriptionOfferDetails } returns offers.toList() }

  private fun offer(basePeriod: String): ProductDetails.SubscriptionOfferDetails {
    val basePhase =
      mockk<ProductDetails.PricingPhase> { every { billingPeriod } returns basePeriod }
    val phases =
      mockk<ProductDetails.PricingPhases> { every { pricingPhaseList } returns listOf(basePhase) }
    return mockk { every { pricingPhases } returns phases }
  }
}
