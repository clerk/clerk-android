package com.clerk.api.integration

import androidx.test.platform.app.InstrumentationRegistry
import com.clerk.api.AndroidCapabilities
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfiguration
import com.clerk.api.CredentialStorage
import com.clerk.api.connect
import java.util.UUID
import org.junit.Assume.assumeTrue

internal const val TEST_VERIFICATION_CODE = "424242"
internal const val TEST_PASSWORD = "Clerk_Android_Test_2025_XyZ9#mK2\$pL7"

/** Configuration is supplied by Gradle, never read from another app's storage. */
internal fun requirePublishableKey(): String {
  val arguments = InstrumentationRegistry.getArguments()
  val key = arguments.getString("clerkIntegrationPublishableKey")?.trim().orEmpty()
  if (arguments.getString("clerkIntegrationRequired") == "true") {
    check(key.isNotEmpty()) { "Required live integration publishable key is missing" }
  }
  assumeTrue("Live integration is not configured; no service calls were made", key.isNotEmpty())
  check(key.startsWith("pk_test_")) { "Live integration requires a development publishable key" }
  return key
}

private class IntegrationCredentialStorage : CredentialStorage {
  private var value: String? = null

  override suspend fun read(): String? = value

  override suspend fun write(value: String) {
    this.value = value
  }

  override suspend fun remove() {
    value = null
  }
}

/** Real HTTP and packaged QuickJS, with memory-only credentials and no OS presenters. */
internal suspend fun connectForIntegrationTesting(publishableKey: String): Clerk {
  val configuration = ClerkConfiguration(publishableKey, "clerk-integration-test://auth/callback")
  val capabilities =
    AndroidCapabilities(
      configuration.publishableKey,
      configuration.frontendAPI,
      IntegrationCredentialStorage(),
      authStorage = IntegrationCredentialStorage(),
    )
  return Clerk.connect(
    InstrumentationRegistry.getInstrumentation().targetContext,
    configuration,
    capabilities,
  )
}

internal fun generateTestEmail(): String = "test+clerk_test_${UUID.randomUUID()}@example.com"

internal fun generateTestPhone(): String {
  val seed = UUID.randomUUID()
  val areaSeed = seed.mostSignificantBits and Long.MAX_VALUE
  val firstDigit = 2 + (areaSeed % 8L).toInt()
  val secondDigit = ((areaSeed / 8L) % 10L).toInt()
  val thirdDigit = ((areaSeed / 80L) % 10L).toInt()
  val suffix = ((seed.leastSignificantBits and 0x7FFF).toInt()) % 100
  return "+1${firstDigit}${secondDigit}${thirdDigit}55501%02d".format(suffix)
}
