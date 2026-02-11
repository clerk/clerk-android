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

/**
 * Reads the publishable key from `.keys.json` at the project root.
 *
 * The file format is:
 * ```json
 * {
 *   "with-email-codes": {
 *     "pk": "pk_test_..."
 *   }
 * }
 * ```
 *
 * Tries multiple paths because Gradle's working directory varies by invocation context. Returns
 * null if the file doesn't exist.
 */
fun loadPublishableKey(instanceName: String = "with-email-codes"): String? {
  val candidates = listOf(".", "../..", "../../..")
  val file = candidates.map { File(it, KEYS_FILE) }.firstOrNull { it.exists() } ?: return null

  val root = Json.parseToJsonElement(file.readText()).jsonObject
  return root[instanceName]?.jsonObject?.get("pk")?.jsonPrimitive?.content
}

/**
 * Calls [Assume.assumeTrue] so tests are **skipped** (not failed) when `.keys.json` is absent.
 * Returns the non-null key.
 */
fun requirePublishableKey(): String {
  val pk = loadPublishableKey()
  Assume.assumeTrue(".keys.json not found at project root — skipping integration test", pk != null)
  return pk!!
}

/**
 * Initializes Clerk with a real publishable key and waits for [Clerk.isInitialized] to become true.
 * Safe to call multiple times — subsequent calls return immediately if already initialized.
 */
suspend fun initializeClerkAndWait(publishableKey: String, timeoutMs: Long = INIT_TIMEOUT_MS) {
  if (Clerk.isInitialized.value) return
  Clerk.initialize(RuntimeEnvironment.getApplication(), publishableKey)
  withTimeout(timeoutMs) { Clerk.isInitialized.first { it } }
}

fun generateTestEmail(): String = "test+clerk_test_${UUID.randomUUID()}@example.com"
