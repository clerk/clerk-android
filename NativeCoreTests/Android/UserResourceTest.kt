package com.clerk.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.net.URI
import java.net.URLDecoder
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserResourceTest {
  @Test fun clearingNullNamesClearsFullName() = verify("clear-null")

  @Test fun clearingEmptyNamesClearsFullName() = verify("clear-empty")

  @Test fun deletingProfileImageReturnsNullableDetails() = verify("image")

  @Test fun metadataReplacementUsesRefreshedServerValue() = verify("metadata")

  @Test fun totpEnrollmentPreservesResultsAndPublishedFlags() = verify("totp")

  private fun verify(scenario: String) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val host = UserResourceHost(PackagedFixtures(instrumentation.context), scenario)
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
        val user = checkNotNull(clerk.user)
        when (scenario) {
          "clear-null",
          "clear-empty" -> {
            check(user.fullName == "Original Name")
            val value: Field<String> = if (scenario == "clear-null") Field.Null else Field.Value("")
            val updated = user.update(UpdateUserParams(firstName = value, lastName = value))
            check(updated === user)
            check(user.firstName == null && user.lastName == null && user.fullName == null)
            check(host.body(0) == mapOf("first_name" to "", "last_name" to ""))
          }
          "image" -> {
            val image = user.setProfileImage(SetProfileImageParams(file = null))
            check(image.id == "img_profile" && image.name == null && image.publicUrl == null)
            check(!user.hasImage && user.imageUrl == "")
            check(host.method(0) == "DELETE")
          }
          "metadata" -> {
            val updated =
              user.update(
                UpdateUserParams(
                  firstName = Field.Value("John"),
                  unsafeMetadata = host.desiredMetadata,
                )
              )
            check(updated === user && user.firstName == "John")
            check(user.unsafeMetadata == host.desiredMetadata)
            check(host.requests.size == 2)
            check(host.body(0) == mapOf("first_name" to "John"))
            val patch = Json.parseToJsonElement(checkNotNull(host.body(1)["unsafe_metadata"]))
            check(
              patch ==
                Json.parseToJsonElement(
                  """{"token":"new-value","serverOnly":null,"nested":{"added":"new","remove":null}}"""
                )
            )
          }
          else -> {
            val enrollment = user.createTOTP()
            check(
              enrollment.id == "totp_profile" &&
                enrollment.secret == "fixture_totp_secret" &&
                !enrollment.verified
            )
            check(enrollment.uri == "otpauth://totp/fixture?secret=fixture_totp_secret")
            val error = runCatching { user.verifyTOTP(VerifyTOTPParams("wrong")) }.exceptionOrNull()
            check(
              error is CoreException && error.errors.firstOrNull()?.code == "form_code_incorrect"
            )
            check(!user.totpEnabled)
            val verified = user.verifyTOTP(VerifyTOTPParams("123456"))
            check(verified.verified && verified.secret == null)
            check(verified.backupCodes == listOf("fixture_backup_one", "fixture_backup_two"))
            check(user.totpEnabled && user.twoFactorEnabled)
            val backup = user.createBackupCode()
            check(backup.codes == listOf("fixture_backup_one", "fixture_backup_two"))
            check(user.backupCodeEnabled)
            val removed = user.disableTOTP()
            check(removed.id == "totp_profile" && removed.deleted)
            check(!user.totpEnabled)
            check(host.body(2) == mapOf("code" to "123456"))
            check(host.method(4) == "DELETE")
          }
        }
        for (request in host.requests) check(
          query(URI(request.getValue("url").requireString()).rawQuery)["_clerk_session_id"] ==
            "sess_native"
        )
      } finally {
        clerk.close()
      }
    }
  }
}

private fun query(value: String?): Map<String, String> =
  value
    .orEmpty()
    .split('&')
    .filter { it.isNotEmpty() }
    .associate {
      val parts = it.split('=', limit = 2)
      URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts.getOrElse(1) { "" }, "UTF-8")
    }

private class UserResourceHost(private val base: PackagedFixtures, private val scenario: String) :
  NativeCapabilities {
  override val supported = base.supported
  val requests = mutableListOf<JsonObject>()
  val desiredMetadata =
    Json.parseToJsonElement("""{"token":"new-value","nested":{"keep":"same","added":"new"}}""")
      .jsonObject
  private val client = base.fixtures.getValue("authenticatedClient").jsonObject.toMutableMap()
  private val user =
    client.getValue("sessions").jsonArray[0].jsonObject.getValue("user").jsonObject.toMutableMap()

  init {
    user["first_name"] = JsonPrimitive("Original")
    user["last_name"] = JsonPrimitive("Name")
    user["has_image"] = JsonPrimitive(true)
    user["image_url"] = JsonPrimitive("https://images.example/old.png")
    user["unsafe_metadata"] = Json.parseToJsonElement("""{"staleLocal":true}""")
    user["totp_enabled"] = JsonPrimitive(false)
    user["two_factor_enabled"] = JsonPrimitive(false)
    user["backup_code_enabled"] = JsonPrimitive(false)
    updateClient()
  }

  private fun updateClient() {
    val session = client.getValue("sessions").jsonArray[0].jsonObject.toMutableMap()
    session["user"] = JsonObject(user.toMap())
    client["sessions"] = JsonArray(listOf(JsonObject(session)))
    base.clientResponse = JsonObject(client.toMap())
  }

  fun body(index: Int): Map<String, String> =
    query((requests[index]["body"] as? JsonPrimitive)?.contentOrNull)

  fun method(index: Int): String =
    query(URI(requests[index].getValue("url").requireString()).rawQuery)["_method"]
      ?: requests[index].getValue("method").requireString()

  override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
    if (capability != "http") return base.perform(capability, arguments)
    val args = arguments.jsonObject
    val url = URI(args.getValue("url").requireString())
    if (!url.path.startsWith("/v1/me")) return base.perform(capability, arguments)
    requests += args
    var failed = false
    val payload =
      when (scenario) {
        "clear-null",
        "clear-empty" -> {
          user["first_name"] = if (scenario == "clear-null") JsonNull else JsonPrimitive("")
          user["last_name"] = user.getValue("first_name")
          JsonObject(user.toMap())
        }
        "image" -> {
          user["has_image"] = JsonPrimitive(false)
          user["image_url"] = JsonPrimitive("")
          Json.parseToJsonElement("""{"object":"image","id":"img_profile","deleted":true}""")
        }
        "metadata" -> {
          user["first_name"] = JsonPrimitive("John")
          user["unsafe_metadata"] =
            if (url.path.endsWith("/metadata")) desiredMetadata
            else
              Json.parseToJsonElement(
                """{"token":"old-value","serverOnly":true,"nested":{"keep":"same","remove":"old"}}"""
              )
          JsonObject(user.toMap())
        }
        else -> {
          val totp =
            Json.parseToJsonElement(
                """{"object":"totp","id":"totp_profile","secret":"fixture_totp_secret","uri":"otpauth://totp/fixture?secret=fixture_totp_secret","verified":false,"created_at":1700000000000,"updated_at":1700000000000}"""
              )
              .jsonObject
              .toMutableMap()
          val codes =
            JsonArray(
              listOf(JsonPrimitive("fixture_backup_one"), JsonPrimitive("fixture_backup_two"))
            )
          when {
            url.path.endsWith("/attempt_verification") -> {
              if (body(requests.lastIndex)["code"] == "wrong") {
                failed = true
                Json.parseToJsonElement(
                  """{"errors":[{"code":"form_code_incorrect","message":"Incorrect code"}]}"""
                )
              } else {
                totp.remove("secret")
                totp.remove("uri")
                totp["verified"] = JsonPrimitive(true)
                totp["backup_codes"] = codes
                user["totp_enabled"] = JsonPrimitive(true)
                user["two_factor_enabled"] = JsonPrimitive(true)
                JsonObject(totp)
              }
            }
            method(requests.lastIndex) == "DELETE" -> {
              user["totp_enabled"] = JsonPrimitive(false)
              user["two_factor_enabled"] = JsonPrimitive(false)
              Json.parseToJsonElement("""{"object":"totp","id":"totp_profile","deleted":true}""")
            }
            url.path.contains("/backup_codes") -> {
              user["backup_code_enabled"] = JsonPrimitive(true)
              buildJsonObject {
                put("object", "backup_code")
                put("id", "bc_profile")
                put("codes", codes)
                put("created_at", 1700000000000)
                put("updated_at", 1700000000000)
              }
            }
            else -> JsonObject(totp)
          }
        }
      }
    val document =
      if (failed) payload
      else
        buildJsonObject {
          put("response", payload)
          if (scenario == "image" || scenario == "totp") {
            updateClient()
            put("client", JsonObject(client.toMap()))
          }
        }
    return buildJsonObject {
      put("status", if (failed) 422 else 200)
      put("headers", buildJsonObject {})
      put("body", document.toString())
    }
  }
}
