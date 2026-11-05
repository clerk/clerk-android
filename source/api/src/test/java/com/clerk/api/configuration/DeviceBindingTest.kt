package com.clerk.api.configuration

import com.clerk.api.devicekey.DeviceKeyHolder
import com.clerk.api.devicekey.JvmDeviceKey
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
  fun `an undocumented Keystore exception does not sign`() = runBlocking {
    val holder = DeviceKeyHolder { throw IllegalStateException("vendor Keystore bug") }

    val binding = scope.resolveDeviceBinding(sharedSessionSync = false, holder = holder)

    assertEquals(DeviceBinding.NotSigning(DeviceBinding.Reason.KEY_UNAVAILABLE), binding.await())
  }

  @Test
  fun `a key that is not ready in time does not sign`() = runBlocking {
    val keyCreated = CountDownLatch(1)
    val holder = DeviceKeyHolder {
      keyCreated.await()
      JvmDeviceKey.getOrCreate()
    }

    val binding =
      scope.resolveDeviceBinding(
        sharedSessionSync = false,
        holder = holder,
        keyTimeout = 50.milliseconds,
      )

    assertEquals(DeviceBinding.NotSigning(DeviceBinding.Reason.TIMEOUT), binding.await())
    keyCreated.countDown()
  }
}
