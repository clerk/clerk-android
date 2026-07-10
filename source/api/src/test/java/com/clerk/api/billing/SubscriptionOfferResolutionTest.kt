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
  fun `prefers the offer with the longest free trial`() {
    val base = offer(basePlan = "monthly")
    val weekTrial =
      offer(
        basePlan = "monthly",
        offer = "trial-week",
        phases = listOf(freePhase("P1W"), paidPhase()),
      )
    val monthTrial =
      offer(
        basePlan = "monthly",
        offer = "trial-month",
        phases = listOf(freePhase("P1M"), paidPhase()),
      )
    val details = productDetails(base, weekTrial, monthTrial)

    assertEquals(monthTrial, resolveSubscriptionOffer(details, "monthly"))
  }

  @Test
  fun `counts repeated free cycles towards the trial length`() {
    val singleMonth =
      offer(
        basePlan = "monthly",
        offer = "one-month",
        phases = listOf(freePhase("P1M"), paidPhase()),
      )
    val twoWeeksTwice =
      offer(
        basePlan = "monthly",
        offer = "two-weeks-x3",
        phases = listOf(freePhase("P2W", cycleCount = 3), paidPhase()),
      )
    val details = productDetails(singleMonth, twoWeeksTwice)

    assertEquals(twoWeeksTwice, resolveSubscriptionOffer(details, "monthly"))
  }

  @Test
  fun `prefers the cheapest introductory offer when no trial exists`() {
    val base = offer(basePlan = "monthly")
    val cheap =
      offer(
        basePlan = "monthly",
        offer = "intro-cheap",
        phases = listOf(paidPhase(priceMicros = 1_000_000L), paidPhase()),
      )
    val pricey =
      offer(
        basePlan = "monthly",
        offer = "intro-pricey",
        phases = listOf(paidPhase(priceMicros = 2_000_000L), paidPhase()),
      )
    val details = productDetails(base, pricey, cheap)

    assertEquals(cheap, resolveSubscriptionOffer(details, "monthly"))
  }

  @Test
  fun `ignores trials belonging to other base plans`() {
    val monthlyBase = offer(basePlan = "monthly")
    val annualTrial =
      offer(
        basePlan = "annual",
        offer = "trial",
        phases = listOf(freePhase("P1M"), paidPhase()),
      )
    val details = productDetails(monthlyBase, annualTrial)

    assertEquals(monthlyBase, resolveSubscriptionOffer(details, "monthly"))
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
    phases: List<ProductDetails.PricingPhase> = emptyList(),
  ): ProductDetails.SubscriptionOfferDetails = mockk {
    every { basePlanId } returns basePlan
    every { offerId } returns offer
    every { pricingPhases } returns mockk { every { pricingPhaseList } returns phases }
  }

  private fun freePhase(period: String, cycleCount: Int = 1): ProductDetails.PricingPhase =
    phase(priceMicros = 0L, period = period, cycleCount = cycleCount)

  private fun paidPhase(priceMicros: Long = 5_000_000L): ProductDetails.PricingPhase =
    phase(priceMicros = priceMicros, period = "P1M", cycleCount = 1)

  private fun phase(
    priceMicros: Long,
    period: String,
    cycleCount: Int,
  ): ProductDetails.PricingPhase = mockk {
    every { priceAmountMicros } returns priceMicros
    every { billingPeriod } returns period
    every { billingCycleCount } returns cycleCount
  }
}
