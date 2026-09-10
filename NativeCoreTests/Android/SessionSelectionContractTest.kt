package com.clerk.api

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.net.URI
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionSelectionContractTest {
  @Test fun selectsExistingSessionWithoutFollowupRefresh() = verify("select")

  @Test fun explicitSelectionOverridesStaleClientSelection() = verify("stale-selection")

  @Test fun emptyTouchClientDoesNotRestoreSession() = verify("empty-touch")

  @Test fun laterEmptyRefreshClearsSelectedSession() = verify("empty-refresh")

  @Test fun forcedPersonalSelectionKeepsCurrentSession() = verify("forced-personal")

  @Test
  fun omittedOrganizationStillSelectsSessionWhenOrganizationsAreForced() = verify("forced-omitted")

  @Test
  fun returnedOrganizationStateOverridesRequestedOrganization() = verify("server-organization")

  @Test fun selectedSessionSignoutLeavesRemainingSessionUnselected() = verify("signout")

  private fun verify(scenario: String) = runBlocking {
    withTimeout(15000) {
      withContext(Dispatchers.Main.immediate) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = PackagedFixtures(instrumentation.context)
        val first = base.fixtures.getValue("session").jsonObject
        val second = JsonObject(first + ("id" to JsonPrimitive("sess_second")))
        val initial =
          JsonObject(
            base.fixtures.getValue("authenticatedClient").jsonObject +
              mapOf(
                "sessions" to JsonArray(listOf(first, second)),
                "last_active_session_id" to JsonPrimitive("sess_native"),
              )
          )
        base.clientResponse = initial
        if (scenario.startsWith("forced-")) {
          val environment = base.fixtures.getValue("environment").jsonObject
          base.environmentResponse =
            JsonObject(
              environment +
                ("organization_settings" to
                  JsonObject(
                    environment.getValue("organization_settings").jsonObject +
                      ("force_organization_selection" to JsonPrimitive(true))
                  ))
            )
        }
        val requests = mutableListOf<JsonObject>()
        val host =
          object : NativeCapabilities {
            override val supported = base.supported

            override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
              if (capability != "http") return base.perform(capability, arguments)
              val args = arguments.jsonObject
              requests += args
              val path = path(args)
              if (path.endsWith("/touch")) {
                val next =
                  if (scenario == "empty-touch") base.fixtures.getValue("client")
                  else
                    JsonObject(
                      initial +
                        ("last_active_session_id" to
                          JsonPrimitive(
                            if (scenario == "stale-selection") "sess_native" else "sess_second"
                          ))
                    )
                base.clientResponse = next
                return envelope(second, next)
              }
              if (path.endsWith("/sessions/sess_second") && scenario == "empty-refresh") {
                base.clientResponse = base.fixtures.getValue("client")
                return envelope(second, base.clientResponse)
              }
              if (path.endsWith("/sessions/sess_native/remove")) {
                val next =
                  JsonObject(
                    initial +
                      mapOf(
                        "sessions" to JsonArray(listOf(second)),
                        "last_active_session_id" to JsonPrimitive("sess_second"),
                      )
                  )
                base.clientResponse = next
                return envelope(JsonObject(first + ("status" to JsonPrimitive("removed"))), next)
              }
              if (path.endsWith("/tokens")) {
                val now = System.currentTimeMillis() / 1000
                fun encoded(value: String) =
                  Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
                val token = buildJsonObject {
                  put("object", "token")
                  put(
                    "jwt",
                    encoded("""{"alg":"none"}""") +
                      "." +
                      encoded("""{"sid":"sess_second","iat":$now,"exp":${now + 3600}}""") +
                      ".fixture",
                  )
                }
                return buildJsonObject {
                  put("status", 200)
                  putJsonObject("headers") {}
                  put("body", token.toString())
                }
              }
              return base.perform(capability, arguments)
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
          check(clerk.sessions.map { it.id } == listOf("sess_native", "sess_second"))
          val current = checkNotNull(clerk.session)
          val target = clerk.sessions.single { it.id == "sess_second" }
          val before = requests.size
          if (scenario == "signout") {
            clerk.signOut(MobileSignOutOptions(sessionId = "sess_native"))
            check(clerk.sessions.map { it.id } == listOf("sess_second"))
            check(clerk.session == null && clerk.user == null)
            check(
              requests.drop(before).single().let {
                path(it).endsWith("/sessions/sess_native/remove") &&
                  it["method"] == JsonPrimitive("POST")
              }
            )
          } else {
            clerk.setActive(
              MobileSetActiveParams(
                session = Field.Value(MobileSetActiveParamsSession.Case1("sess_second")),
                organization =
                  when (scenario) {
                    "forced-personal" -> Field.Null
                    "server-organization" ->
                      Field.Value(MobileSetActiveParamsOrganization.Case1("org_requested"))
                    else -> Field.Omitted
                  },
              )
            )
            if (scenario == "forced-personal") {
              check(clerk.session === current && requests.size == before)
            } else {
              val touch = requests.drop(before).single { path(it).endsWith("/touch") }
              check(path(touch).endsWith("/sessions/sess_second/touch"))
              val body =
                Uri.parse("https://fixture.invalid/?" + touch.getValue("body").requireString())
              check(
                body.getQueryParameter("intent") ==
                  (if (scenario == "server-organization") "select_org" else "select_session") &&
                  body.getQueryParameter("active_organization_id") ==
                    (if (scenario == "server-organization") "org_requested" else "")
              )
              check(base.clientReads == 1)
              if (scenario == "empty-touch") {
                check(clerk.sessions.isEmpty() && clerk.session == null && clerk.user == null)
              } else {
                check(clerk.session === target && clerk.user?.id == "user_native")
                check(target.lastActiveOrganizationId == null && clerk.organization == null)
                if (scenario == "empty-refresh") {
                  target.reload()
                  check(clerk.sessions.isEmpty() && clerk.session == null && clerk.user == null)
                }
              }
            }
          }
        } finally {
          clerk.close()
        }
      }
    }
  }

  private fun path(args: JsonObject) = URI(args.getValue("url").requireString()).path

  private fun envelope(resource: JsonElement, client: JsonElement?): JsonObject = buildJsonObject {
    put("status", 200)
    putJsonObject("headers") {}
    put(
      "body",
      buildJsonObject {
          put("response", resource)
          client?.let { put("client", it) }
        }
        .toString(),
    )
  }
}
