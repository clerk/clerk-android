package com.clerk.ui.auth

import com.clerk.api.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class SignUpPresentationTest {
  @Test
  fun optionalMissingFieldsDoNotBlockRequiredPassword() =
    fixture(
      required = listOf("email_address", "password"),
      missing = listOf("first_name", "password", "last_name"),
    ) {
      assertEquals("password", it.signUp.firstFieldToCollect)
    }

  @Test
  fun onlyOptionalMissingFieldsDoNotBlockCompletion() =
    fixture(
      required = listOf("email_address"),
      missing = listOf("first_name", "last_name"),
    ) {
      assertNull(it.signUp.firstFieldToCollect)
    }

  @Test
  fun requiredFieldsFollowPresentationOrder() =
    fixture(
      required = listOf("password", "username", "phone_number", "email_address"),
      missing = listOf("password", "username", "phone_number", "email_address"),
    ) {
      assertEquals("email_address", it.signUp.firstFieldToCollect)
    }

  @Test
  fun activeEmailCodeOverridesEnvironmentEmailLink() =
    fixture(
      active = "email_code",
      environment = listOf("email_link"),
    ) {
      assertEquals("email_code", it.signUp.emailVerificationStrategy(it))
    }

  @Test
  fun activeEmailLinkOverridesEnvironmentEmailCode() =
    fixture(
      active = "email_link",
      environment = listOf("email_code"),
    ) {
      assertEquals("email_link", it.signUp.emailVerificationStrategy(it))
    }

  @Test
  fun missingActiveStrategyUsesEnvironmentEmailLink() =
    fixture(environment = listOf("email_link")) {
      assertEquals("email_link", it.signUp.emailVerificationStrategy(it))
    }

  @Test
  fun missingEmailLinkConfigurationUsesEmailCode() = fixture {
    assertEquals("email_code", it.signUp.emailVerificationStrategy(it))
  }

  // Feed the real generated projections a modified TypeScript preview snapshot.
  // No mock native DTO or HTTP implementation owns the presentation state.
  private fun fixture(
    required: List<String> = emptyList(),
    missing: List<String> = emptyList(),
    active: String? = null,
    environment: List<String> = emptyList(),
    verify: (Clerk) -> Unit,
  ) {
    val document =
      Json.parseToJsonElement(File("src/main/assets/clerk-preview/default.json").readText())
        .jsonObject
    val originalState = document.getValue("state").jsonObject
    val original = originalState.getValue("resources").jsonArray
    val emailHandle =
      original
        .first {
          it.jsonObject.getValue("handle").jsonObject["type"] ==
            JsonPrimitive("SignUpVerifications")
        }
        .jsonObject
        .getValue("state")
        .jsonObject
        .getValue("emailAddress")
        .jsonObject
        .getValue("\$ref")
    val resources =
      JsonArray(
        original.map { record ->
          val item = record.jsonObject
          val handle = item.getValue("handle").jsonObject
          val state = item.getValue("state").jsonObject
          val updated =
            when {
              handle["type"] == JsonPrimitive("SignUp") ->
                JsonObject(
                  state +
                    mapOf(
                      "requiredFields" to JsonArray(required.map(::JsonPrimitive)),
                      "optionalFields" to
                        JsonArray(listOf("first_name", "last_name").map(::JsonPrimitive)),
                      "missingFields" to JsonArray(missing.map(::JsonPrimitive)),
                    )
                )
              handle == emailHandle ->
                JsonObject(state + ("strategy" to (active?.let(::JsonPrimitive) ?: JsonNull)))
              handle["type"] == JsonPrimitive("EnvironmentResource") -> {
                val settings = state.getValue("userSettings").jsonObject
                val attributes = settings.getValue("attributes").jsonObject
                val email = attributes.getValue("email_address").jsonObject
                JsonObject(
                  state +
                    ("userSettings" to
                      JsonObject(
                        settings +
                          ("attributes" to
                            JsonObject(
                              attributes +
                                ("email_address" to
                                  JsonObject(
                                    email +
                                      ("verifications" to
                                        JsonArray(environment.map(::JsonPrimitive)))
                                  ))
                            ))
                      ))
                )
              }
              else -> state
            }
          JsonObject(item + ("state" to updated))
        }
      )
    val transport =
      object : CoreTransport {
        override var receive: ((JsonElement) -> Unit)? = null

        override fun send(message: JsonElement) {
          error("Presentation must not invoke the core")
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
          put("id", "presentation")
          put("manifest", document.getValue("manifest"))
          put("state", JsonObject(originalState + ("resources" to resources)))
        }
      )
      verify(runtime.resource(runtime.roots.getValue("clerk")) as Clerk)
    } finally {
      runtime.close()
    }
  }
}
