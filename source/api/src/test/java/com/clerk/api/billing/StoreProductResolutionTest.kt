package com.clerk.api.billing

import com.clerk.api.network.model.billing.BillingPlan
import com.clerk.api.network.model.billing.BillingStore
import com.clerk.api.network.model.billing.BillingStoreProduct
import com.clerk.api.network.serialization.ClerkResult
import org.junit.Assert.assertEquals
import org.junit.Test

class StoreProductResolutionTest {

  @Test
  fun `resolves the single Google mapping without selectors`() {
    val plan = plan(GOOGLE_MONTHLY, APPLE)

    assertEquals(GOOGLE_MONTHLY, resolveStoreProduct(plan).successValue())
  }

  @Test
  fun `fails with ProductNotMapped when the plan has no Google mapping`() {
    val plan = plan(APPLE)

    assertEquals(BillingError.ProductNotMapped(plan.id), resolveStoreProduct(plan).failureError())
  }

  @Test
  fun `fails with AmbiguousStoreProduct when several mappings exist and no selector is passed`() {
    val plan = plan(GOOGLE_MONTHLY, GOOGLE_ANNUAL)

    assertEquals(
      BillingError.AmbiguousStoreProduct(plan.id, listOf(GOOGLE_MONTHLY, GOOGLE_ANNUAL)),
      resolveStoreProduct(plan).failureError(),
    )
  }

  @Test
  fun `productId selects among several products`() {
    val plan = plan(GOOGLE_MONTHLY, GOOGLE_LEGACY)

    assertEquals(
      GOOGLE_LEGACY,
      resolveStoreProduct(plan, productId = "com.acme.pro.legacy").successValue(),
    )
  }

  @Test
  fun `purchaseOptionId selects among the base plans of one product`() {
    val plan = plan(GOOGLE_MONTHLY, GOOGLE_ANNUAL)

    assertEquals(
      GOOGLE_ANNUAL,
      resolveStoreProduct(plan, purchaseOptionId = "annual").successValue(),
    )
  }

  @Test
  fun `productId and purchaseOptionId together select the exact mapping`() {
    val plan = plan(GOOGLE_MONTHLY, GOOGLE_ANNUAL, GOOGLE_LEGACY)

    assertEquals(
      GOOGLE_ANNUAL,
      resolveStoreProduct(plan, productId = "com.acme.pro", purchaseOptionId = "annual")
        .successValue(),
    )
  }

  @Test
  fun `fails with AmbiguousStoreProduct when the productId matches several base plans`() {
    val plan = plan(GOOGLE_MONTHLY, GOOGLE_ANNUAL)

    assertEquals(
      BillingError.AmbiguousStoreProduct(plan.id, listOf(GOOGLE_MONTHLY, GOOGLE_ANNUAL)),
      resolveStoreProduct(plan, productId = "com.acme.pro").failureError(),
    )
  }

  @Test
  fun `fails with ProductNotMapped carrying the selectors when nothing matches`() {
    val plan = plan(GOOGLE_MONTHLY, GOOGLE_ANNUAL)

    assertEquals(
      BillingError.ProductNotMapped(plan.id, "com.acme.pro", "weekly"),
      resolveStoreProduct(plan, productId = "com.acme.pro", purchaseOptionId = "weekly")
        .failureError(),
    )
  }

  @Test
  fun `selectors never match mappings for other stores`() {
    val plan = plan(APPLE)

    assertEquals(
      BillingError.ProductNotMapped(plan.id, APPLE.productId, null),
      resolveStoreProduct(plan, productId = APPLE.productId).failureError(),
    )
  }

  private fun plan(vararg storeProducts: BillingStoreProduct): BillingPlan =
    BillingPlan(
      id = "cplan_123",
      name = "Pro",
      slug = "pro",
      storeProducts = storeProducts.toList(),
    )

  private fun ClerkResult<BillingStoreProduct, BillingError>.successValue(): BillingStoreProduct =
    (this as ClerkResult.Success).value

  private fun ClerkResult<BillingStoreProduct, BillingError>.failureError(): BillingError? =
    (this as ClerkResult.Failure).error

  private companion object {
    val GOOGLE_MONTHLY =
      BillingStoreProduct(
        store = BillingStore.GOOGLE,
        productId = "com.acme.pro",
        purchaseOptionId = "monthly",
      )
    val GOOGLE_ANNUAL =
      BillingStoreProduct(
        store = BillingStore.GOOGLE,
        productId = "com.acme.pro",
        purchaseOptionId = "annual",
      )
    val GOOGLE_LEGACY =
      BillingStoreProduct(
        store = BillingStore.GOOGLE,
        productId = "com.acme.pro.legacy",
        purchaseOptionId = "monthly",
      )
    val APPLE = BillingStoreProduct(store = BillingStore.APPLE, productId = "com.acme.pro.ios")
  }
}
