package com.clerk.api.billing

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.billing.BillingManagedBy
import com.clerk.api.network.model.billing.BillingPlan
import com.clerk.api.network.model.billing.BillingPlanPeriod
import com.clerk.api.network.model.billing.BillingStore
import com.clerk.api.network.model.billing.BillingSubscriptionItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingModelSerializationTest {

  @Test
  fun `decodes plan with store products and features`() {
    val plan = ClerkApi.json.decodeFromString<BillingPlan>(PLAN_JSON)

    assertEquals("cplan_123", plan.id)
    assertEquals("Pro", plan.name)
    assertEquals("pro", plan.slug)
    assertEquals("The pro plan", plan.description)
    assertEquals(999L, plan.fee?.amount)
    assertEquals("USD", plan.fee?.currency)
    assertEquals(9990L, plan.annualFee?.amount)
    assertEquals(833L, plan.annualMonthlyFee?.amount)
    assertTrue(plan.isRecurring)
    assertTrue(plan.freeTrialEnabled)
    assertEquals(14, plan.freeTrialDays)
    assertEquals(listOf("widgets"), plan.features.map { it.slug })
    assertEquals(3, plan.storeProducts.size)

    val googleMonthly =
      plan.storeProducts.single {
        it.store == BillingStore.GOOGLE && it.purchaseOptionId == "monthly"
      }
    assertEquals("com.acme.pro", googleMonthly.productId)

    val apple = plan.storeProducts.single { it.store == BillingStore.APPLE }
    assertEquals("com.acme.pro.ios", apple.productId)
    assertNull(apple.purchaseOptionId)
  }

  @Test
  fun `decodes store-managed subscription item`() {
    val item =
      ClerkApi.json.decodeFromString<BillingSubscriptionItem>(
        """
        {
          "object": "commerce_subscription_item",
          "id": "sub_item_123",
          "status": "active",
          "plan_id": "cplan_123",
          "plan": null,
          "plan_period": "month",
          "managed_by": "google",
          "period_start": 1750000000000,
          "period_end": 1752678400000,
          "canceled_at": null,
          "past_due_at": null,
          "ended_at": null,
          "is_free_trial": false,
          "payer_id": "payer_123",
          "created_at": 1750000000000,
          "updated_at": 1750000000001
        }
        """
          .trimIndent()
      )

    assertEquals("sub_item_123", item.id)
    assertEquals(BillingSubscriptionItem.Status.ACTIVE, item.status)
    assertEquals("cplan_123", item.planId)
    assertNull(item.plan)
    assertEquals(BillingPlanPeriod.MONTH, item.planPeriod)
    assertEquals(BillingManagedBy.GOOGLE, item.managedBy)
    assertEquals(1750000000000L, item.periodStart)
    assertEquals(1752678400000L, item.periodEnd)
    assertNull(item.canceledAt)
    assertNull(item.endedAt)
    assertEquals("payer_123", item.payerId)
  }

  @Test
  fun `managed_by defaults to clerk when omitted`() {
    val item =
      ClerkApi.json.decodeFromString<BillingSubscriptionItem>(
        """
        {
          "id": "sub_item_456",
          "status": "canceled",
          "plan_period": "annual",
          "period_start": 1750000000000,
          "period_end": null,
          "canceled_at": 1751000000000
        }
        """
          .trimIndent()
      )

    assertEquals(BillingManagedBy.CLERK, item.managedBy)
    assertEquals(BillingSubscriptionItem.Status.CANCELED, item.status)
    assertEquals(BillingPlanPeriod.ANNUAL, item.planPeriod)
    assertEquals(1751000000000L, item.canceledAt)
    assertNull(item.periodEnd)
  }

  @Test
  fun `unknown enum values coerce to safe defaults`() {
    val item =
      ClerkApi.json.decodeFromString<BillingSubscriptionItem>(
        """
        {
          "id": "sub_item_789",
          "status": "some_future_status",
          "plan_period": "week",
          "period_start": 0
        }
        """
          .trimIndent()
      )

    assertEquals(BillingSubscriptionItem.Status.UNKNOWN, item.status)
    assertEquals(BillingPlanPeriod.UNKNOWN, item.planPeriod)
  }

  private companion object {
    val PLAN_JSON =
      """
      {
        "object": "commerce_plan",
        "id": "cplan_123",
        "name": "Pro",
        "slug": "pro",
        "description": "The pro plan",
        "fee": {
          "amount": 999,
          "amount_formatted": "9.99",
          "currency": "USD",
          "currency_symbol": "$"
        },
        "annual_fee": {
          "amount": 9990,
          "amount_formatted": "99.90",
          "currency": "USD",
          "currency_symbol": "$"
        },
        "annual_monthly_fee": {
          "amount": 833,
          "amount_formatted": "8.33",
          "currency": "USD",
          "currency_symbol": "$"
        },
        "is_default": false,
        "is_recurring": true,
        "publicly_visible": true,
        "has_base_fee": true,
        "for_payer_type": "user",
        "avatar_url": null,
        "free_trial_enabled": true,
        "free_trial_days": 14,
        "features": [
          {
            "object": "feature",
            "id": "feat_1",
            "name": "Widgets",
            "slug": "widgets",
            "description": null,
            "avatar_url": null
          }
        ],
        "store_products": [
          { "store": "google", "product_id": "com.acme.pro", "purchase_option_id": "monthly" },
          { "store": "google", "product_id": "com.acme.pro", "purchase_option_id": "annual" },
          { "store": "apple", "product_id": "com.acme.pro.ios", "purchase_option_id": null }
        ]
      }
      """
        .trimIndent()
  }
}
