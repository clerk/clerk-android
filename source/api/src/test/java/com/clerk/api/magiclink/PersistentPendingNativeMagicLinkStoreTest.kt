package com.clerk.api.magiclink

import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PersistentPendingNativeMagicLinkStoreTest {
  @Before
  fun setup() {
    StorageHelper.initialize(RuntimeEnvironment.getApplication())
    StorageHelper.reset(RuntimeEnvironment.getApplication())
  }

  @After
  fun tearDown() {
    StorageHelper.reset(RuntimeEnvironment.getApplication())
  }

  @Test
  fun `save persists flow across store instances`() {
    val flow =
      PendingNativeMagicLinkFlow(
        codeVerifier = "verifier_123",
        state = PendingNativeMagicLinkState.SIGN_IN,
        createdAtEpochMs = 100L,
        expiresAtEpochMs = 200L,
        flowId = "flow_123",
      )

    PersistentPendingNativeMagicLinkStore().save(flow)

    val restored = PersistentPendingNativeMagicLinkStore().load()

    assertEquals(flow, restored)
  }

  @Test
  fun `load clears corrupted payload`() {
    StorageHelper.saveValue(StorageKey.PENDING_NATIVE_MAGIC_LINK_FLOW, "{bad json")

    val restored = PersistentPendingNativeMagicLinkStore().load()

    assertNull(restored)
    assertNull(StorageHelper.loadValue(StorageKey.PENDING_NATIVE_MAGIC_LINK_FLOW))
  }
}
