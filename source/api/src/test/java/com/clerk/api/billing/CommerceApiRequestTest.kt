package com.clerk.api.billing

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.CommerceApi
import com.clerk.api.network.serialization.ClerkApiResultCallAdapterFactory
import com.clerk.api.network.serialization.ClerkApiResultConverterFactory
import com.clerk.api.network.serialization.ClerkResult
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Verifies the requests [CommerceApi] constructs and that responses decode through the same
 * Retrofit stack [ClerkApi] configures, without hitting the network.
 */
class CommerceApiRequestTest {

  private val capturedRequests = mutableListOf<Request>()

  private fun api(responseJson: String): CommerceApi {
    val client =
      OkHttpClient.Builder()
        .addInterceptor { chain ->
          val request = chain.request()
          capturedRequests.add(request)
          Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(responseJson.toResponseBody("application/json; charset=utf-8".toMediaType()))
            .build()
        }
        .build()

    return Retrofit.Builder()
      .baseUrl("https://example.clerk.accounts.dev/v1/")
      .client(client)
      .addCallAdapterFactory(ClerkApiResultCallAdapterFactory)
      .addConverterFactory(ClerkApiResultConverterFactory)
      .addConverterFactory(
        ClerkApi.json.asConverterFactory("application/json; charset=utf-8".toMediaType())
      )
      .build()
      .create(CommerceApi::class.java)
  }

  @Test
  fun `store purchase posts form-encoded store and payload to the store_purchases endpoint`() =
    runTest {
      val result =
        api(STORE_PURCHASE_RESPONSE)
          .createStorePurchase(store = "google", payload = "play-purchase-token-123")

      val request = capturedRequests.single()
      assertEquals("POST", request.method)
      assertEquals("/v1/me/commerce/store_purchases", request.url.encodedPath)

      val buffer = Buffer()
      requireNotNull(request.body).writeTo(buffer)
      assertEquals("store=google&payload=play-purchase-token-123", buffer.readUtf8())

      assertTrue(result is ClerkResult.Success)
      assertEquals("sub_item_123", (result as ClerkResult.Success).value.id)
    }

  @Test
  fun `plans requests the commerce plans endpoint with the payer type filter`() = runTest {
    val result = api(PLANS_RESPONSE).plans(payerType = "user")

    val request = capturedRequests.single()
    assertEquals("GET", request.method)
    assertEquals("/v1/commerce/plans", request.url.encodedPath)
    assertEquals("user", request.url.queryParameter("payer_type"))

    assertTrue(result is ClerkResult.Success)
    val plans = (result as ClerkResult.Success).value.data
    assertEquals(listOf("cplan_123"), plans.map { it.id })
    assertEquals("com.acme.pro.monthly", plans.single().storeProducts.single().productId)
  }

  @Test
  fun `subscription items requests the user-scoped endpoint`() = runTest {
    val result = api(SUBSCRIPTION_ITEMS_RESPONSE).subscriptionItems()

    val request = capturedRequests.single()
    assertEquals("GET", request.method)
    assertEquals("/v1/me/commerce/subscription_items", request.url.encodedPath)

    assertTrue(result is ClerkResult.Success)
    assertEquals("sub_item_123", (result as ClerkResult.Success).value.data.single().id)
  }

  private companion object {
    val SUBSCRIPTION_ITEM_JSON =
      """
      {
        "object": "commerce_subscription_item",
        "id": "sub_item_123",
        "status": "active",
        "plan_id": "cplan_123",
        "plan_period": "month",
        "managed_by": "google",
        "period_start": 1750000000000,
        "period_end": 1752678400000,
        "canceled_at": null,
        "is_free_trial": false
      }
      """
        .trimIndent()

    val STORE_PURCHASE_RESPONSE = """{ "response": $SUBSCRIPTION_ITEM_JSON, "client": null }"""

    val PLANS_RESPONSE =
      """
      {
        "response": {
          "data": [
            {
              "object": "commerce_plan",
              "id": "cplan_123",
              "name": "Pro",
              "slug": "pro",
              "is_recurring": true,
              "store_products": [
                { "store": "google", "product_id": "com.acme.pro.monthly", "period": "month" }
              ]
            }
          ],
          "total_count": 1
        },
        "client": null
      }
      """
        .trimIndent()

    val SUBSCRIPTION_ITEMS_RESPONSE =
      """
      {
        "response": { "data": [$SUBSCRIPTION_ITEM_JSON], "total_count": 1 },
        "client": null
      }
      """
        .trimIndent()
  }
}
