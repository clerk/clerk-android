package com.clerk.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking

class ClerkTelemetryEnvironmentTest {

  @Test
  fun readsTheExplicitOwnersCurrentEnvironment() = runBlocking {
    var enabled = true
    var instanceType = "development"
    val environment =
      ClerkTelemetryEnvironment(
        sdkVersion = "2.0.0-alpha.0",
        instanceTypeProvider = { instanceType },
        telemetryEnabledProvider = { enabled },
        debugModeEnabledProvider = { false },
        publishableKeyProvider = { "pk_test_example" },
      )
    assertEquals("clerk-android", environment.sdkName)
    assertEquals("2.0.0-alpha.0", environment.sdkVersion)
    assertEquals("development", environment.instanceTypeString())
    assertEquals(true, environment.isTelemetryEnabled())
    assertEquals(false, environment.isDebugModeEnabled())
    assertEquals("pk_test_example", environment.publishableKey())
    enabled = false
    instanceType = "production"
    assertEquals(false, environment.isTelemetryEnabled())
    assertEquals("production", environment.instanceTypeString())
  }

  @Test
  fun treatsAnAbsentOrEmptyPublishableKeyAsUnavailable() = runBlocking {
    var key: String? = null
    val environment =
      ClerkTelemetryEnvironment("2.0.0-alpha.0", { "development" }, { true }, { false }, { key })
    assertNull(environment.publishableKey())
    key = ""
    assertNull(environment.publishableKey())
    key = "pk_test_example"
    assertEquals(key, environment.publishableKey())
  }
}
