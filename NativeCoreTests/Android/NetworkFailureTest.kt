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
class NetworkFailureTest {
  @Test
  fun unauthorizedClientRecoveryTerminatesAndAllowsAnotherRequest() = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val base = PackagedFixtures(instrumentation.context)
      base.clientResponse = base.fixtures.getValue("authenticatedClient")
      base.sessionReloadResponse = buildJsonObject {
        put("response", base.fixtures.getValue("session"))
      }
      var clientReads = 0
      var rejecting = true
      val host =
        object : NativeCapabilities {
          override val supported = base.supported

          override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
            if (capability != "http") return base.perform(capability, arguments)
            val path = URI(arguments.jsonObject.getValue("url").requireString()).path
            if (path == "/v1/client") clientReads++
            if (
              !rejecting ||
                (path == "/v1/client" && clientReads == 1) ||
                (path != "/v1/client" && path != "/v1/client/sessions/sess_native")
            ) {
              return base.perform(capability, arguments)
            }
            return buildJsonObject {
              put("status", if (clientReads >= 4) 403 else 401)
              put("headers", buildJsonObject {})
              put(
                "body",
                """{"errors":[{"code":"authentication_invalid","message":"Authentication invalid"}]}""",
              )
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
        val session = checkNotNull(clerk.session)
        try {
          session.reload()
          error("Expected the unauthorized response")
        } catch (error: CoreException) {
          check(error.status == 401)
          check(error.errors.firstOrNull()?.code == "authentication_invalid")
        }
        check(clientReads == 2) {
          "Expected one startup read and one recovery read, got $clientReads"
        }
        check(clerk.session === session && session.status.rawValue == "active")
        rejecting = false
        check(session.reload() === session)
      } finally {
        clerk.close()
      }
    }
  }

  @Test fun signOutPreventsRetry() = obsoleteCredentialDoesNotReachRetry(true)

  @Test fun credentialRotationPreventsRetry() = obsoleteCredentialDoesNotReachRetry(false)

  @Test fun transientNetworkFailureRetriesAndCompletes() = obsoleteCredentialDoesNotReachRetry(null)

  private fun obsoleteCredentialDoesNotReachRetry(signOut: Boolean?) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val base = PackagedFixtures(instrumentation.context)
      base.clientResponse = base.fixtures.getValue("authenticatedClient")
      base.sessionReloadResponse = buildJsonObject {
        put("response", base.fixtures.getValue("session"))
      }
      val entered = CompletableDeferred<Unit>()
      val release = CompletableDeferred<Unit>()
      var attempts = 0
      val host =
        object : NativeCapabilities {
          override val supported = base.supported

          override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
            if (capability != "http") return base.perform(capability, arguments)
            val path = URI(arguments.jsonObject.getValue("url").requireString()).path
            if (path == "/v1/client/sessions/sess_native") {
              attempts++
              if (attempts == 1) {
                entered.complete(Unit)
                release.await()
                throw IllegalStateException("Network unavailable")
              }
            }
            if (path == "/v1/me")
              return buildJsonObject {
                put("status", 200)
                put(
                  "headers",
                  buildJsonObject { put("authorization", "rotated-client-credential") },
                )
                put(
                  "body",
                  buildJsonObject {
                      put("response", base.fixtures.getValue("session").jsonObject.getValue("user"))
                    }
                    .toString(),
                )
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
      var stage = "request-start"
      try {
        val session = checkNotNull(clerk.session)
        val pending = async { runCatching { session.reload() } }
        withTimeout(3000) { entered.await() }
        stage = "identity-change"
        withTimeout(3000) {
          if (signOut == true) clerk.signOut()
          else if (signOut == false) checkNotNull(clerk.user).reload()
        }
        release.complete(Unit)
        stage = "request-completion"
        val result = withTimeout(3000) { pending.await() }
        if (signOut == null) {
          check(result.getOrThrow() === session)
          check(attempts == 2)
          check(clerk.session === session)
        } else {
          check(result.exceptionOrNull() is CoreException)
          check(attempts == 1) { "Expected one transmission, got $attempts" }
        }
        if (signOut == true) check(clerk.session == null)
        else if (signOut == false) {
          check(clerk.session === session)
          check(base.credential == "rotated-client-credential")
        }
      } catch (error: TimeoutCancellationException) {
        throw AssertionError("Timed out at $stage with $attempts attempts", error)
      } finally {
        release.complete(Unit)
        clerk.close()
      }
    }
  }
}
