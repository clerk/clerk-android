package com.clerk.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.net.URI
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionRevocationTest {
  @Test fun revokeSelectedSession() = verifyRevocation("current")
  @Test fun revokeOtherSession() = verifyRevocation("other")
  @Test fun rejectedRevocationPreservesState() = verifyRevocation("rejected")
  @Test fun setupMfaTask() = verifyTask("setup-mfa", SessionTaskKey.SetupMfa)
  @Test fun resetPasswordTask() = verifyTask("reset-password", SessionTaskKey.ResetPassword)
  @Test fun futureTask() = verifyTask("another-task", SessionTaskKey.Unrecognized("another-task"))

  private fun verifyRevocation(scenario: String) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val base = PackagedFixtures(instrumentation.context)
      base.clientResponse = base.fixtures.getValue("authenticatedClient")
      val targetId = if (scenario == "other") "sess_other" else "sess_native"
      var revocations = 0
      val host = object : NativeCapabilities {
        override val supported = base.supported
        override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
          if (capability != "http") return base.perform(capability, arguments)
          val args = arguments.jsonObject
          val path = URI(args.getValue("url").requireString()).path
          if (!path.endsWith("/me/sessions/active") && !path.endsWith("/revoke")) return base.perform(capability, arguments)
          val client = base.fixtures.getValue("authenticatedClient").jsonObject.toMutableMap()
          val session = client.getValue("sessions").jsonArray[0].jsonObject.toMutableMap()
          val listed = session.toMutableMap()
          listed["id"] = JsonPrimitive(targetId); listed["user"] = JsonNull
          listed["latest_activity"] = buildJsonObject { put("id", "act_fixture"); put("browser_name", "Safari"); put("is_mobile", true) }
          val body: JsonElement
          if (path.endsWith("/active")) {
            check(args.getValue("method").requireString() == "GET")
            body = JsonArray(listOf(JsonObject(listed)))
          } else {
            revocations++
            check(path == "/v1/me/sessions/$targetId/revoke")
            check(args.getValue("method").requireString() == "POST" && args.getValue("body").requireString() == "")
            if (scenario == "rejected") return buildJsonObject { put("status", 403); put("headers", buildJsonObject {}); put("body", """{"errors":[{"code":"session_revoke_denied","message":"Cannot revoke"}]}""") }
            listed["status"] = JsonPrimitive("revoked")
            if (scenario == "current") {
              session["status"] = JsonPrimitive("revoked")
              client["sessions"] = JsonArray(listOf(JsonObject(session)))
            }
            body = buildJsonObject { put("response", JsonObject(listed)); put("client", JsonObject(client)) }
          }
          return buildJsonObject { put("status", 200); put("headers", buildJsonObject {}); put("body", body.toString()) }
        }
      }
      val key = "pk_test_" + Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
      val clerk = Clerk.connect(instrumentation.targetContext, ClerkConfiguration(key, "clerk-test://sso-callback"), host)
      try {
        val selected = checkNotNull(clerk.session)
        val listed = checkNotNull(clerk.user).getSessions().first()
        check(listed.id == targetId && listed.latestActivity.browserName == "Safari" && listed.latestActivity.isMobile == true)
        if (scenario == "rejected") {
          val failure = runCatching { listed.revoke() }.exceptionOrNull()
          check(failure is CoreException && failure.status == 403 && failure.errors.first().code == "session_revoke_denied")
          check(listed.status == "active")
        } else {
          val result = listed.revoke()
          check(result.status == "revoked" && result.id == targetId)
        }
        check(revocations == 1)
        if (scenario == "current") {
          check(clerk.session == null && selected.isInvalidated)
          val failure = runCatching { selected.getToken() }.exceptionOrNull()
          check(failure is CoreException && failure.code == "stale_resource")
        } else check(clerk.session?.id == selected.id)
      } finally { clerk.close() }
    }
  }

  private fun verifyTask(key: String, expected: SessionTaskKey) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val host = PackagedFixtures(instrumentation.context)
      val client = host.fixtures.getValue("authenticatedClient").jsonObject.toMutableMap()
      val session = client.getValue("sessions").jsonArray[0].jsonObject.toMutableMap()
      session["status"] = JsonPrimitive("pending")
      session["tasks"] = buildJsonArray { add(buildJsonObject { put("key", key) }) }
      client["sessions"] = JsonArray(listOf(JsonObject(session)))
      host.clientResponse = JsonObject(client)
      val publishableKey = "pk_test_" + Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
      val clerk = Clerk.connect(instrumentation.targetContext, ClerkConfiguration(publishableKey, "clerk-test://sso-callback"), host)
      try {
        val pending = checkNotNull(clerk.session)
        check(pending.status == SessionStatus.Pending)
        check(pending.tasks?.map { it.key } == listOf(expected) && pending.currentTask?.key == expected)
      } finally { clerk.close() }
    }
  }
}
