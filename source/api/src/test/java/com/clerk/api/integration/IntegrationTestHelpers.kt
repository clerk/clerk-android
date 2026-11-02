package com.clerk.api.integration

import com.clerk.api.Clerk
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assume
import org.robolectric.RuntimeEnvironment

const val TEST_VERIFICATION_CODE = "424242"
const val TEST_PASSWORD = "Clerk_Android_Test_2025_XyZ9#mK2\$pL7"
private const val INIT_TIMEOUT_MS = 30_000L
private const val KEYS_FILE = ".keys.json"

fun loadPublishableKey(instanceName: String = "with-email-codes"): String? {
  val candidates = listOf(".", "../..", "../../..")
  val file = candidates.map { File(it, KEYS_FILE) }.firstOrNull { it.exists() } ?: return null

  val root = Json.parseToJsonElement(file.readText()).jsonObject
  return root[instanceName]?.jsonObject?.get("pk")?.jsonPrimitive?.content
}

fun requirePublishableKey(): String {
  val pk = loadPublishableKey()
  Assume.assumeTrue(".keys.json not found at project root — skipping integration test", pk != null)
  return pk!!
}

suspend fun initializeClerkAndWait(publishableKey: String, timeoutMs: Long = INIT_TIMEOUT_MS) {
  if (Clerk.isInitialized.value) return
  Clerk.initialize(RuntimeEnvironment.getApplication(), publishableKey)
  withTimeout(timeoutMs) { Clerk.isInitialized.first { it } }
}

fun generateTestEmail(): String = "test+clerk_test_${UUID.randomUUID()}@example.com"

fun generateTestPhone(): String {
  val seed = UUID.randomUUID()
  val areaSeed = seed.mostSignificantBits and Long.MAX_VALUE
  val firstDigit = 2 + (areaSeed % 8L).toInt()
  val secondDigit = ((areaSeed / 8L) % 10L).toInt()
  val thirdDigit = ((areaSeed / 80L) % 10L).toInt()
  val suffix = ((seed.leastSignificantBits and 0x7FFF).toInt()) % 100
  return "+1${firstDigit}${secondDigit}${thirdDigit}55501%02d".format(suffix)
}
