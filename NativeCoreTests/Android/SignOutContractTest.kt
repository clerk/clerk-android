package com.clerk.api

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignOutContractTest {
  @Test fun successfulSignOutUsesServerClientWithoutRefresh() = verify("success")

  @Test fun rejectedSignOutClearsSelectionButPreservesKnownServerSessions() = verify("rejected")

  @Test fun networkFailureIsNotReplayedOrReportedAsSuccess() = verify("network")

  @Test fun noSessionSignOutDoesNotRequestOrEraseClientCredential() = verify("empty")

  @Test fun blankBearerAuthorizationClearsStoredClientCredential() = verify("clear-credential")

  @Test fun scopedSignOutPreservesOtherSelectedSession() = verify("scoped")

  @Test fun scopedRejectionPreservesOtherSelectedSession() = verify("scoped-rejected")

  private fun verify(scenario: String) = runBlocking {
    withTimeout(15000) {
      withContext(Dispatchers.Main.immediate) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = PackagedFixtures(instrumentation.context)
        val scoped = scenario.startsWith("scoped")
        val rejected = scenario.endsWith("rejected")
        val failed = rejected || scenario == "network"
        val empty = scenario == "empty"
        val session = base.fixtures.getValue("session")
        val other = JsonObject(session.jsonObject + ("id" to JsonPrimitive("sess_other")))
        val client =
          JsonObject(
            base.fixtures.getValue("authenticatedClient").jsonObject +
              ("sessions" to JsonArray(if (scoped) listOf(session, other) else listOf(session)))
          )
        base.clientResponse = if (empty) base.fixtures.getValue("client") else client
        val requests = mutableListOf<JsonObject>()
        var recordOperations = 0
        var tokenRequests = 0
        val host =
          object : NativeCapabilities {
            override val supported = base.supported

            override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
              val args = arguments.jsonObject
              if (capability == "storage.write" || capability == "storage.remove")
                recordOperations++
              if (capability != "http") return base.perform(capability, arguments)
              requests += args
              val url = Uri.parse(args.getValue("url").requireString())
              if (url.path!!.contains("/tokens/")) tokenRequests++
              val path =
                if (scoped) "/v1/client/sessions/sess_other/remove" else "/v1/client/sessions"
              if (url.path != path) return base.perform(capability, arguments)
              check(args["method"] == JsonPrimitive("POST"))
              check(url.getQueryParameter("_method") == if (scoped) null else "DELETE")
              check(
                args.getValue("headers").jsonObject["authorization"] ==
                  JsonPrimitive("fixture-client-credential")
              )
              if (scenario == "network") throw IllegalStateException("Network unavailable")
              val response =
                if (rejected)
                  Json.parseToJsonElement(
                    """{"errors":[{"code":"service_failure","message":"Unavailable","long_message":"Try signing out again"}],"clerk_trace_id":"signout_fixture_trace"}"""
                  )
                else {
                  val next =
                    if (scoped) JsonObject(client + ("sessions" to JsonArray(listOf(session))))
                    else base.fixtures.getValue("client")
                  base.clientResponse = next
                  buildJsonObject {
                    put(
                      "response",
                      if (scoped) JsonObject(other + ("status" to JsonPrimitive("removed")))
                      else next,
                    )
                    put("client", next)
                  }
                }
              return buildJsonObject {
                put("status", if (rejected) 503 else 200)
                put(
                  "headers",
                  buildJsonObject {
                    if (!rejected)
                      put(
                        "authorization",
                        if (scenario == "clear-credential") "Bearer "
                        else "signed-out-client-credential",
                      )
                  },
                )
                put("body", response.toString())
              }
            }
          }
        val key =
          "pk_test_" +
            Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
        val clerk =
          Clerk.connect(
            instrumentation.targetContext,
            ClerkConfiguration(key, "clerk-test://sso-callback"),
            host,
          )
        try {
          val selected = clerk.session
          check(empty || selected?.id == "sess_native")
          val before = requests.size
          val writesBefore = recordOperations
          val result = runCatching {
            clerk.signOut(if (scoped) MobileSignOutOptions(sessionId = "sess_other") else null)
          }
          if (rejected) {
            val error = result.exceptionOrNull()
            check(error is CoreException && error.status == 503)
            check(
              error.errors.single().code == "service_failure" &&
                error.errors.single().longMessage == "Try signing out again"
            )
            check(error.clerkTraceId == "signout_fixture_trace")
          } else if (scenario == "network") {
            check(result.exceptionOrNull() is CoreException)
          } else result.getOrThrow()
          val signoutRequests = requests.drop(before)
          check(signoutRequests.size == if (empty) 0 else 1)
          check(signoutRequests.none { it["method"] == JsonPrimitive("GET") })
          check(
            base.credential ==
              when {
                scenario == "clear-credential" -> null
                failed || empty -> "fixture-client-credential"
                else -> "signed-out-client-credential"
              }
          )
          if (failed || empty) check(recordOperations == writesBefore)
          check(selected == null || selected.isInvalidated)
          if (scoped) {
            check(clerk.session?.id == "sess_native" && clerk.user?.id == "user_native")
            check(
              clerk.sessions.map { it.id } ==
                if (rejected) listOf("sess_native", "sess_other") else listOf("sess_native")
            )
          } else {
            check(clerk.session == null && clerk.user == null)
            check(clerk.sessions.size == if (failed) 1 else 0)
          }
          check(tokenRequests == 0)
        } finally {
          clerk.close()
        }
      }
    }
  }
}
