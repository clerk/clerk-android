package com.clerk.api.configuration

import com.clerk.api.devicekey.DeviceKeyHolder
import com.clerk.api.devicekey.JvmDeviceKey
import java.io.IOException
import java.security.KeyStoreException
import java.util.concurrent.CountDownLatch
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
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
  fun `shared session sync does not sign and never loads the key`() = runBlocking {
    var loads = 0
    val holder = DeviceKeyHolder {
      loads += 1
      JvmDeviceKey.getOrCreate()
    }

    val binding = scope.resolveDeviceBinding(sharedSessionSync = true, holder = holder)

    assertEquals(
      DeviceBinding.NotSigning(DeviceBinding.Reason.SHARED_SESSION_SYNC),
      binding.await(),
    )
    assertEquals(0, loads)
  }

  @Test
  fun `a ready key signs`() = runBlocking {
    val holder = DeviceKeyHolder { JvmDeviceKey.getOrCreate() }

    val binding = scope.resolveDeviceBinding(sharedSessionSync = false, holder = holder)

    assertEquals(DeviceBinding.Signing(holder), binding.await())
  }

  @Test
  fun `a Keystore failure does not sign`() = runBlocking {
    val holder = DeviceKeyHolder { throw KeyStoreException("no Keystore") }

    val binding = scope.resolveDeviceBinding(sharedSessionSync = false, holder = holder)

    assertEquals(DeviceBinding.NotSigning(DeviceBinding.Reason.KEY_UNAVAILABLE), binding.await())
  }

  @Test
  fun `a key load error that is not a Keystore failure keeps signing, so each request retries it`() =
    runBlocking {
      for (failure in listOf(IllegalStateException("vendor Keystore bug"), IOException("load"))) {
        val holder = DeviceKeyHolder { throw failure }

        val binding = scope.resolveDeviceBinding(sharedSessionSync = false, holder = holder)

        assertEquals(DeviceBinding.Signing(holder), binding.await())
      }
    }

  @Test
  fun `a wait shorter than the key load sees TIMEOUT each time, then Signing once the key is ready`() =
    runBlocking {
      val keyCreated = CountDownLatch(1)
      val holder = DeviceKeyHolder {
        keyCreated.await()
        JvmDeviceKey.getOrCreate()
      }

      val binding = scope.resolveDeviceBinding(sharedSessionSync = false, holder = holder)

      val timedOut = DeviceBinding.NotSigning(DeviceBinding.Reason.TIMEOUT)
      assertEquals(timedOut, binding.awaitWithin(50.milliseconds))
      assertEquals(timedOut, binding.awaitWithin(50.milliseconds))
      keyCreated.countDown()
      assertEquals(DeviceBinding.Signing(holder), binding.awaitWithin(DEVICE_KEY_WAIT))
    }
}
