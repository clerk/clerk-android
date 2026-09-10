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
class BiometricMetadataTest {
  @Test fun enrollmentPreservesUnrelatedMetadata() = verify("enroll")

  @Test fun cleanupPreservesUnrelatedMetadata() = verify("forget")

  @Test fun enrollmentRejectsUnreadableMetadata() = verify("corrupt")

  @Test fun enrollmentCleansAReplacedKeyForTheSameId() = verify("same-id")

  @Test fun revocationSurvivesLocalReadFailure() = verify("revoke-read")

  @Test fun committedEnrollmentSurvivesCleanupReadFailure() = verify("saved-read")

  @Test fun lostSessionRollsBackServerEnrollment() = verify("lost-session")

  @Test fun listPreservesBackendValues() = verify("list")

  private fun verify(scenario: String) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val host = BiometricMetadataHost(PackagedFixtures(instrumentation.context), scenario)
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
        when (scenario) {
          "corrupt" -> {
            val error = runCatching { clerk.biometricCredentials.enroll() }.exceptionOrNull()
            check(error is CoreException && error.code == "invalid_biometric_metadata")
            check(host.base.biometricRecords == "not-json")
            check(host.writes == 0 && host.base.biometricSignCount == 0)
          }
          "forget" -> {
            check(
              clerk.biometricCredentials.forgetLocalCredentials(
                BiometricCredentialsForgetLocalCredentialsParams("user_native")
              ) == 1.0
            )
            check(host.records() == listOf(host.other, host.malformedOwn))
            check(host.deleted == listOf("key_old"))
          }
          "revoke-read" -> {
            val revoked =
              clerk.biometricCredentials.revoke(BiometricCredentialsRevokeParams("td_old"))
            check(
              revoked.id == "td_old" && revoked.status == BiometricCredentialStatus.Case2("revoked")
            )
            check(host.writes == 0 && host.deleted.isEmpty())
          }
          "lost-session" -> {
            val error = runCatching { clerk.biometricCredentials.enroll() }.exceptionOrNull()
            check(error is CoreException && error.code == "stale_authentication_attempt")
            check(clerk.session == null && host.deleted == listOf("tdlk_native"))
            check(host.records().size == 3)
            check(
              host.requests.any {
                val url = URI(it.getValue("url").requireString())
                url.path.endsWith("/td_native") && url.rawQuery.contains("_method=DELETE")
              }
            )
          }
          "list" -> {
            val value = clerk.biometricCredentials.list().single()
            check(value.id == "td_old" && value.`object` == "trusted_device")
            check(value.platform == BiometricCredentialPlatform.Case2("android"))
            check(value.algorithm == BiometricCredentialAlgorithm.Case1("ES256"))
            check(value.name == "Test device" && value.appIdentifier == "com.example.native")
            check(value.createdAt.toEpochMilli() == 1710000000000L)
            check(value.updatedAt.toEpochMilli() == 1710000001000L)
            check(value.lastUsedAt?.toEpochMilli() == 1710000002000L && value.revokedAt == null)
          }
          else -> {
            val enrolled =
              clerk.biometricCredentials.enroll(
                BiometricCredentialEnrollmentParams(identifierHint = " TEST@example.com ")
              )
            check(enrolled.id == "td_native")
            val records = host.records()
            check(records.contains(host.other) && records.contains(host.malformedOwn))
            check(records.any { it.jsonObject["localKeyId"] == JsonPrimitive("tdlk_native") })
            check(!host.deleted.contains("tdlk_native"))
            if (scenario == "saved-read") {
              check(host.deleted.isEmpty() && records.size == 4)
            } else {
              check(host.deleted == listOf("key_old") && records.size == 3)
            }
          }
        }
        host.requests.forEach { request ->
          check(
            URI(request.getValue("url").requireString())
              .rawQuery
              .contains("_clerk_session_id=sess_native")
          )
        }
      } finally {
        clerk.close()
      }
    }
  }
}

private class BiometricMetadataHost(val base: PackagedFixtures, private val scenario: String) :
  NativeCapabilities {
  override val supported = base.supported
  val other =
    Json.parseToJsonElement(
      """{"id":"other_legacy","localKeyId":"other_key","appIdentifier":"com.example.other","futureField":{"nested":["keep",null]}}"""
    )
  val malformedOwn =
    Json.parseToJsonElement(
      """{"id":"own_legacy","localKeyId":"own_key","appIdentifier":"com.example.native"}"""
    )
  val deleted = mutableListOf<String>()
  val requests = mutableListOf<JsonObject>()
  var writes = 0

  init {
    base.clientResponse = base.fixtures.getValue("authenticatedClient")
    val own = buildJsonObject {
      put("id", if (scenario == "same-id") "td_native" else "td_old")
      put("localKeyId", "key_old")
      put("userId", "user_native")
      put("appIdentifier", "com.example.native")
      put("policy", "biometry_current_set")
      put("createdAt", 1710000000000L)
      put("updatedAt", 1710000001000L)
    }
    base.biometricRecords =
      if (scenario == "corrupt") "not-json"
      else JsonArray(listOf(own, other, malformedOwn)).toString()
  }

  fun records() = Json.parseToJsonElement(checkNotNull(base.biometricRecords)).jsonArray.toList()

  override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
    val args = arguments.jsonObject
    if (
      capability == "biometrics.storage.read" &&
        args["key"] == JsonPrimitive("credentials") &&
        (scenario == "revoke-read" || (scenario == "saved-read" && writes > 0))
    )
      throw CoreException("secure_storage_locked")
    if (capability == "biometrics.storage.write" && args["key"] == JsonPrimitive("credentials"))
      writes++
    if (capability == "biometrics.deleteKey") deleted += args.getValue("localKeyId").requireString()
    if (capability == "http") {
      val url = URI(args.getValue("url").requireString())
      if (url.path.contains("/biometric_credentials")) {
        requests += args
        if (scenario == "lost-session" && url.path.endsWith("/attempt")) {
          val response = base.perform(capability, arguments).jsonObject
          val body = Json.parseToJsonElement(response.getValue("body").requireString()).jsonObject
          return JsonObject(
            response +
              ("body" to
                JsonPrimitive(
                  JsonObject(body + ("client" to base.fixtures.getValue("client"))).toString()
                ))
          )
        }
        if (
          url.path.endsWith("/td_old") ||
            url.path.endsWith("/td_native") ||
            url.path.endsWith("/biometric_credentials")
        ) {
          val credential = buildJsonObject {
            put("id", "td_old")
            put("object", "trusted_device")
            put("platform", "android")
            put("app_identifier", "com.example.native")
            put("name", "Test device")
            put("algorithm", "ES256")
            put("status", if (url.path.endsWith("/td_old")) "revoked" else "active")
            put("created_at", 1710000000000L)
            put("updated_at", 1710000001000L)
            put("last_used_at", 1710000002000L)
            put("revoked_at", JsonNull)
          }
          return buildJsonObject {
            put("status", 200)
            put("headers", buildJsonObject {})
            put(
              "body",
              buildJsonObject {
                  put(
                    "response",
                    if (url.path.endsWith("/td_old")) credential else JsonArray(listOf(credential)),
                  )
                }
                .toString(),
            )
          }
        }
      }
    }
    return base.perform(capability, arguments)
  }
}
