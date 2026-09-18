package com.clerk.snapshot.sessiontask

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clerk.api.Clerk
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.sessiontask.mfa.SessionTaskMfaView
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Test

class SessionTaskMfaViewSnapshotTest : BaseSnapshotTest() {

  @After
  fun tearDownMocks() {
    unmockkObject(Clerk)
  }

  @Test
  fun forcedMfaMethodPickerHasNoBackButton() {
    mockkObject(Clerk)
    every { Clerk.mfaPhoneCodeIsEnabled } returns true
    every { Clerk.mfaAuthenticatorAppIsEnabled } returns true

    paparazzi.snapshot {
      Box(Modifier.size(width = 390.dp, height = 640.dp)) {
        SessionTaskMfaView(onAuthComplete = {})
      }
    }
  }
}
