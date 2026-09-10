package com.clerk.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.net.URI
import java.time.Instant
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaymentMethodTest {
  @Test fun userCollectionPreservesPaymentMethodFields() = verifyCollection(false)
  @Test fun organizationCollectionPreservesPaymentMethodFields() = verifyCollection(true)

  private fun verifyCollection(organization: Boolean) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val base = PackagedFixtures(instrumentation.context)
      val membership = Json.parseToJsonElement("""{"object":"organization_membership","id":"orgmem_native","role":"org:member","role_name":"Member","permissions":[],"public_metadata":{},"created_at":1700000000000,"updated_at":1700000000000,"public_user_data":{"user_id":"user_native","identifier":"test@example.com","image_url":"","has_image":false},"organization":{"object":"organization","id":"org_payment","name":"Payment Org","slug":"payment-org","image_url":"","has_image":false,"public_metadata":{},"created_at":1700000000000,"updated_at":1700000000000}}""")
      val client = base.fixtures.getValue("authenticatedClient").jsonObject
      val session = client.getValue("sessions").jsonArray[0].jsonObject
      val user = JsonObject(session.getValue("user").jsonObject + ("organization_memberships" to JsonArray(listOf(membership))))
      base.clientResponse = JsonObject(client + ("sessions" to JsonArray(listOf(JsonObject(session + mapOf(
        "user" to user, "last_active_organization_id" to JsonPrimitive("org_payment")))))))
      var requested = false
      val host = object : NativeCapabilities {
        override val supported = base.supported
        override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
          if (capability != "http") return base.perform(capability, arguments)
          val args = arguments.jsonObject
          val url = URI(args.getValue("url").requireString())
          if (!url.path.endsWith("/billing/payment_methods")) return base.perform(capability, arguments)
          check(url.path == if (organization) "/v1/organizations/org_payment/billing/payment_methods" else "/v1/me/billing/payment_methods")
          check(args.getValue("method").requireString() == "GET")
          val query = url.rawQuery.split('&')
          check(listOf("limit=10", "offset=20", "_clerk_session_id=sess_native").all { it in query })
          requested = true
          return buildJsonObject {
            put("status", 200); put("headers", buildJsonObject {})
            put("body", """{"response":{"total_count":21,"data":[{"object":"commerce_payment_method","id":"pm_1","last4":"4242","payment_type":"card","card_type":"visa","is_default":true,"is_removable":true,"status":"active","wallet_type":null,"expiry_year":2030,"expiry_month":12,"created_at":1700000000000,"updated_at":1700000001000},{"object":"commerce_payment_method","id":"pm_2","last4":null,"card_type":null,"status":"future_status","expiry_month":null}]}}""")
          }
        }
      }
      val key = "pk_test_" + Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
      val clerk = Clerk.connect(instrumentation.targetContext, ClerkConfiguration(key, "clerk-test://sso-callback"), host)
      try {
        val params = GetPaymentMethodsParams(initialPage = 3.0, pageSize = 10.0)
        val result = if (organization) checkNotNull(clerk.organization).getPaymentMethods(params) else checkNotNull(clerk.user).getPaymentMethods(params)
        check(result.totalCount == 21.0 && result.data.size == 2)
        val method = result.data.first()
        check(method.id == "pm_1" && method.last4 == "4242" && method.cardType == "visa")
        check(method.paymentType == "card" && method.status.rawValue == "active")
        check(method.isDefault == true && method.isRemovable == true)
        check(method.walletType == Field.Null)
        check(method.expiryMonth == Field.Value(12.0) && method.expiryYear == Field.Value(2030.0))
        check(method.createdAt == Field.Value(Instant.ofEpochMilli(1700000000000)))
        check(method.updatedAt == Field.Value(Instant.ofEpochMilli(1700000001000)))
        check(result.data[1].status.rawValue == "future_status" && result.data[1].last4 == null)
        check(result.data[1].expiryMonth == Field.Null)
        check(requested)
      } finally { clerk.close() }
    }
  }
}
