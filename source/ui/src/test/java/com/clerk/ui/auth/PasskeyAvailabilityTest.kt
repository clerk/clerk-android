package com.clerk.ui.auth

import com.clerk.api.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.*
import org.junit.Assert.assertEquals
import org.junit.Test

class PasskeyAvailabilityTest {
  @Test fun enabledFirstFactorPermitsAutomaticPasskey() = verify(true, true, true, true)

  @Test fun registrationOnlyPasskeyDoesNotStartAuthentication() = verify(true, false, true, false)

  @Test fun disabledPasskeyDoesNotStartAuthentication() = verify(false, true, true, false)

  @Test fun missingPasskeyDoesNotStartAuthentication() = verify(null, true, true, false)

  @Test fun disabledAutofillDoesNotStartAutomaticPasskey() = verify(true, true, false, false)

  private fun verify(
    enabled: Boolean?,
    firstFactor: Boolean,
    autofill: Boolean,
    expected: Boolean,
  ) {
    val document =
      Json.parseToJsonElement(File("src/main/assets/clerk-preview/default.json").readText())
        .jsonObject
    val originalState = document.getValue("state").jsonObject
    val resources =
      JsonArray(
        originalState.getValue("resources").jsonArray.map { record ->
          val item = record.jsonObject
          if (item.getValue("handle").jsonObject["type"] != JsonPrimitive("EnvironmentResource"))
            return@map record
          val state = item.getValue("state").jsonObject
          val settings = state.getValue("userSettings").jsonObject
          val attributes = settings.getValue("attributes").jsonObject
          val updated =
            if (enabled == null) JsonObject(attributes - "passkey")
            else
              JsonObject(
                attributes +
                  ("passkey" to
                    JsonObject(
                      attributes.getValue("passkey").jsonObject +
                        mapOf(
                          "enabled" to JsonPrimitive(enabled),
                          "used_for_first_factor" to JsonPrimitive(firstFactor),
                        )
                    ))
              )
          JsonObject(
            item +
              ("state" to
                JsonObject(
                  state +
                    ("userSettings" to
                      JsonObject(
                        settings +
                          mapOf(
                            "attributes" to updated,
                            "passkeySettings" to
                              JsonObject(
                                settings.getValue("passkeySettings").jsonObject +
                                  ("allow_autofill" to JsonPrimitive(autofill))
                              ),
                          )
                      ))
                ))
          )
        }
      )
    val transport =
      object : CoreTransport {
        override var receive: ((JsonElement) -> Unit)? = null

        override fun send(message: JsonElement) {
          error("Availability must not invoke the core")
        }

        override fun close() {
          receive = null
        }
      }
    val runtime = CoreRuntime(transport, Dispatchers.Unconfined)
    try {
      transport.receive!!.invoke(
        buildJsonObject {
          put("kind", "ready")
          put("id", "passkey-availability")
          put("manifest", document.getValue("manifest"))
          put("state", JsonObject(originalState + ("resources" to resources)))
        }
      )
      val clerk = runtime.resource(runtime.roots.getValue("clerk")) as Clerk
      val configured = AuthStartViewHelper(clerk).passkeySignInConfigIsEnabled
      assertEquals(expected, configured)
      assertEquals(expected, shouldStartAutomaticPasskeySignIn(AuthMode.SignIn, false, configured))
    } finally {
      runtime.close()
    }
  }
}
