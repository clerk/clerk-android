package com.clerk.api.storage

import java.security.KeyStoreException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowBuild

@RunWith(RobolectricTestRunner::class)
class StorageCipherFactoryTest {
  @Test
  fun `device without the Android keystore refuses to fall back to a derived key`() {
    ShadowBuild.setFingerprint("google/oriole/oriole:14/UQ1A.240205.004/11269751:user/release-keys")

    assertThrows(KeyStoreException::class.java) { StorageCipherFactory.create("test.master_key") }
  }

  @Test
  fun `robolectric cipher round-trips a value it encrypted`() {
    val cipher = StorageCipherFactory.create("test.master_key")

    val ciphertext = cipher.encrypt("session-token")

    assertNotEquals("session-token", ciphertext)
    assertEquals("session-token", cipher.decrypt(ciphertext))
  }
}
