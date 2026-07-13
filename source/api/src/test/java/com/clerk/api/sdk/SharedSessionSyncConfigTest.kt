package com.clerk.api.sdk

import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.SharedSessionSyncConfig
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SharedSessionSyncConfigTest {
  @Test
  fun `shared session sync is opt in`() {
    assertNull(ClerkConfigurationOptions().sharedSessionSync)
    assertSame(
      SharedSessionSyncConfig.enabled,
      ClerkConfigurationOptions(sharedSessionSync = SharedSessionSyncConfig.enabled)
        .sharedSessionSync,
    )
  }
}
