package com.clerk.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionAuthorizationTest {
  @Test fun freshAuthorization() = verifyAuthorization(listOf(0, 0), true)
  @Test fun staleAuthorization() = verifyAuthorization(listOf(10, 10), false)
  @Test fun noSecondFactor() = verifyAuthorization(listOf(0, -1), true)
  @Test fun noEnrolledFactors() = verifyAuthorization(listOf(-1, -1), false)
  @Test fun missingFactorAges() = verifyAuthorization(null, false)

  private fun verifyAuthorization(ages: List<Int>?, fresh: Boolean) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val host = PackagedFixtures(instrumentation.context)
      host.clientResponse = authorizationClient(host, ages)
      val key = "pk_test_" + Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
      val clerk = Clerk.connect(instrumentation.targetContext, ClerkConfiguration(key, "clerk-test://sso-callback"), host)
      try {
        val session = checkNotNull(clerk.session)
        val before = host.requests.size
        check(session.factorVerificationAge == ages?.let { SessionFactorVerificationAgeValue(it[0].toDouble(), it[1].toDouble()) })
        for (role in listOf("admin", "org:admin")) check(session.checkAuthorization(CheckAuthorizationParams.Case1(SessionCheckAuthorizationIsAuthorizedParamsCase1(role))))
        check(!session.checkAuthorization(CheckAuthorizationParams.Case1(SessionCheckAuthorizationIsAuthorizedParamsCase1("org:member"))))
        for (feature in listOf("reservations", "o:reservations", "org:reservations", "organization:reservations", "dashboard", "u:dashboard", "user:dashboard", "o:support", "u:support", "o:billing", "u:billing")) {
          check(session.checkAuthorization(CheckAuthorizationParams.Case3(SessionCheckAuthorizationIsAuthorizedParamsCase3(feature))))
        }
        check(!session.checkAuthorization(CheckAuthorizationParams.Case3(SessionCheckAuthorizationIsAuthorizedParamsCase3("lol:dashboard"))))
        check(session.checkAuthorization(CheckAuthorizationParams.Case4(SessionCheckAuthorizationIsAuthorizedParamsCase4("plus"))))
        check(!session.checkAuthorization(CheckAuthorizationParams.Case4(SessionCheckAuthorizationIsAuthorizedParamsCase4("free"))))
        check(!session.checkAuthorization(CheckAuthorizationParams.Case5(SessionCheckAuthorizationIsAuthorizedParamsCase5())))
        val permitted = CheckAuthorizationParams.Case2(SessionCheckAuthorizationIsAuthorizedParamsCase2("org:read"))
        check(session.checkAuthorization(permitted))
        check(!session.checkAuthorization(CheckAuthorizationParams.Case2(SessionCheckAuthorizationIsAuthorizedParamsCase2("org:delete"))))
        check(session.checkAuthorization(CheckAuthorizationParams.Case2(SessionCheckAuthorizationIsAuthorizedParamsCase2("org:read", ReverificationConfig.Case1("strict_mfa")))) == fresh)
        check(!session.checkAuthorization(CheckAuthorizationParams.Case3(SessionCheckAuthorizationIsAuthorizedParamsCase3("missing", ReverificationConfig.Case2("strict")))))
        val invalidConfig = ReverificationConfig.Case5(SessionCheckAuthorizationIsAuthorizedParamsCase1ReverificationCase5(SessionVerificationLevel.Unrecognized("nope"), 10.0))
        val invalid = runCatching {
          session.checkAuthorization(CheckAuthorizationParams.Case5(SessionCheckAuthorizationIsAuthorizedParamsCase5(invalidConfig)))
        }.exceptionOrNull()
        check(invalid is CoreException && invalid.code == "invalid_bridge_value")
        check(host.requests.size == before)
        val updatedClient = authorizationClient(host, ages, emptyList())
        host.clientResponse = updatedClient
        host.sessionReloadResponse = buildJsonObject {
          put("response", updatedClient.getValue("sessions").jsonArray[0]); put("client", updatedClient)
        }
        session.reload()
        check(clerk.session?.id == session.id && !session.checkAuthorization(permitted))
      } finally { clerk.close() }
    }
  }

  private fun authorizationClient(host: PackagedFixtures, ages: List<Int>?, permissions: List<String> = listOf("org:read")): JsonObject {
    val client = host.fixtures.getValue("authenticatedClient").jsonObject.toMutableMap()
    val session = client.getValue("sessions").jsonArray[0].jsonObject.toMutableMap()
    val user = session.getValue("user").jsonObject.toMutableMap()
    val member = Json.parseToJsonElement("""{"object":"organization_membership","id":"orgmem_auth","role":"admin","role_name":"Admin","permissions":[],"public_metadata":{},"created_at":1700000000000,"updated_at":1700000000000,"organization":{"object":"organization","id":"org_auth","name":"Auth Org","slug":"auth-org","image_url":"","has_image":false,"public_metadata":{},"created_at":1700000000000,"updated_at":1700000000000}}""").jsonObject.toMutableMap()
    member["permissions"] = JsonArray(permissions.map(::JsonPrimitive))
    user["organization_memberships"] = JsonArray(listOf(JsonObject(member)))
    session["user"] = JsonObject(user)
    session["last_active_organization_id"] = JsonPrimitive("org_auth")
    session["factor_verification_age"] = ages?.let { JsonArray(it.map(::JsonPrimitive)) } ?: JsonNull
    val now = System.currentTimeMillis() / 1000
    fun encoded(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
    val token = encoded("""{"alg":"none"}""") + "." + encoded("""{"sid":"sess_native","iat":$now,"exp":${now + 3600},"fea":"o:reservations,u:dashboard,ou:support,uo:billing","pla":"u:plus"}""") + ".fixture"
    session["last_active_token"] = buildJsonObject { put("object", "token"); put("jwt", token) }
    client["sessions"] = JsonArray(listOf(JsonObject(session)))
    return JsonObject(client)
  }
}
