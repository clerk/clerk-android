package com.clerk.api.billing

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.ClerkPaginatedResponse
import com.clerk.api.network.api.BillingApi
import com.clerk.api.network.serialization.ClerkApiResultCallAdapterFactory
import com.clerk.api.network.serialization.ClerkApiResultConverterFactory
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.getPaymentMethods as organizationGetPaymentMethods
import com.clerk.api.user.User
import com.clerk.api.user.getPaymentMethods as userGetPaymentMethods
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class BillingTest {

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `parity list includes every BillingNamespace GET and payer getPaymentMethods`() {
    val billingMethods = Billing::class.java.methods.map { it.name }.toSet()
    val expectedBillingGets =
      listOf(
        "getPaymentAttempts",
        "getPaymentAttempt",
        "getPlans",
        "getPlan",
        "getSubscription",
        "getStatements",
        "getStatement",
        "getCreditBalance",
        "getCreditHistory",
      )

    expectedBillingGets.forEach { name ->
      assertTrue("Billing is missing $name", billingMethods.contains(name))
    }
    assertSame(Billing, Clerk.billing)
    assertTrue(
      Class.forName("com.clerk.api.user.UserKt").methods.any { it.name == "getPaymentMethods" }
    )
    assertTrue(
      Class.forName("com.clerk.api.organizations.OrganizationKt").methods.any {
        it.name == "getPaymentMethods"
      }
    )
  }

  @Test
  fun `Billing has no startCheckout member`() {
    val names = Billing::class.java.methods.map { it.name }.toSet()
    assertFalse(names.contains("startCheckout"))
    assertFalse(names.contains("updateCheckout"))
    assertFalse(names.contains("initializePaymentMethod"))
    assertFalse(names.contains("addPaymentMethod"))
  }

  @Test
  fun `billingOffsetLimit converts initialPage and pageSize to offset and limit`() {
    assertEquals(BillingOffsetLimit(offset = 0, limit = 10), billingOffsetLimit())
    assertEquals(BillingOffsetLimit(offset = 0, limit = 10), billingOffsetLimit(1, 10))
    assertEquals(BillingOffsetLimit(offset = 5, limit = 5), billingOffsetLimit(2, 5))
    assertEquals(BillingOffsetLimit(offset = 20, limit = 10), billingOffsetLimit(3, 10))
  }

  @Test
  fun `forPayer organization maps to payer_type org otherwise user`() {
    assertEquals("org", ForPayerType.ORGANIZATION.toPayerTypeQueryValue())
    assertEquals("user", ForPayerType.USER.toPayerTypeQueryValue())
    assertEquals("user", null.toPayerTypeQueryValue())
  }

  @Test
  fun `getPlans maps organization forPayer orgId minSeats and pagination to FAPI query`() =
    runTest {
      val billingApi = mockk<BillingApi>()
      mockkObject(ClerkApi)
      every { ClerkApi.billing } returns billingApi
      coEvery {
        billingApi.getPlans(
          payerType = any(),
          orgId = any(),
          minSeats = any(),
          offset = any(),
          limit = any(),
          sessionId = any(),
        )
      } returns ClerkResult.success(ClerkPaginatedResponse(data = emptyList(), totalCount = 0))

      val result =
        Billing.getPlans(
          GetPlansParams(
            forPayer = ForPayerType.ORGANIZATION,
            orgId = "org_123",
            minSeats = 4,
            initialPage = 2,
            pageSize = 5,
          )
        )

      assertTrue(result is ClerkResult.Success)
      coVerify(exactly = 1) {
        billingApi.getPlans(
          payerType = "org",
          orgId = "org_123",
          minSeats = 4,
          offset = 5,
          limit = 5,
          sessionId = any(),
        )
      }
    }

  @Test
  fun `getPlans defaults forPayer to user and pagination to first page`() = runTest {
    val billingApi = mockk<BillingApi>()
    mockkObject(ClerkApi)
    every { ClerkApi.billing } returns billingApi
    coEvery {
      billingApi.getPlans(
        payerType = any(),
        orgId = any(),
        minSeats = any(),
        offset = any(),
        limit = any(),
        sessionId = any(),
      )
    } returns ClerkResult.success(ClerkPaginatedResponse(data = emptyList(), totalCount = 0))

    Billing.getPlans()

    coVerify(exactly = 1) {
      billingApi.getPlans(
        payerType = "user",
        orgId = null,
        minSeats = null,
        offset = 0,
        limit = 10,
        sessionId = any(),
      )
    }
  }

  @Test
  fun `user scoped billing paths use me prefix`() = runTest {
    val interceptor = CapturingInterceptor(piggybacked(EMPTY_PAGE))
    val api = billingApi(interceptor)

    runCatching { api.getUserSubscription() }
    assertEquals("/v1/me/billing/subscription", interceptor.path)

    runCatching { api.getUserStatements(limit = 10, offset = 0) }
    assertEquals("/v1/me/billing/statements", interceptor.path)

    runCatching { api.getUserStatement(id = "stmt_1") }
    assertEquals("/v1/me/billing/statements/stmt_1", interceptor.path)

    runCatching { api.getUserPaymentAttempts(limit = 10, offset = 0) }
    assertEquals("/v1/me/billing/payment_attempts", interceptor.path)

    runCatching { api.getUserPaymentAttempt(id = "pay_1") }
    assertEquals("/v1/me/billing/payment_attempts/pay_1", interceptor.path)

    runCatching { api.getUserCreditBalance() }
    assertEquals("/v1/me/billing/credits", interceptor.path)

    runCatching { api.getUserCreditHistory() }
    assertEquals("/v1/me/billing/credits/history", interceptor.path)

    runCatching { api.getUserPaymentMethods(limit = 10, offset = 0) }
    assertEquals("/v1/me/billing/payment_methods", interceptor.path)

    runCatching { api.getPlans(payerType = "user", limit = 10, offset = 0) }
    assertEquals("/v1/billing/plans", interceptor.path)

    runCatching { api.getPlan(id = "plan_1") }
    assertEquals("/v1/billing/plans/plan_1", interceptor.path)
  }

  @Test
  fun `organization scoped billing paths include organization id`() = runTest {
    val interceptor = CapturingInterceptor(piggybacked(EMPTY_PAGE))
    val api = billingApi(interceptor)
    val orgId = "org_123"

    runCatching { api.getOrganizationSubscription(organizationId = orgId) }
    assertEquals("/v1/organizations/org_123/billing/subscription", interceptor.path)

    runCatching { api.getOrganizationStatements(organizationId = orgId, limit = 10, offset = 0) }
    assertEquals("/v1/organizations/org_123/billing/statements", interceptor.path)

    runCatching { api.getOrganizationStatement(organizationId = orgId, id = "stmt_1") }
    assertEquals("/v1/organizations/org_123/billing/statements/stmt_1", interceptor.path)

    runCatching {
      api.getOrganizationPaymentAttempts(organizationId = orgId, limit = 10, offset = 0)
    }
    assertEquals("/v1/organizations/org_123/billing/payment_attempts", interceptor.path)

    runCatching { api.getOrganizationPaymentAttempt(organizationId = orgId, id = "pay_1") }
    assertEquals("/v1/organizations/org_123/billing/payment_attempts/pay_1", interceptor.path)

    runCatching { api.getOrganizationCreditBalance(organizationId = orgId) }
    assertEquals("/v1/organizations/org_123/billing/credits", interceptor.path)

    runCatching { api.getOrganizationCreditHistory(organizationId = orgId) }
    assertEquals("/v1/organizations/org_123/billing/credits/history", interceptor.path)

    runCatching {
      api.getOrganizationPaymentMethods(organizationId = orgId, limit = 10, offset = 0)
    }
    assertEquals("/v1/organizations/org_123/billing/payment_methods", interceptor.path)
  }

  @Test
  fun `Billing routes user vs org subscription to matching paths`() = runTest {
    val billingApi = mockk<BillingApi>()
    mockkObject(ClerkApi)
    every { ClerkApi.billing } returns billingApi
    coEvery { billingApi.getUserSubscription(any()) } returns
      ClerkResult.success(decode(SUBSCRIPTION_JSON))
    coEvery { billingApi.getOrganizationSubscription(any(), any()) } returns
      ClerkResult.success(decode(SUBSCRIPTION_JSON))

    Billing.getSubscription()
    coVerify { billingApi.getUserSubscription(sessionId = any()) }

    Billing.getSubscription(GetSubscriptionParams(orgId = "org_123"))
    coVerify {
      billingApi.getOrganizationSubscription(organizationId = "org_123", sessionId = any())
    }
  }

  @Test
  fun `User getPaymentMethods hits me payment methods with offset limit`() = runTest {
    val billingApi = mockk<BillingApi>()
    mockkObject(ClerkApi)
    every { ClerkApi.billing } returns billingApi
    coEvery { billingApi.getUserPaymentMethods(any(), any(), any()) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = emptyList(), totalCount = 0))

    user(id = "user_123")
      .userGetPaymentMethods(GetPaymentMethodsParams(initialPage = 3, pageSize = 4))

    coVerify { billingApi.getUserPaymentMethods(offset = 8, limit = 4, sessionId = any()) }
  }

  @Test
  fun `Organization getPaymentMethods uses the org id`() = runTest {
    val billingApi = mockk<BillingApi>()
    mockkObject(ClerkApi)
    every { ClerkApi.billing } returns billingApi
    coEvery { billingApi.getOrganizationPaymentMethods(any(), any(), any(), any()) } returns
      ClerkResult.success(ClerkPaginatedResponse(data = emptyList(), totalCount = 0))

    organization(id = "org_abc")
      .organizationGetPaymentMethods(GetPaymentMethodsParams(pageSize = 7))

    coVerify {
      billingApi.getOrganizationPaymentMethods(
        organizationId = "org_abc",
        offset = 0,
        limit = 7,
        sessionId = any(),
      )
    }
  }

  @Test
  fun `converter decodes raw getPlans body matching clerk-js`() = runTest {
    val interceptor = CapturingInterceptor("""{"data": [$PLAN_JSON], "total_count": 1}""")
    val api = billingApi(interceptor)

    val result =
      api.getPlans(payerType = "org", orgId = "org_123", minSeats = 2, limit = 5, offset = 5)

    assertTrue(result is ClerkResult.Success)
    val page = (result as ClerkResult.Success).value
    assertEquals(1, page.totalCount)
    assertEquals("plan_pro", page.data.single().id)
    assertEquals("org", interceptor.url.queryParameter("payer_type"))
    assertEquals("org_123", interceptor.url.queryParameter("org_id"))
    assertEquals("2", interceptor.url.queryParameter("min_seats"))
    assertEquals("5", interceptor.url.queryParameter("limit"))
    assertEquals("5", interceptor.url.queryParameter("offset"))
  }

  @Test
  fun `converter decodes raw getPlan and getPaymentAttempt bodies`() = runTest {
    val planApi = billingApi(CapturingInterceptor(PLAN_JSON))
    val plan = planApi.getPlan(id = "plan_pro")
    assertTrue(plan is ClerkResult.Success)
    assertEquals("plan_pro", (plan as ClerkResult.Success).value.id)

    val paymentApi = billingApi(CapturingInterceptor(PAYMENT_JSON))
    val payment = paymentApi.getUserPaymentAttempt(id = "pay_1")
    assertTrue(payment is ClerkResult.Success)
    assertEquals("pay_1", (payment as ClerkResult.Success).value.id)
  }

  @Test
  fun `converter unwraps piggybacked subscription and statements`() = runTest {
    val subscriptionApi = billingApi(CapturingInterceptor(piggybacked(SUBSCRIPTION_JSON)))
    val subscription = subscriptionApi.getUserSubscription()
    assertTrue(subscription is ClerkResult.Success)
    assertEquals("sub_1", (subscription as ClerkResult.Success).value.id)

    val statementsApi =
      billingApi(
        CapturingInterceptor(piggybacked("""{"data": [$STATEMENT_JSON], "total_count": 1}"""))
      )
    val statements = statementsApi.getUserStatements(limit = 10, offset = 0)
    assertTrue(statements is ClerkResult.Success)
    assertEquals("stmt_1", (statements as ClerkResult.Success).value.data.single().id)
  }

  @Test
  fun `decodes billing plan including Feature unit prices and available prices`() {
    val plan = decode<BillingPlan>(PLAN_JSON)

    assertEquals("plan_pro", plan.id)
    assertEquals("Pro", plan.name)
    assertEquals(1000L, plan.fee?.amount)
    assertEquals("10.00", plan.fee?.amountFormatted)
    assertEquals("USD", plan.fee?.currency)
    assertEquals("$", plan.fee?.currencySymbol)
    assertEquals(BillingPayerResourceType.ORG, plan.forPayerType)
    assertEquals("sso", plan.features.single().slug)
    assertEquals("SSO", plan.features.single().name)
    assertEquals("feature_sso", plan.features.single().id)
    assertEquals("seats", plan.unitPrices?.single()?.name)
    assertEquals(1, plan.unitPrices?.single()?.blockSize)
    assertEquals("tier_1", plan.unitPrices?.single()?.tiers?.single()?.id)
    assertEquals("price_annual", plan.availablePrices?.single()?.id)
    assertTrue(plan.availablePrices?.single()?.isDefault == true)
    assertEquals(14, plan.freeTrialDays)
    assertTrue(plan.freeTrialEnabled)
  }

  @Test
  fun `decodes billing plan unit price tier when id is missing`() {
    val json = PLAN_JSON.replace("\"id\": \"tier_1\",", "")
    val plan = decode<BillingPlan>(json)

    assertEquals("plan_pro", plan.id)
    assertEquals(null, plan.unitPrices?.single()?.tiers?.single()?.id)
    assertEquals(1, plan.unitPrices?.single()?.tiers?.single()?.startsAtBlock)
  }

  @Test
  fun `decodes billing subscription with seats credits and discount`() {
    val subscription = decode<BillingSubscription>(SUBSCRIPTION_JSON)
    val item = subscription.subscriptionItems.single()

    assertEquals("sub_1", subscription.id)
    assertEquals(BillingSubscriptionStatus.ACTIVE, subscription.status)
    assertEquals(1_700_000_000_000, subscription.createdAt)
    assertTrue(subscription.eligibleForFreeTrial)
    assertEquals("item_1", item.id)
    assertEquals("plan_pro", item.plan.id)
    assertEquals(BillingSubscriptionPlanPeriod.MONTH, item.planPeriod)
    assertEquals(3, item.seats?.quantity)
    assertEquals("seats", item.credits?.total?.currency)
    assertEquals("promo", item.appliedDiscount?.name)
    assertEquals(BillingDiscountSource.PROMO_CODE, item.appliedDiscount?.source)
    assertEquals(BillingDiscountEffect.PERCENTAGE, item.appliedDiscount?.effect)
    assertEquals(2, item.nextPayment?.perUnitTotals?.single()?.blockSize)
  }

  @Test
  fun `decodes billing payment with totals and payment method`() {
    val payment = decode<BillingPayment>(PAYMENT_JSON)

    assertEquals("pay_1", payment.id)
    assertEquals(BillingPaymentChargeType.RECURRING, payment.chargeType)
    assertEquals(BillingPaymentStatus.PAID, payment.status)
    assertEquals("pm_1", payment.paymentMethod?.id)
    assertEquals("4242", payment.paymentMethod?.last4)
    assertEquals(BillingPaymentMethodStatus.ACTIVE, payment.paymentMethod?.status)
    assertEquals(1000L, payment.totals?.subtotal?.amount)
    assertEquals("seats", payment.totals?.perUnitTotals?.single()?.name)
    assertEquals(100L, payment.totals?.discounts?.proration?.amount?.amount)
  }

  @Test
  fun `decodes unknown billing enum values without failing the resource`() {
    val json =
      PAYMENT_JSON.replace(
          "\"charge_type\": \"recurring\"",
          "\"charge_type\": \"price_transition\"",
        )
        .replace("\"status\": \"paid\"", "\"status\": \"settled\"")
    val payment = decode<BillingPayment>(json)

    assertEquals("pay_1", payment.id)
    assertEquals(BillingPaymentChargeType.PRICE_TRANSITION, payment.chargeType)
    assertEquals(BillingPaymentStatus.UNKNOWN, payment.status)
  }

  @Test
  fun `decodes billing payment when nested subscription item omits created at`() {
    val json =
      PAYMENT_JSON.replace("\"price_id\": \"price_1\",", "")
        .replace("\"created_at\": 1700000000000,", "")
        .replace("\"period_start\": 1700000000000,", "")
    val payment = decode<BillingPayment>(json)

    assertEquals("pay_1", payment.id)
    assertEquals("item_1", payment.subscriptionItem.id)
    assertEquals(null, payment.subscriptionItem.priceId)
    assertEquals(null, payment.subscriptionItem.createdAt)
    assertEquals(null, payment.subscriptionItem.periodStart)
    assertEquals(BillingPaymentStatus.PAID, payment.status)
  }

  @Test
  fun `decodes billing payment when nested subscription item sends epoch timestamps`() {
    val json =
      PAYMENT_JSON.replace("\"created_at\": 1700000000000,", "\"created_at\": 0,")
        .replace("\"period_start\": 1700000000000,", "\"period_start\": 0,")
    val payment = decode<BillingPayment>(json)

    assertEquals(null, payment.subscriptionItem.createdAt)
    assertEquals(null, payment.subscriptionItem.periodStart)
  }

  @Test
  fun `decodes fractional percent off and int64 money amounts`() {
    val json =
      MONEY.replace("\"amount\": 1000,", "\"amount\": 3000000000,")
        .let { money ->
          """
          {
            "amount": $money,
            "discount_id": "disc_1",
            "name": "Launch",
            "effect": "percentage",
            "percent_off": 12.5
          }
          """
        }
    val discount = decode<BillingAppliedDiscount>(json)

    assertEquals(3_000_000_000L, discount.amount.amount)
    assertEquals(12.5, discount.percentOff)
  }

  @Test
  fun `decodes billing statement with groups`() {
    val statement = decode<BillingStatement>(STATEMENT_JSON)

    assertEquals("stmt_1", statement.id)
    assertEquals(BillingStatementStatus.OPEN, statement.status)
    assertEquals(2500L, statement.totals.grandTotal.amount)
    assertEquals("pay_1", statement.groups.single().items.single().id)
    assertEquals("grp_1", statement.groups.single().id)
  }

  @Test
  fun `decodes billing payment method credit balance and ledger`() {
    val method = decode<BillingPaymentMethod>(PAYMENT_METHOD_JSON)
    val balance = decode<BillingCreditBalance>(CREDIT_BALANCE_JSON)
    val ledger = decode<BillingCreditLedger>(CREDIT_LEDGER_JSON)
    val feature = decode<Feature>(FEATURE_JSON)

    assertEquals("pm_1", method.id)
    assertEquals("card", method.paymentType)
    assertEquals("visa", method.cardType)
    assertEquals(500L, balance.balance?.amount)
    assertEquals("ledger_1", ledger.id)
    assertEquals("grant", ledger.sourceType)
    assertEquals("SSO", feature.name)
    assertEquals("sso", feature.slug)
  }

  @Test
  fun `decodes paginated plans without response wrapper`() {
    val page =
      ClerkApi.json.decodeFromString<ClerkPaginatedResponse<BillingPlan>>(
        """{"data": [$PLAN_JSON], "total_count": 1}"""
      )

    assertEquals(1, page.totalCount)
    assertEquals("feature_sso", page.data.single().features.single().id)
  }

  @Test
  fun `decodes plan from raw json matching clerk-js getPlan shape`() {
    val plan = ClerkApi.json.decodeFromString<BillingPlan>(PLAN_JSON)
    assertEquals("plan_pro", plan.id)
    assertNull(plan.annualFee)
  }

  private fun billingApi(interceptor: CapturingInterceptor): BillingApi {
    return Retrofit.Builder()
      .baseUrl("https://example.com/v1/")
      .client(OkHttpClient.Builder().addInterceptor(interceptor).build())
      .addCallAdapterFactory(ClerkApiResultCallAdapterFactory)
      .addConverterFactory(ClerkApiResultConverterFactory)
      .addConverterFactory(
        ClerkApi.json.asConverterFactory("application/json; charset=utf-8".toMediaType())
      )
      .build()
      .create(BillingApi::class.java)
  }

  private inline fun <reified T> decode(json: String): T = ClerkApi.json.decodeFromString(json)

  private class CapturingInterceptor(private val responseBody: String) : Interceptor {
    lateinit var path: String
    lateinit var url: HttpUrl

    override fun intercept(chain: Interceptor.Chain): Response {
      val request = chain.request()
      path = request.url.encodedPath
      url = request.url
      return Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(responseBody.toResponseBody("application/json".toMediaType()))
        .build()
    }
  }

  private companion object {
    fun piggybacked(inner: String): String = """{"response": $inner, "client": null}"""

    fun user(id: String): User {
      return User(
        id = id,
        imageUrl = "",
        hasImage = false,
        passkeys = emptyList(),
        passwordEnabled = false,
        phoneNumbers = emptyList(),
        totpEnabled = false,
        twoFactorEnabled = false,
        updatedAt = 0L,
      )
    }

    fun organization(id: String): Organization {
      return Organization(
        id = id,
        name = "Acme",
        slug = "acme",
        imageUrl = "https://example.com/acme.png",
        maxAllowedMemberships = 5,
        adminDeleteEnabled = true,
        createdAt = 1_000,
        updatedAt = 1_000,
        publicMetadata = JsonObject(emptyMap()),
      )
    }

    const val MONEY =
      """{"amount": 1000, "amount_formatted": "10.00", "currency": "USD", "currency_symbol": "$"}"""

    const val FEATURE_JSON =
      """
      {
        "id": "feature_sso",
        "name": "SSO",
        "description": "Single sign-on",
        "slug": "sso",
        "avatar_url": null
      }
      """

    const val PLAN_JSON =
      """
      {
        "id": "plan_pro",
        "name": "Pro",
        "fee": $MONEY,
        "annual_fee": null,
        "annual_monthly_fee": null,
        "description": "Pro plan",
        "is_default": false,
        "is_recurring": true,
        "has_base_fee": true,
        "for_payer_type": "org",
        "publicly_visible": true,
        "slug": "pro",
        "avatar_url": null,
        "features": [$FEATURE_JSON],
        "unit_prices": [
          {
            "name": "seats",
            "block_size": 1,
            "tiers": [
              {
                "id": "tier_1",
                "starts_at_block": 1,
                "ends_after_block": null,
                "fee_per_block": $MONEY
              }
            ]
          }
        ],
        "available_prices": [
          {
            "id": "price_annual",
            "fee": null,
            "annual_monthly_fee": $MONEY,
            "is_default": true,
            "unit_prices": []
          }
        ],
        "free_trial_days": 14,
        "free_trial_enabled": true
      }
      """

    const val DISCOUNT_REDEMPTION_JSON =
      """
      {
        "id": "red_1",
        "subscription_item_id": "item_1",
        "discount_id": "disc_1",
        "name": "promo",
        "source": "promo_code",
        "promo_code": "SAVE",
        "effect": "percentage",
        "percent_off": 10,
        "cycles_remaining": 2,
        "cycles_applied": 1,
        "status": "active",
        "redeemed_at": 1700000000000,
        "redeemed_by": "user_1"
      }
      """

    const val CREDITS_JSON =
      """
      {
        "proration": {
          "amount": $MONEY,
          "cycle_days_remaining": 10,
          "cycle_days_total": 30,
          "cycle_remaining_percent": 33.3
        },
        "payer": {
          "remaining_balance": $MONEY,
          "applied_amount": $MONEY
        },
        "total": {"amount": 0, "amount_formatted": "0.00", "currency": "seats", "currency_symbol": "$"}
      }
      """

    const val SUBSCRIPTION_ITEM_JSON =
      """
      {
        "id": "item_1",
        "plan": $PLAN_JSON,
        "plan_period": "month",
        "price_id": "price_1",
        "status": "active",
        "created_at": 1700000000000,
        "past_due_at": null,
        "period_start": 1700000000000,
        "period_end": 1702678400000,
        "canceled_at": null,
        "amount": $MONEY,
        "next_payment": {
          "amount": $MONEY,
          "date": 1702678400000,
          "per_unit_totals": [
            {
              "name": "seats",
              "block_size": 2,
              "tiers": [
                {
                  "quantity": 3,
                  "fee_per_block": $MONEY,
                  "total": $MONEY
                }
              ]
            }
          ]
        },
        "credit": {"amount": $MONEY},
        "credits": $CREDITS_JSON,
        "applied_discount": $DISCOUNT_REDEMPTION_JSON,
        "seats": {
          "quantity": 3,
          "tiers": [
            {
              "quantity": 3,
              "fee_per_block": $MONEY,
              "total": $MONEY
            }
          ]
        },
        "is_free_trial": false
      }
      """

    const val SUBSCRIPTION_JSON =
      """
      {
        "id": "sub_1",
        "status": "active",
        "created_at": 1700000000000,
        "active_at": 1700000000000,
        "updated_at": 1700000000000,
        "past_due_at": null,
        "next_payment": {
          "amount": $MONEY,
          "date": 1702678400000
        },
        "subscription_items": [$SUBSCRIPTION_ITEM_JSON],
        "eligible_for_free_trial": true
      }
      """

    const val PAYMENT_METHOD_JSON =
      """
      {
        "id": "pm_1",
        "last4": "4242",
        "payment_type": "card",
        "card_type": "visa",
        "is_default": true,
        "is_removable": true,
        "status": "active",
        "wallet_type": null,
        "expiry_year": 2030,
        "expiry_month": 12,
        "created_at": 1700000000000,
        "updated_at": 1700000000000
      }
      """

    const val PAYMENT_JSON =
      """
      {
        "id": "pay_1",
        "amount": $MONEY,
        "paid_at": 1700000000000,
        "failed_at": null,
        "updated_at": 1700000000000,
        "payment_method": $PAYMENT_METHOD_JSON,
        "subscription_item": $SUBSCRIPTION_ITEM_JSON,
        "charge_type": "recurring",
        "status": "paid",
        "totals": {
          "subtotal": $MONEY,
          "grand_total": $MONEY,
          "tax_total": {"amount": 0, "amount_formatted": "0.00", "currency": "USD", "currency_symbol": "$"},
          "base_fee": $MONEY,
          "per_unit_totals": [
            {
              "name": "seats",
              "block_size": 1,
              "tiers": [
                {
                  "quantity": 3,
                  "fee_per_block": $MONEY,
                  "total": $MONEY
                }
              ]
            }
          ],
          "discounts": {
            "proration": {
              "amount": {"amount": 100, "amount_formatted": "1.00", "currency": "USD", "currency_symbol": "$"},
              "cycle_days_passed": 5,
              "cycle_days_total": 30,
              "cycle_passed_percent": 16.6
            },
            "total": $MONEY
          }
        }
      }
      """

    const val STATEMENT_JSON =
      """
      {
        "id": "stmt_1",
        "status": "open",
        "timestamp": 1700000000000,
        "totals": {
          "subtotal": $MONEY,
          "grand_total": {"amount": 2500, "amount_formatted": "25.00", "currency": "USD", "currency_symbol": "$"},
          "tax_total": {"amount": 0, "amount_formatted": "0.00", "currency": "USD", "currency_symbol": "$"}
        },
        "groups": [
          {
            "id": "grp_1",
            "timestamp": 1700000000000,
            "items": [$PAYMENT_JSON]
          }
        ]
      }
      """

    const val CREDIT_BALANCE_JSON =
      """
      {
        "balance": {"amount": 500, "amount_formatted": "5.00", "currency": "USD", "currency_symbol": "$"}
      }
      """

    const val CREDIT_LEDGER_JSON =
      """
      {
        "id": "ledger_1",
        "amount": $MONEY,
        "source_type": "grant",
        "source_id": "grant_1",
        "created_at": 1700000000000
      }
      """

    const val EMPTY_PAGE = """{"data": [], "total_count": 0}"""
  }
}
