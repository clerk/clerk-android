package com.clerk.api.billing

import com.android.billingclient.api.BillingFlowParams.SubscriptionUpdateParams.ReplacementMode
import com.android.billingclient.api.Purchase
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlanChangeResolutionTest {

  @Test
  fun `attaches the active purchase as the replacement for a plan change`() {
    val active = purchase(Purchase.PurchaseState.PURCHASED, token = "old-purchase-token")

    assertEquals(
      SubscriptionReplacement("old-purchase-token", ReplacementMode.CHARGE_PRORATED_PRICE),
      resolveSubscriptionReplacement(listOf(active), ReplacementMode.CHARGE_PRORATED_PRICE),
    )
  }

  @Test
  fun `a fresh purchase without existing subscriptions attaches no replacement`() {
    assertNull(resolveSubscriptionReplacement(emptyList(), ReplacementMode.CHARGE_PRORATED_PRICE))
  }

  @Test
  fun `pending purchases are not replaced`() {
    val pending = purchase(Purchase.PurchaseState.PENDING, token = "pending-token")

    assertNull(
      resolveSubscriptionReplacement(listOf(pending), ReplacementMode.CHARGE_PRORATED_PRICE)
    )
  }

  @Test
  fun `skips pending purchases when picking the purchase to replace`() {
    val pending = purchase(Purchase.PurchaseState.PENDING, token = "pending-token")
    val active = purchase(Purchase.PurchaseState.PURCHASED, token = "active-token")

    assertEquals(
      SubscriptionReplacement("active-token", ReplacementMode.CHARGE_PRORATED_PRICE),
      resolveSubscriptionReplacement(
        listOf(pending, active),
        ReplacementMode.CHARGE_PRORATED_PRICE,
      ),
    )
  }

  @Test
  fun `carries the requested replacement mode`() {
    val active = purchase(Purchase.PurchaseState.PURCHASED, token = "old-purchase-token")

    assertEquals(
      SubscriptionReplacement("old-purchase-token", ReplacementMode.DEFERRED),
      resolveSubscriptionReplacement(listOf(active), ReplacementMode.DEFERRED),
    )
  }

  private fun purchase(state: Int, token: String): Purchase = mockk {
    every { purchaseState } returns state
    every { purchaseToken } returns token
  }
}
