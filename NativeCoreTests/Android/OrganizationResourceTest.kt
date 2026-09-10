package com.clerk.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.net.URI
import java.net.URLDecoder
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrganizationResourceTest {
  @Test fun logoReceiptRefreshesOrganization() = verify("receipt")
  @Test fun logoResourceRemainsSupported() = verify("resource")
  @Test fun logoRejectionPreservesOrganization() = verify("rejected")
  @Test fun failedRefreshPropagates() = verify("refresh-failed")
  @Test fun fetchedOrganizationPreservesItsOwnState() = verify("fetched")
  @Test fun emptyInvitationFiltersAreOmitted() = verify("filters")

  private fun verify(scenario: String) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val base = PackagedFixtures(instrumentation.context)
      val organization = Json.parseToJsonElement("""{"object":"organization","id":"org_resource","name":"Original organization","slug":"original","has_image":true,"image_url":"https://images.example/logo.png","public_metadata":{},"created_at":1700000000000,"updated_at":1700000000000}""").jsonObject
      val membership = buildJsonObject {
        put("object", "organization_membership")
        put("id", "orgmem_resource")
        put("role", "org:member")
        put("role_name", "Member")
        put("permissions", JsonArray(emptyList()))
        put("public_metadata", buildJsonObject {})
        put("organization", organization)
        put("created_at", 1700000000000)
        put("updated_at", 1700000000000)
      }
      val client = base.fixtures.getValue("authenticatedClient").jsonObject.toMutableMap()
      val session = client.getValue("sessions").jsonArray[0].jsonObject.toMutableMap()
      val user = session.getValue("user").jsonObject.toMutableMap()
      user["organization_memberships"] = JsonArray(listOf(membership))
      session["user"] = JsonObject(user)
      session["last_active_organization_id"] = JsonPrimitive("org_resource")
      client["sessions"] = JsonArray(listOf(JsonObject(session)))
      base.clientResponse = JsonObject(client)
      val requests = mutableListOf<JsonObject>()
      val host = object : NativeCapabilities {
        override val supported = base.supported
        override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
          if (capability != "http") return base.perform(capability, arguments)
          val args = arguments.jsonObject
          val url = URI(args.getValue("url").requireString())
          if (!url.path.startsWith("/v1/organizations/org_resource")) return base.perform(capability, arguments)
          requests += args
          val failed = scenario == "rejected" || (scenario == "refresh-failed" && !url.path.endsWith("/logo"))
          val payload = when {
            failed -> Json.parseToJsonElement("""{"errors":[{"code":"not_allowed_access","message":"Not allowed"}]}""")
            url.path.endsWith("/logo") && scenario != "resource" -> Json.parseToJsonElement("""{"response":{"object":"image","id":"img_resource","deleted":true}}""")
            url.path.endsWith("/invitations") -> Json.parseToJsonElement("""{"response":{"data":[],"total_count":0}}""")
            else -> buildJsonObject {
              put("response", buildJsonObject {
                organization.forEach { (key, value) -> put(key, value) }
                if (scenario == "fetched") put("name", if (args.getValue("method").requireString() == "GET") "Fetched organization" else "Updated organization")
                else { put("has_image", false); put("image_url", "") }
              })
            }
          }
          return buildJsonObject { put("status", if (failed) 403 else 200); put("headers", buildJsonObject {}); put("body", payload.toString()) }
        }
      }
      val key = "pk_test_" + Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
      val clerk = Clerk.connect(instrumentation.targetContext, ClerkConfiguration(key, "clerk-test://sso-callback"), host)
      try {
        val root = checkNotNull(clerk.organization)
        when (scenario) {
          "fetched" -> {
            val fetched = clerk.getOrganization(root.id)
            check(fetched !== root)
            check(fetched.name == "Fetched organization")
            check(root.name == "Original organization")
            val updated = fetched.update(UpdateOrganizationParams(name = "Updated organization"))
            check(updated.name == "Updated organization" && fetched.name == "Updated organization")
            check(root.name == "Original organization")
          }
          "filters" -> {
            val page = root.getInvitations(GetInvitationsParams(initialPage = 3.0, pageSize = 10.0, status = emptyList()))
            check(page.data.isEmpty() && page.totalCount == 0.0)
            val query = query(requests.last())
            check(query.none { it.first == "status" })
            check("offset" to "20" in query && "limit" to "10" in query)
          }
          "rejected", "refresh-failed" -> {
            val error = runCatching { root.setLogo(SetOrganizationLogoParams(file = null)) }.exceptionOrNull()
            check(error is CoreException && error.errors.firstOrNull()?.code == "not_allowed_access")
            check(root.name == "Original organization" && root.hasImage)
          }
          else -> {
            val result = root.setLogo(SetOrganizationLogoParams(file = null))
            check(result.id == root.id && result.name == "Original organization")
            check(!result.hasImage && result.imageUrl == "")
            check(requests.size == if (scenario == "receipt") 2 else 1)
            if (scenario == "receipt") check(!root.hasImage)
          }
        }
        for (request in requests) {
          val query = query(request)
          check("_clerk_session_id" to "sess_native" in query)
          if (URI(request.getValue("url").requireString()).path.endsWith("/logo")) {
            check(request.getValue("method").requireString() == "POST")
            check("_method" to "DELETE" in query)
          }
        }
      } finally { clerk.close() }
    }
  }

  private fun query(request: JsonObject): List<Pair<String, String>> =
    URI(request.getValue("url").requireString()).rawQuery.orEmpty().split('&').map {
      val parts = it.split('=', limit = 2)
      URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts.getOrElse(1) { "" }, "UTF-8")
    }
}
