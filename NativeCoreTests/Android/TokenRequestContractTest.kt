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
class TokenRequestContractTest {
  @Test fun rejectedTokenKeepsSession() = rejectedToken(403, "token_denied", false)

  @Test fun unauthorizedTokenRefreshesCurrentSession() = rejectedToken(401, "not_authorized", false)

  @Test fun revokedTokenUsesRefreshedClientState() = rejectedToken(401, "session_revoked", false)

  @Test
  fun invalidAuthenticationUsesRefreshedClientState() =
    rejectedToken(401, "authentication_invalid", false)

  @Test
  fun revokedTokenClearsSessionWhenRecoveryRemovesIt() = rejectedToken(401, "session_revoked", true)

  @Test
  fun exhaustedNetworkFailureThrowsAndLaterCallRecovers() {
    var rejecting = true
    var calls = 0
    val recovered = token("network_recovered")
    runFixture(
      fastTimers = { rejecting },
      http = { _, args ->
        if (!path(args).endsWith("/tokens/firebase")) null
        else {
          calls++
          if (rejecting) throw IllegalStateException("Fixture network unavailable")
          success(recovered)
        }
      },
    ) { clerk, _ ->
      val session = checkNotNull(clerk.session)
      val error =
        runCatching { session.getToken(GetTokenOptions(template = "firebase")) }.exceptionOrNull()
      check(error is CoreException)
      check(calls > 1 && calls <= 36)
      check(clerk.session?.handle == session.handle)
      val failedCalls = calls
      rejecting = false
      check(session.getToken(GetTokenOptions(template = "firebase")) == recovered)
      check(
        session.getToken(GetTokenOptions(template = "firebase")) == recovered &&
          calls == failedCalls + 1
      )
    }
  }

  @Test fun closingOwnerRejectsForcedTokenAndIgnoresLateCredential() = closingOwner(true)

  @Test fun closingOwnerReleasesSharedTokenWaiters() = closingOwner(false)

  private fun closingOwner(forced: Boolean) {
    val started = CompletableDeferred<Unit>()
    val release = CompletableDeferred<Unit>()
    val replied = CompletableDeferred<Unit>()
    var calls = 0
    runFixture(
      http = { _, args ->
        if (!path(args).endsWith("/tokens/firebase")) null
        else {
          calls++
          started.complete(Unit)
          withContext(NonCancellable) { release.await() }
          replied.complete(Unit)
          JsonObject(
            success(token("late")) +
              ("headers" to
                buildJsonObject {
                  put("authorization", "late-credential-after-close")
                })
          )
        }
      }
    ) { clerk, base ->
      try {
        val session = checkNotNull(clerk.session)
        val credential = base.credential
        val options = GetTokenOptions(template = "firebase", skipCache = forced)
        val first = async { runCatching { session.getToken(options) } }
        started.await()
        val second = if (forced) null else async { runCatching { session.getToken(options) } }
        drainEarlierInvocations(session)
        check(calls == 1)
        clerk.close()
        for (call in listOfNotNull(first, second)) {
          val error = call.await().exceptionOrNull()
          check(error is CoreException && error.code == "runtime_disposed")
        }
        release.complete(Unit)
        replied.await()
        delay(20)
        check(base.credential == credential && calls == 1)
        val closed = runCatching { session.getToken() }.exceptionOrNull()
        check(closed is CoreException && closed.code == "stale_resource")
      } finally {
        release.complete(Unit)
      }
    }
  }

  private fun rejectedToken(status: Int, code: String, removed: Boolean) {
    var calls = 0
    val recovered = token("recovered")
    runFixture(
      http = { base, args ->
        if (!path(args).endsWith("/tokens/firebase")) null
        else if (++calls == 1) {
          if (removed) base.clientResponse = base.fixtures.getValue("client")
          failure(status, code)
        } else success(recovered)
      }
    ) { clerk, base ->
      val session = checkNotNull(clerk.session)
      val error =
        runCatching { session.getToken(GetTokenOptions(template = "firebase")) }.exceptionOrNull()
      check(error is CoreException && error.status == status && error.errors.single().code == code)
      check(error.clerkTraceId == "trace_token_fixture")
      check(calls == 1)
      check(base.clientReads == if (status == 401) 2 else 1)
      if (removed) {
        check(clerk.session == null && clerk.user == null)
        val stale = runCatching { session.getToken() }.exceptionOrNull()
        check(stale is CoreException && stale.code == "stale_resource")
        check(calls == 1)
      } else {
        check(clerk.session?.handle == session.handle)
        check(session.getToken(GetTokenOptions(template = "firebase")) == recovered)
        check(calls == 2)
      }
    }
  }

  @Test
  fun cacheSeparatesSessionsTemplatesAndOrganizations() {
    val requests = mutableListOf<JsonObject>()
    val tokens = mutableListOf<String>()
    runFixture(
      configure = { base ->
        val client = base.clientResponse!!.jsonObject
        val session = client.getValue("sessions").jsonArray[0].jsonObject
        base.clientResponse =
          JsonObject(
            client +
              ("sessions" to
                JsonArray(
                  listOf(
                    session,
                    JsonObject(session + ("id" to JsonPrimitive("sess_other"))),
                  )
                ))
          )
      },
      http = { _, args ->
        if (!path(args).contains("/tokens")) null
        else {
          requests += args
          val sid = path(args).substringAfter("/sessions/").substringBefore('/')
          val jwt = token("request_${requests.size}", sid, requests.size)
          tokens += jwt
          success(jwt)
        }
      },
    ) { clerk, _ ->
      val selected = checkNotNull(clerk.session)
      val other = clerk.sessions.single { it.id == "sess_other" }
      val options =
        listOf(
          GetTokenOptions(),
          GetTokenOptions(template = "firebase"),
          GetTokenOptions(template = "reports"),
          GetTokenOptions(organizationId = "org_explicit"),
        )
      val values = options.map { selected.getToken(it) }
      val otherValue = other.getToken()
      check(requests.size == 5 && values.toSet().size == 4 && otherValue !in values)
      options.forEachIndexed { index, option -> check(selected.getToken(option) == values[index]) }
      check(other.getToken() == otherValue && requests.size == 5)
      check(
        requests.map(::path) ==
          listOf(
            "/v1/client/sessions/sess_native/tokens",
            "/v1/client/sessions/sess_native/tokens/firebase",
            "/v1/client/sessions/sess_native/tokens/reports",
            "/v1/client/sessions/sess_native/tokens",
            "/v1/client/sessions/sess_other/tokens",
          )
      )
      check(requests.all { it["method"] == JsonPrimitive("POST") })
      check(requests[1]["body"] == JsonPrimitive("") && requests[2]["body"] == JsonPrimitive(""))
      check(
        Uri.parse("https://fixture.invalid/?" + requests[3].getValue("body").requireString())
          .getQueryParameter("organization_id") == "org_explicit"
      )
      check(selected.getToken(GetTokenOptions(skipCache = true)) == tokens.last())
      check(requests.size == 6)
      check(selected.getToken() == tokens.last() && requests.size == 6)
      check(clerk.session?.handle == selected.handle)
    }
  }

  @Test fun sharedFailureReachesBothCallersAndNextCallRecovers() = sharedToken(true)

  @Test fun sharedSuccessReachesBothCallersAndRemainsCached() = sharedToken(false)

  private fun sharedToken(failed: Boolean) {
    val started = CompletableDeferred<Unit>()
    val release = CompletableDeferred<Unit>()
    var calls = 0
    val recovered = token("recovered")
    runFixture(
      http = { _, args ->
        if (!path(args).endsWith("/tokens/firebase")) null
        else if (++calls == 1) {
          started.complete(Unit)
          release.await()
          if (failed) failure(403, "token_denied") else success(recovered)
        } else success(recovered)
      }
    ) { clerk, _ ->
      try {
        val session = checkNotNull(clerk.session)
        val first = async {
          runCatching { session.getToken(GetTokenOptions(template = "firebase")) }
        }
        started.await()
        val second = async {
          runCatching { session.getToken(GetTokenOptions(template = "firebase")) }
        }
        drainEarlierInvocations(session)
        check(calls == 1)
        release.complete(Unit)
        for (call in listOf(first, second)) {
          val result = call.await()
          if (failed) {
            val error = result.exceptionOrNull()
            check(
              error is CoreException &&
                error.status == 403 &&
                error.errors.single().code == "token_denied"
            )
          } else check(result.getOrThrow() == recovered)
        }
        check(calls == 1 && clerk.session?.handle == session.handle)
        check(session.getToken(GetTokenOptions(template = "firebase")) == recovered)
        check(
          session.getToken(GetTokenOptions(template = "firebase")) == recovered &&
            calls == if (failed) 2 else 1
        )
      } finally {
        release.complete(Unit)
      }
    }
  }

  @Test
  fun pendingSessionUsesCurrentCoreStateAndServerOutcome() {
    var pending = true
    var calls = 0
    val minted = token("active")
    runFixture(
      configure = { base -> base.clientResponse = clientStatus(base, "pending") },
      http = { base, args ->
        when {
          path(args).endsWith("/tokens") -> {
            calls++
            if (pending) failure(422, "session_pending") else success(minted)
          }
          path(args).endsWith("/sessions/sess_native") -> {
            base.clientResponse = clientStatus(base, if (pending) "pending" else "active")
            val client = base.clientResponse!!.jsonObject
            envelope(
              buildJsonObject {
                put("response", client.getValue("sessions").jsonArray[0])
                put("client", client)
              }
            )
          }
          else -> null
        }
      },
    ) { clerk, _ ->
      val saved = checkNotNull(clerk.session)
      suspend fun expectPending() {
        check(
          saved.status.rawValue == "pending" &&
            saved.currentTask?.key?.rawValue == "choose-organization"
        )
        val error =
          runCatching { saved.getToken(GetTokenOptions(skipCache = true)) }.exceptionOrNull()
        check(
          error is CoreException &&
            error.status == 422 &&
            error.errors.single().code == "session_pending"
        )
        check(clerk.session?.handle == saved.handle && saved.status.rawValue == "pending")
      }
      expectPending()
      pending = false
      saved.reload()
      check(saved.status.rawValue == "active" && saved.currentTask == null)
      check(saved.getToken() == minted)
      pending = true
      saved.reload()
      expectPending()
      check(calls == 3)
    }
  }

  private fun clientStatus(base: PackagedFixtures, status: String): JsonObject {
    val client = base.fixtures.getValue("authenticatedClient").jsonObject
    val session = client.getValue("sessions").jsonArray[0].jsonObject
    return JsonObject(
      client +
        ("sessions" to
          buildJsonArray {
            add(
              JsonObject(
                session +
                  mapOf(
                    "status" to JsonPrimitive(status),
                    "tasks" to
                      buildJsonArray {
                        if (status == "pending")
                          add(buildJsonObject { put("key", "choose-organization") })
                      },
                  )
              )
            )
          })
    )
  }

  private fun path(args: JsonObject) = URI(args.getValue("url").requireString()).path

  private suspend fun drainEarlierInvocations(session: Session) {
    // A read-only core round trip places both token callers ahead of response release.
    session.checkAuthorization(
      CheckAuthorizationParams.Case5(SessionCheckAuthorizationIsAuthorizedParamsCase5())
    )
  }

  private fun token(name: String, sid: String = "sess_native", origin: Int = 1): String {
    val now = System.currentTimeMillis() / 1000
    fun encode(value: String) =
      Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
    return encode("""{"alg":"none","typ":"JWT","oiat":$origin}""") +
      "." +
      encode("""{"sid":"$sid","iat":$now,"exp":${now + 3600}}""") +
      ".$name"
  }

  private fun success(jwt: String) =
    envelope(
      buildJsonObject {
        put("object", "token")
        put("jwt", jwt)
      }
    )

  private fun failure(status: Int, code: String) =
    envelope(
      buildJsonObject {
        put("clerk_trace_id", "trace_token_fixture")
        put(
          "errors",
          buildJsonArray {
            add(
              buildJsonObject {
                put("code", code)
                put("message", "Token rejected")
              }
            )
          },
        )
      },
      status,
    )

  private fun envelope(body: JsonElement, status: Int = 200) = buildJsonObject {
    put("status", status)
    put("headers", buildJsonObject {})
    put("body", body.toString())
  }

  private fun runFixture(
    configure: (PackagedFixtures) -> Unit = {},
    fastTimers: () -> Boolean = { false },
    http: suspend (PackagedFixtures, JsonObject) -> JsonElement?,
    action: suspend CoroutineScope.(Clerk, PackagedFixtures) -> Unit,
  ) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      withTimeout(10000) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = PackagedFixtures(instrumentation.context)
        base.clientResponse = base.fixtures.getValue("authenticatedClient")
        configure(base)
        val host =
          object : NativeCapabilities {
            override val supported = base.supported

            override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
              if (capability == "http")
                http(base, arguments.jsonObject)?.let {
                  return it
                }
              if (
                capability == "timer" &&
                  fastTimers() &&
                  arguments.jsonObject.getValue("milliseconds").jsonPrimitive.double <= 50000
              ) {
                delay(1)
                return JsonNull
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
          action(clerk, base)
        } finally {
          clerk.close()
        }
      }
    }
  }
}
