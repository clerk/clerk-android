package com.clerk.telemetry

import kotlin.test.Test
import kotlin.test.assertTrue

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

  private companion object {
    const val PROVIDER_CONSTRUCTOR_PARAMETER_COUNT = 5
  }
}
