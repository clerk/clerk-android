package com.clerk.api.devicekey

import java.security.KeyStoreException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceKeyHolderDeleteTest {
  @Test
  fun `deleting a ready key makes the next use create a new one`() {
    val holder = DeviceKeyHolder { JvmDeviceKey.getOrCreate() }
    val before = holder.thumbprint()

    assertTrue(holder.deleteKey(JvmDeviceKey::deleteStored).isSuccess)

    assertNotEquals(before, holder.thumbprint())
  }

  @Test
  fun `deleting an unavailable key makes the next use load again`() {
    var keystoreFails = true
    val holder = DeviceKeyHolder {
      if (keystoreFails) throw KeyStoreException("Keystore busy") else JvmDeviceKey.getOrCreate()
    }
    assertIs<DeviceKeyState.Unavailable>(holder.state())
    keystoreFails = false

    assertTrue(holder.deleteKey(JvmDeviceKey::deleteStored).isSuccess)

    assertIs<DeviceKeyState.Ready>(holder.state())
  }

  @Test
  fun `a delete the Keystore refuses keeps the key`() {
    val holder = DeviceKeyHolder { JvmDeviceKey.getOrCreate() }
    val before = holder.thumbprint()

    assertTrue(holder.deleteKey { throw KeyStoreException("Keystore busy") }.isFailure)

    assertEquals(before, holder.thumbprint())
  }

  private fun DeviceKeyHolder.thumbprint(): String = withKey { it.thumbprint }.getOrThrow()
}
