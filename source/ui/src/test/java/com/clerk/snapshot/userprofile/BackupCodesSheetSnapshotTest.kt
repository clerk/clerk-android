package com.clerk.snapshot.userprofile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.security.BackupCodesSheet
import com.clerk.ui.userprofile.security.BottomSheetCallbacks
import com.clerk.ui.userprofile.security.BottomSheetType
import com.clerk.ui.userprofile.security.MfaType
import org.junit.Test

class BackupCodesSheetSnapshotTest : BaseSnapshotTest() {

  @Test
  fun backupCodesSheetShowsCopyForEachMfaType() {
    MfaType.entries.forEach { mfaType ->
      paparazzi.snapshot(name = mfaType.name) {
        ClerkMaterialTheme {
          Box(modifier = Modifier.size(width = 412.dp, height = 600.dp)) {
            BackupCodesSheet(
              type = BottomSheetType.BackupCodes(listOf("abcd1234", "efgh5678"), mfaType),
              callbacks =
                BottomSheetCallbacks(
                  onClickMfaType = {},
                  onCurrentPasswordEntered = { _, _ -> },
                  onDismiss = {},
                  onError = {},
                  onAddPhoneNumber = {},
                  onNavigateToBackupCodes = { _, _ -> },
                  onVerify = {},
                ),
            )
          }
        }
      }
    }
  }
}
