package com.clerk.api

import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlatformCapabilityTest {
  private fun host() =
    AndroidCapabilities(
      publishableKey = "fixture",
      frontendAPI = "https://example.com",
      storage =
        object : CredentialStorage {
          override suspend fun read(): String? = error("Unexpected storage")

          override suspend fun write(value: String) {
            error("Unexpected storage")
          }

          override suspend fun remove() {
            error("Unexpected storage")
          }
        },
      activity = { error("Unsupported passkeys must not request a presenter") },
    )

  @Test
  @SdkSuppress(maxSdkVersion = 27)
  fun unsupportedPasskeysAreNotAdvertisedAndFailBeforePresentation() = runBlocking {
    val host = host()
    assertFalse("passkeys" in host.supported)
    assertTrue("googleIdentity" in host.supported)
    for (method in listOf("passkeys.create", "passkeys.get")) {
      val failure = runCatching { host.perform(method, JsonObject(emptyMap())) }.exceptionOrNull()
      assertTrue(failure is CoreException)
      assertEquals("capability_unavailable", (failure as CoreException).code)
    }
  }

  @Test
  @SdkSuppress(minSdkVersion = 28)
  fun supportedPlatformAdvertisesPasskeysAndPackagesBiometricPermission() {
    assertTrue("passkeys" in host().supported)
    assertTrue("googleIdentity" in host().supported)
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    assertEquals(
      PackageManager.PERMISSION_GRANTED,
      context.checkSelfPermission("android.permission.USE_BIOMETRIC"),
    )
  }
}
