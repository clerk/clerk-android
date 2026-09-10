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
class EmailLinkFailureTest {
  @Test fun signInStorageFailurePreventsPreparation() = verify("signIn", true)

  @Test fun signUpStorageFailurePreventsPreparation() = verify("signUp", true)

  @Test fun signInPreparationFailurePreservesVerifier() = verify("signIn", false)

  @Test fun signUpPreparationFailurePreservesVerifier() = verify("signUp", false)

  private fun verify(kind: String, storageFailure: Boolean) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val base = PackagedFixtures(instrumentation.context)
      val client = base.fixtures.getValue("client").jsonObject.toMutableMap()
      client["sign_in"] =
        JsonObject(
          base.fixtures.getValue("signIn").jsonObject +
            ("supported_first_factors" to
              buildJsonArray {
                add(
                  buildJsonObject {
                    put("strategy", "email_link")
                    put("email_address_id", "idn_email")
                    put("safe_identifier", "test@example.com")
                  }
                )
              })
        )
      client["sign_up"] =
        JsonObject(
          base.fixtures.getValue("signUp").jsonObject +
            ("email_address" to JsonPrimitive("test@example.com"))
        )
      base.clientResponse = JsonObject(client)
      var prepares = 0
      var savedRecord: String? = null
      val host =
        object : NativeCapabilities {
          override val supported = base.supported

          override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
            if (capability == "authStorage.write" && storageFailure)
              throw CoreException("secure_storage_locked")
            if (capability == "http") {
              val path = URI(arguments.jsonObject.getValue("url").requireString()).path
              if (
                path.endsWith("/prepare_first_factor") || path.endsWith("/prepare_verification")
              ) {
                prepares++
                savedRecord = checkNotNull(base.authRecord)
                val saved = Json.parseToJsonElement(savedRecord!!).jsonObject
                check(saved.getValue("kind").requireString() == kind)
                check(
                  saved.getValue("flowId").requireString() ==
                    if (kind == "signIn") "sia_native" else "sua_native"
                )
                check(saved.getValue("codeVerifier").requireString().length == 43)
                return buildJsonObject {
                  put("status", 422)
                  put("headers", buildJsonObject {})
                  put(
                    "body",
                    """{"errors":[{"code":"prepare_rejected","message":"Preparation rejected"}]}""",
                  )
                }
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
        try {
          if (kind == "signIn")
            clerk.signIn.emailLink.sendLink(
              SignInEmailLinkSendParams.Case2(
                SignInEmailLinkSendLinkParamsCase2(emailAddressId = "idn_email")
              )
            )
          else clerk.signUp.verifications.sendEmailLink(SignUpEmailLinkSendParams())
          error("Expected email-link preparation to fail")
        } catch (failure: CoreException) {
          if (storageFailure) check(failure.code == "secure_storage_locked")
          else check(failure.errors.firstOrNull()?.code == "prepare_rejected")
        }
        check(prepares == if (storageFailure) 0 else 1)
        if (storageFailure) check(base.authRecord == null)
        else check(base.authRecord == savedRecord)
        check(clerk.session == null)
      } finally {
        clerk.close()
      }
    }
  }
}
