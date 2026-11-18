package com.clerk.api.configuration

import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.FrameworkIntegrationApi
import com.clerk.api.SharedSessionSyncConfig
import com.clerk.api.devicekey.DeviceKeyHolder
import com.clerk.api.devicekey.JvmDeviceKey
import java.io.IOException
import java.security.KeyStoreException
import java.util.concurrent.CountDownLatch
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceBindingTest {
  private val scope = CoroutineScope(Dispatchers.IO)

  @After
  fun tearDown() {
    scope.cancel()
  }

  @Test
  fun `an opted out runtime does not sign and never loads the key`() = runBlocking {
    for (reason in
      listOf(DeviceBinding.Reason.SHARED_SESSION_SYNC, DeviceBinding.Reason.FRAMEWORK_HOST)) {
      var loads = 0
      val holder = DeviceKeyHolder {
        loads += 1
        JvmDeviceKey.getOrCreate()
      }

      val binding = scope.resolveDeviceBinding(optOut = reason, holder = holder)

      assertEquals(DeviceBinding.NotSigning(reason), binding.current(DEVICE_KEY_WAIT))
      assertEquals(0, loads)
    }
  }

  @Test
  fun `plain options sign`() {
    assertNull(signingOptOut(null))
    assertNull(signingOptOut(ClerkConfigurationOptions()))
    assertNull(signingOptOut(ClerkConfigurationOptions().withCustomHeaders(mapOf("x-app" to "1"))))
  }

  @Test
  fun `shared session sync opts out`() {
    assertEquals(
      DeviceBinding.Reason.SHARED_SESSION_SYNC,
      signingOptOut(ClerkConfigurationOptions(sharedSessionSync = SharedSessionSyncConfig.enabled)),
    )
  }

  @Test
  fun `a framework host that names itself in the host SDK header opts out`() {
    for (header in listOf("x-clerk-host-sdk", "X-Clerk-Host-SDK")) {
      val options =
        ClerkConfigurationOptions()
          .withCustomHeaders(mapOf(header to "expo"))
          .withUpdateRequiredViewDisabled()

      assertEquals(DeviceBinding.Reason.FRAMEWORK_HOST, signingOptOut(options))
    }
  }

  @Test
  @OptIn(FrameworkIntegrationApi::class)
  fun `a framework host that signs its own requests opts in`() {
    val options =
      ClerkConfigurationOptions()
        .withCustomHeaders(mapOf("x-clerk-host-sdk" to "expo"))
        .withHostSignsRequests()
        .withUpdateRequiredViewDisabled()

    assertNull(signingOptOut(options))
  }

  @Test
  @OptIn(FrameworkIntegrationApi::class)
  fun `shared session sync stays unbound even when the host signs`() {
    val options =
      ClerkConfigurationOptions(sharedSessionSync = SharedSessionSyncConfig.enabled)
        .withCustomHeaders(mapOf("x-clerk-host-sdk" to "expo"))
        .withHostSignsRequests()

    assertEquals(DeviceBinding.Reason.SHARED_SESSION_SYNC, signingOptOut(options))
  }

  @Test
  fun `a ready key signs`() = runBlocking {
    val holder = DeviceKeyHolder { JvmDeviceKey.getOrCreate() }

    val binding = scope.resolveDeviceBinding(optOut = null, holder = holder)

    assertEquals(DeviceBinding.Signing(holder), binding.current(DEVICE_KEY_WAIT))
  }

  @Test
  fun `a Keystore failure does not sign`() = runBlocking {
    val holder = DeviceKeyHolder { throw KeyStoreException("no Keystore") }

    val binding = scope.resolveDeviceBinding(optOut = null, holder = holder)

    assertEquals(
      DeviceBinding.NotSigning(DeviceBinding.Reason.KEY_UNAVAILABLE),
      binding.current(DEVICE_KEY_WAIT),
    )
  }

  @Test
  fun `a key load error that is not a Keystore failure does not sign and is not retried`() =
    runBlocking {
      for (failure in listOf(IllegalStateException("vendor Keystore bug"), IOException("load"))) {
        var loads = 0
        val holder = DeviceKeyHolder {
          loads += 1
          throw failure
        }

        val binding = scope.resolveDeviceBinding(optOut = null, holder = holder)

        val unavailable = DeviceBinding.NotSigning(DeviceBinding.Reason.KEY_UNAVAILABLE)
        assertEquals(unavailable, binding.current(DEVICE_KEY_WAIT))
        assertEquals(unavailable, binding.current(DEVICE_KEY_WAIT))
        assertEquals(1, loads)
      }
    }

  @Test
  fun `a wait shorter than the key load sees TIMEOUT, later calls do not wait, then Signing once the key is ready`() =
    runBlocking {
      val keyCreated = CountDownLatch(1)
      val holder = DeviceKeyHolder {
        keyCreated.await()
        JvmDeviceKey.getOrCreate()
      }

      val binding = scope.resolveDeviceBinding(optOut = null, holder = holder)

      val timedOut = DeviceBinding.NotSigning(DeviceBinding.Reason.TIMEOUT)
      assertEquals(timedOut, binding.current(50.milliseconds))
      val started = TimeSource.Monotonic.markNow()
      assertEquals(timedOut, binding.current(DEVICE_KEY_WAIT))
      assertTrue(started.elapsedNow() < 50.milliseconds, "waited ${started.elapsedNow()}")
      keyCreated.countDown()
      withTimeout(DEVICE_KEY_WAIT) {
        while (binding.current(DEVICE_KEY_WAIT) != DeviceBinding.Signing(holder)) delay(10)
      }
    }
}
