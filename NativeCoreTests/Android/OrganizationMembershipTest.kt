package com.clerk.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.net.URI
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrganizationMembershipTest {
  @Test fun deletionReceiptReturnsReadableMembership() = verifyDeletion(false)
  @Test fun fullDeletionResponseReturnsUpdatedMembership() = verifyDeletion(true)

  private fun verifyDeletion(fullResource: Boolean) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val base = PackagedFixtures(instrumentation.context)
      val membership = Json.parseToJsonElement("""
        {"object":"organization_membership","id":"orgmem_native","role":"org:member","role_name":"Member","permissions":[],"public_metadata":{"source":"before"},"created_at":1700000000000,"updated_at":1700000000000,
        "public_user_data":{"user_id":"user_native","first_name":"Test","last_name":"User","image_url":"","has_image":false,"identifier":"test@example.com"},
        "organization":{"object":"organization","id":"org_membership","name":"Membership Org","slug":"membership-org","image_url":"","has_image":false,"public_metadata":{},"created_at":1700000000000,"updated_at":1700000000000}}
      """).jsonObject
      var client = base.fixtures.getValue("authenticatedClient").jsonObject
      val session = client.getValue("sessions").jsonArray[0].jsonObject
      val user = JsonObject(session.getValue("user").jsonObject + ("organization_memberships" to JsonArray(listOf(membership))))
      client = JsonObject(client + ("sessions" to JsonArray(listOf(JsonObject(session + mapOf(
        "user" to user, "last_active_organization_id" to JsonPrimitive("org_membership")))))))
      base.clientResponse = client
      var deleted = false
      val host = object : NativeCapabilities {
        override val supported = base.supported
        override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
          if (capability != "http") return base.perform(capability, arguments)
          val args = arguments.jsonObject
          val url = URI(args.getValue("url").requireString())
          if (!url.path.contains("/memberships/")) return base.perform(capability, arguments)
          check(url.path == "/v1/organizations/org_membership/memberships/user_native")
          val query = url.rawQuery.split('&')
          val method = if ("_method=DELETE" in query) "DELETE" else args.getValue("method").requireString()
          check(method == "DELETE" && "_clerk_session_id=sess_native" in query)
          val currentSession = client.getValue("sessions").jsonArray[0].jsonObject
          val currentUser = JsonObject(currentSession.getValue("user").jsonObject + ("organization_memberships" to JsonArray(emptyList())))
          client = JsonObject(client + ("sessions" to JsonArray(listOf(JsonObject(currentSession + mapOf(
            "user" to currentUser, "last_active_organization_id" to JsonNull))))))
          base.clientResponse = client
          val returned = if (fullResource) JsonObject(membership + ("public_metadata" to buildJsonObject { put("source", "server") }))
            else buildJsonObject { put("object", "organization_membership"); put("id", "orgmem_native"); put("deleted", true) }
          deleted = true
          return buildJsonObject {
            put("status", 200); put("headers", buildJsonObject { put("authorization", "fixture-client-credential") })
            put("body", buildJsonObject { put("response", returned); put("client", client) }.toString())
          }
        }
      }
      val key = "pk_test_" + Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
      val clerk = Clerk.connect(instrumentation.targetContext, ClerkConfiguration(key, "clerk-test://sso-callback"), host)
      try {
        val currentUser = checkNotNull(clerk.user)
        val member = currentUser.organizationMemberships.single()
        check(clerk.organization?.id == "org_membership")
        val removed: OrganizationMembership = member.destroy()
        check(removed.id == "orgmem_native" && removed.role == "org:member")
        check(removed.publicMetadata["source"] == JsonPrimitive(if (fullResource) "server" else "before"))
        check(removed.organization.name == "Membership Org")
        check(currentUser.organizationMemberships.isEmpty() && clerk.organization == null && deleted)
        clerk.signIn.reset()
        check(clerk.loaded)
      } finally { clerk.close() }
    }
  }
}
