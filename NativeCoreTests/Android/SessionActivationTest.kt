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
class SessionActivationTest {
  @Test fun acceptedOrganization() = verify("org_next")
  @Test fun personalAccount() = verify("personal")
  @Test fun omittedOrganization() = verify("omitted")
  @Test fun rejectedOrganization() = verify("rejected")
  @Test fun unauthorizedOrganization() = verify("unauthorized")
  @Test fun pendingRejectionCannotOverwriteAcceptedSelection() = verify("race")

  private fun verify(scenario: String) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val base = PackagedFixtures(instrumentation.context)
      val rejectionStatus = if (scenario == "unauthorized") 401 else 403
      val rejectionCode = if (scenario == "unauthorized") "unauthorized_organization" else "not_a_member_in_organization"
      val client = base.fixtures.getValue("authenticatedClient").jsonObject.toMutableMap()
      val sessionJson = client.getValue("sessions").jsonArray[0].jsonObject.toMutableMap()
      val user = sessionJson.getValue("user").jsonObject.toMutableMap()
      user["organization_memberships"] = JsonArray(listOf("org_previous", "org_rejected", "org_next").map { id ->
        Json.parseToJsonElement("""{"object":"organization_membership","id":"om_$id","role":"org:admin","role_name":"Admin","permissions":["org:read"],"public_metadata":{},"created_at":1700000000000,"updated_at":1700000000000,"organization":{"object":"organization","id":"$id","name":"$id","slug":"$id","image_url":"","has_image":false,"public_metadata":{},"created_at":1700000000000,"updated_at":1700000000000}}""")
      })
      sessionJson["user"] = JsonObject(user)
      sessionJson["last_active_organization_id"] = JsonPrimitive("org_previous")
      client["sessions"] = JsonArray(listOf(JsonObject(sessionJson)))
      base.clientResponse = JsonObject(client)
      val now = System.currentTimeMillis() / 1000
      fun encoded(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
      val token = buildJsonObject { put("object", "token"); put("jwt", encoded("""{"alg":"none"}""") + "." + encoded("""{"sid":"sess_native","iat":$now,"exp":${now + 3600}}""") + ".fixture") }
      val began = CompletableDeferred<Unit>()
      val release = CompletableDeferred<Unit>()
      var lastBody: Map<String, String>? = null
      val host = object : NativeCapabilities {
        override val supported = base.supported
        override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
          if (capability != "http") return base.perform(capability, arguments)
          val args = arguments.jsonObject
          val path = URI(args.getValue("url").requireString()).path
          val body: JsonElement
          if (path.endsWith("/tokens")) body = token
          else if (path.endsWith("/touch")) {
            lastBody = args.getValue("body").requireString().split("&").associate { entry ->
              val pair = entry.split("=", limit = 2)
              URLDecoder.decode(pair[0], "UTF-8") to URLDecoder.decode(pair.getOrElse(1) { "" }, "UTF-8")
            }
            val requested = lastBody?.get("active_organization_id") ?: ""
            if (requested == "org_rejected") {
              began.complete(Unit)
              if (scenario == "race") release.await()
              return buildJsonObject { put("status", rejectionStatus); put("headers", buildJsonObject {}); put("body", """{"errors":[{"code":"$rejectionCode","message":"Unable to switch"}]}""") }
            }
            sessionJson["last_active_organization_id"] = if (requested.isEmpty()) JsonNull else JsonPrimitive(requested)
            client["sessions"] = JsonArray(listOf(JsonObject(sessionJson)))
            base.clientResponse = JsonObject(client)
            body = buildJsonObject { put("response", JsonObject(sessionJson)); put("client", JsonObject(client)) }
          } else return base.perform(capability, arguments)
          return buildJsonObject { put("status", 200); put("headers", buildJsonObject {}); put("body", body.toString()) }
        }
      }
      val key = "pk_test_" + Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
      val clerk = Clerk.connect(instrumentation.targetContext, ClerkConfiguration(key, "clerk-test://sso-callback"), host)
      fun selection(value: String) = MobileSetActiveParams(organization = Field.Value(MobileSetActiveParamsOrganization.Case1(value)))
      try {
        if (scenario == "race") {
          val rejected = async { runCatching { clerk.setActive(selection("org_rejected")) } }
          withTimeout(3000) { began.await() }
          val session = checkNotNull(clerk.session)
          session.checkAuthorization(CheckAuthorizationParams.Case1(SessionCheckAuthorizationIsAuthorizedParamsCase1("org:admin")))
          check(session.lastActiveOrganizationId == "org_previous")
          clerk.setActive(selection("org_next"))
          release.complete(Unit)
          val failure = rejected.await().exceptionOrNull()
          check(failure is CoreException && failure.status == 403)
          check(clerk.session?.lastActiveOrganizationId == "org_next" && clerk.organization?.id == "org_next")
        } else if (scenario == "rejected" || scenario == "unauthorized") {
          val failure = runCatching { clerk.setActive(selection("org_rejected")) }.exceptionOrNull()
          check(failure is CoreException && failure.status == rejectionStatus && failure.errors.first().code == rejectionCode)
          check(clerk.session?.lastActiveOrganizationId == "org_previous" && clerk.organization?.id == "org_previous")
        } else {
          val params = when (scenario) { "omitted" -> MobileSetActiveParams(); "personal" -> MobileSetActiveParams(organization = Field.Null); else -> selection(scenario) }
          clerk.setActive(params)
          val expected = when (scenario) { "omitted" -> "org_previous"; "personal" -> null; else -> scenario }
          check(clerk.session?.lastActiveOrganizationId == expected && clerk.organization?.id == expected)
          check(lastBody == mapOf("active_organization_id" to (expected ?: ""), "intent" to if (scenario == "omitted") "select_session" else "select_org"))
        }
      } finally { release.complete(Unit); clerk.close() }
    }
  }
}
