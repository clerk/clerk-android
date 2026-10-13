package com.clerk.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class ClerkTelemetryEnvironmentTest {

  @Test
  fun exposesNoArgumentConstructorForBinaryCompatibility() {
    val constructors = ClerkTelemetryEnvironment::class.java.constructors

    assertTrue(constructors.any { it.parameterCount == 0 })
  }

  @Test
  fun exposesProviderConstructorForInjectedEnvironments() {
    val constructors = ClerkTelemetryEnvironment::class.java.constructors

    assertTrue(constructors.any { it.parameterCount == PROVIDER_CONSTRUCTOR_PARAMETER_COUNT })
  }

  @Test
  fun readsEachValueFromItsInjectedProvider() = runBlocking {
    var telemetryEnabled = true
    val environment =
      ClerkTelemetryEnvironment(
        sdkVersion = "1.2.3",
        instanceTypeProvider = { "DEVELOPMENT" },
        telemetryEnabledProvider = { telemetryEnabled },
        debugModeEnabledProvider = { true },
        publishableKeyProvider = { "pk_test_123" },
      )

    assertEquals("clerk-android", environment.sdkName)
    assertEquals("1.2.3", environment.sdkVersion)
    assertEquals("DEVELOPMENT", environment.instanceTypeString())
    assertTrue(environment.isTelemetryEnabled())
    assertTrue(environment.isDebugModeEnabled())
    assertEquals("pk_test_123", environment.publishableKey())

    telemetryEnabled = false
    assertFalse(environment.isTelemetryEnabled())
  }

  @Test
  fun treatsEmptyPublishableKeyAsMissing() = runBlocking {
    assertNull(environment(publishableKey = "").publishableKey())
    assertNull(environment(publishableKey = null).publishableKey())
  }

  private fun environment(publishableKey: String?) =
    ClerkTelemetryEnvironment(
      sdkVersion = "1.2.3",
      instanceTypeProvider = { "PRODUCTION" },
      telemetryEnabledProvider = { false },
      debugModeEnabledProvider = { false },
      publishableKeyProvider = { publishableKey },
    )

  private companion object {
    const val PROVIDER_CONSTRUCTOR_PARAMETER_COUNT = 5
  }
}
