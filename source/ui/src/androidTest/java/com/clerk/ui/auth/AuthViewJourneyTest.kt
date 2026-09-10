package com.clerk.ui.auth

import android.graphics.Bitmap
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.clerk.api.*
import com.clerk.ui.R
import com.clerk.ui.core.composition.ClerkProvider
import com.clerk.ui.core.composition.LocalClerk
import java.io.File
import java.net.URI
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Renders the production AuthView; only HTTP and credential persistence are fixtures. */
@RunWith(AndroidJUnit4::class)
class AuthViewJourneyTest {
  @get:Rule val compose = createComposeRule()

  @Test
  fun emailCodeErrorCanBeRetriedAndPrebuiltFlowFinalizesExactlyOnce() = emailCodeJourney(false)

  @Test
  fun automaticPasskeyPreparationFailureIsVisibleAndEmailSignInCompletes() = emailCodeJourney(true)

  @Test fun disabledPasskeyDoesNotPrepareAndEmailSignInCompletes() = emailCodeJourney(false, true)

  private fun emailCodeJourney(rejectAutomaticPasskey: Boolean, disabledPasskey: Boolean = false) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val context = instrumentation.targetContext
    val fixtures =
      Json.parseToJsonElement(
          instrumentation.context.assets.open("fapi.json").bufferedReader().use { it.readText() }
        )
        .jsonObject
    val host = EmailCodeJourneyHost(fixtures, rejectAutomaticPasskey, disabledPasskey)
    val prefix =
      if (disabledPasskey) "disabled-passkey-" else if (rejectAutomaticPasskey) "passkey-" else ""
    val key =
      "pk_test_" +
        Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
    val clerk = runBlocking {
      withContext(Dispatchers.Main.immediate) {
        Clerk.connect(context, ClerkConfiguration(key, "clerk-test://sso-callback"), host)
      }
    }
    val mounted = mutableStateOf(true)
    var completions = 0
    try {
      compose.setContent {
        if (mounted.value) {
          ClerkProvider(clerk) {
            if (LocalClerk.isAuthFlowComplete) Text("Signed in through the core")
            else
              AuthView(
                mode = AuthMode.SignIn,
                isDismissible = false,
                persistIdentifiers = false,
                onAuthComplete = { completions++ },
                logo = {},
              )
          }
        }
      }
      if (rejectAutomaticPasskey) {
        waitForText(EmailCodeJourneyHost.PASSKEY_PREPARATION_MESSAGE)
        compose.runOnIdle {
          assertEquals(1, host.passkeyPreparations)
          assertNull(clerk.session)
          assertEquals(0, completions)
        }
        capture("${prefix}00-preparation-error")
      }
      compose.onNode(hasSetTextAction()).performTextInput("test@example.com")
      capture("${prefix}01-identifier")
      compose.onNodeWithText(context.getString(R.string.continue_text)).performClick()
      waitForText(context.getString(R.string.check_your_email))
      compose.runOnIdle {
        assertEquals("needs_first_factor", clerk.signIn.status.rawValue)
        assertNull(clerk.session)
        assertEquals(0, completions)
      }
      compose.onNode(hasSetTextAction()).performTextReplacement("000000")
      waitForText(EmailCodeJourneyHost.INVALID_CODE_MESSAGE)
      compose.runOnIdle {
        assertEquals("needs_first_factor", clerk.signIn.status.rawValue)
        assertNull(clerk.session)
        assertEquals(0, completions)
        assertEquals(0, host.touches)
      }
      capture("${prefix}02-invalid-code")
      compose.onNode(hasSetTextAction()).performTextReplacement("424242")
      waitForText("Signed in through the core")
      compose.runOnIdle {
        assertEquals("active", clerk.session?.status?.rawValue)
        assertEquals("user_native", clerk.user?.id)
        assertEquals(1, completions)
        assertEquals(1, host.touches)
        assertEquals(listOf("000000", "424242"), host.attemptedCodes)
        assertEquals(if (rejectAutomaticPasskey) 1 else 0, host.passkeyPreparations)
      }
      capture("${prefix}03-completed")
    } catch (error: Throwable) {
      println(
        "Auth journey attempted codes: ${host.attemptedCodes}; touches: ${host.touches}; requests: ${host.requests}"
      )
      println(compose.onRoot().printToString())
      runCatching { capture("${prefix}failure") }
      throw error
    } finally {
      compose.runOnIdle { mounted.value = false }
      compose.waitForIdle()
      runBlocking { withContext(Dispatchers.Main.immediate) { clerk.close() } }
    }
  }

  private fun waitForText(text: String) {
    compose.waitUntil(15_000) {
      compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithText(text).assertIsDisplayed()
  }

  private fun capture(name: String) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val additionalOutput =
      InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
    val root = additionalOutput?.let(::File) ?: context.getExternalFilesDir(null)
    val directory = File(root, "auth-view-journey").apply { mkdirs() }
    File(directory, "$name.png").outputStream().use { output ->
      check(
        compose
          .onRoot()
          .captureToImage()
          .asAndroidBitmap()
          .compress(Bitmap.CompressFormat.PNG, 100, output)
      )
    }
  }
}

private class EmailCodeJourneyHost(
  private val fixtures: JsonObject,
  private val rejectAutomaticPasskey: Boolean,
  private val disabledPasskey: Boolean,
) : NativeCapabilities {
  override val supported =
    setOf("http", "storage", "timer", "random") +
      if (rejectAutomaticPasskey || disabledPasskey) setOf("passkeys") else emptySet()
  private var credential: JsonElement = JsonNull
  private var signIn: JsonElement = JsonNull
  private var complete = false
  private val expires = System.currentTimeMillis() + 3_600_000
  val attemptedCodes = mutableListOf<String>()
  val requests = mutableListOf<String>()
  var touches = 0
    private set

  var passkeyPreparations = 0
    private set

  private val environment: JsonObject = run {
    val original = fixtures.getValue("environment").jsonObject
    val settings = original.getValue("user_settings").jsonObject
    val attributes = settings.getValue("attributes").jsonObject
    val email =
      JsonObject(
        attributes.getValue("email_address").jsonObject +
          mapOf(
            "enabled" to JsonPrimitive(true),
            "used_for_first_factor" to JsonPrimitive(true),
            "first_factors" to JsonArray(listOf(JsonPrimitive("email_code"))),
            "verifications" to JsonArray(listOf(JsonPrimitive("email_code"))),
          )
      )
    val enabledAttributes =
      attributes.toMutableMap().apply {
        put("email_address", email)
        if (rejectAutomaticPasskey || disabledPasskey)
          put(
            "passkey",
            JsonObject(
              attributes.getValue("passkey").jsonObject +
                mapOf(
                  "enabled" to JsonPrimitive(!disabledPasskey),
                  "used_for_first_factor" to JsonPrimitive(true),
                )
            ),
          )
      }
    val enabledSettings =
      settings.toMutableMap().apply {
        put("attributes", JsonObject(enabledAttributes))
        if (rejectAutomaticPasskey || disabledPasskey)
          put(
            "passkey_settings",
            JsonObject(
              settings.getValue("passkey_settings").jsonObject +
                ("allow_autofill" to JsonPrimitive(true))
            ),
          )
      }
    JsonObject(original + ("user_settings" to JsonObject(enabledSettings)))
  }

  private fun session() =
    JsonObject(
      fixtures.getValue("session").jsonObject +
        mapOf(
          "expire_at" to JsonPrimitive(expires),
          "abandon_at" to JsonPrimitive(expires),
        )
    )

  private fun client() =
    JsonObject(
      fixtures.getValue("client").jsonObject +
        mapOf(
          "sign_in" to signIn,
          "sessions" to JsonArray(if (complete) listOf(session()) else emptyList()),
          "last_active_session_id" to if (touches > 0) JsonPrimitive("sess_native") else JsonNull,
        )
    )

  private fun updateSignIn(prepared: Boolean = false) {
    signIn =
      JsonObject(
        fixtures.getValue("signIn").jsonObject +
          mapOf(
            "identifier" to JsonPrimitive("test@example.com"),
            "status" to JsonPrimitive(if (complete) "complete" else "needs_first_factor"),
            "created_session_id" to if (complete) JsonPrimitive("sess_native") else JsonNull,
            "supported_first_factors" to
              buildJsonArray {
                add(
                  buildJsonObject {
                    put("strategy", "email_code")
                    put("email_address_id", "idn_email")
                    put("safe_identifier", "test@example.com")
                  }
                )
              },
            "first_factor_verification" to
              if (prepared || complete)
                JsonObject(
                  fixtures
                    .getValue("signIn")
                    .jsonObject
                    .getValue("first_factor_verification")
                    .jsonObject +
                    mapOf(
                      "status" to JsonPrimitive(if (complete) "verified" else "unverified"),
                      "strategy" to JsonPrimitive("email_code"),
                      "external_verification_redirect_url" to JsonNull,
                      "expire_at" to JsonPrimitive(expires),
                    )
                )
              else JsonNull,
          )
      )
  }

  override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
    val args = arguments.jsonObject
    when (capability) {
      "storage.read" -> return credential
      "storage.write" -> {
        credential = args.getValue("value")
        return JsonNull
      }
      "storage.remove" -> {
        credential = JsonNull
        return JsonNull
      }
      "timer" -> {
        delay(args.getValue("milliseconds").jsonPrimitive.double.toLong())
        return JsonNull
      }
    }
    check(capability == "http") { "Unexpected capability: $capability" }
    val path = URI(args.getValue("url").jsonPrimitive.content).path
    requests += path
    val form =
      android.net.Uri.parse("https://fixture/?${args["body"]?.jsonPrimitive?.content.orEmpty()}")
    var status = 200
    val body =
      when {
        path.endsWith("/environment") -> buildJsonObject { put("response", environment) }
        path.endsWith("/client") -> buildJsonObject { put("response", client()) }
        path.endsWith("/client/sign_ins") -> {
          if (form.getQueryParameter("strategy") == "passkey") {
            passkeyPreparations++
            check(rejectAutomaticPasskey)
            status = 422
            buildJsonObject {
              put(
                "errors",
                buildJsonArray {
                  add(
                    buildJsonObject {
                      put("code", "passkey_preparation_failed")
                      put("message", "Passkey preparation failed")
                      put("long_message", PASSKEY_PREPARATION_MESSAGE)
                    }
                  )
                },
              )
            }
          } else {
            check(form.getQueryParameter("identifier") == "test@example.com")
            updateSignIn()
            buildJsonObject {
              put("response", signIn)
              put("client", client())
            }
          }
        }
        path.endsWith("/prepare_first_factor") -> {
          check(form.getQueryParameter("strategy") == "email_code")
          updateSignIn(prepared = true)
          buildJsonObject {
            put("response", signIn)
            put("client", client())
          }
        }
        path.endsWith("/attempt_first_factor") -> {
          check(form.getQueryParameter("strategy") == "email_code")
          val code = checkNotNull(form.getQueryParameter("code"))
          attemptedCodes += code
          if (code == "424242") {
            complete = true
            updateSignIn()
            buildJsonObject {
              put("response", signIn)
              put("client", client())
            }
          } else {
            status = 422
            buildJsonObject {
              put(
                "errors",
                buildJsonArray {
                  add(
                    buildJsonObject {
                      put("code", "form_code_incorrect")
                      put("message", "Incorrect code")
                      put("long_message", INVALID_CODE_MESSAGE)
                    }
                  )
                },
              )
            }
          }
        }
        path.endsWith("/touch") -> {
          check(complete)
          touches++
          buildJsonObject {
            put("response", session())
            put("client", client())
          }
        }
        path.endsWith("/tokens") -> {
          val header =
            Base64.getUrlEncoder()
              .withoutPadding()
              .encodeToString("""{"alg":"RS256","typ":"JWT"}""".toByteArray())
          val claims = buildJsonObject {
            put("sub", "user_native")
            put("sid", "sess_native")
            put("iat", System.currentTimeMillis() / 1000)
            put("exp", expires / 1000)
          }
          val payload =
            Base64.getUrlEncoder().withoutPadding().encodeToString(claims.toString().toByteArray())
          buildJsonObject {
            put("object", "token")
            put("jwt", "$header.$payload.fixture_signature")
          }
        }
        else -> error("Unexpected HTTP fixture path: $path")
      }
    return buildJsonObject {
      put("status", status)
      put(
        "headers",
        buildJsonObject {
          if (!path.endsWith("/environment")) put("authorization", "fixture-credential")
        },
      )
      put("body", body.toString())
    }
  }

  companion object {
    const val INVALID_CODE_MESSAGE = "That code is incorrect. Try again."
    const val PASSKEY_PREPARATION_MESSAGE = "Passkey sign-in could not start. Try email instead."
  }
}
