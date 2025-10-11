package com.clerk.snapshot.userprofile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import com.clerk.ui.userprofile.BackupCodesView
import com.clerk.ui.userprofile.MfaType
import kotlinx.collections.immutable.toImmutableList
import org.junit.Test

class BackupCodeViewSnapshotTest : BaseSnapshotTest() {

  @Test
  fun backupCodeViewSnapshotTestLight() {
    paparazzi.snapshot {
      Box(modifier = Modifier.height(700.dp)) {
        ClerkMaterialTheme {
          BackupCodesView(
            mfaType = MfaType.AuthenticatorApp,
            codes =
              listOf(
                  "jsdwz752",
                  "abxkq983",
                  "abxkq983",
                  "mpltk294",
                  "mpltk294",
                  "qwert678",
                  "dj2b5ugx",
                  "xyztj501",
                  "qwert678",
                  "4nb52vql",
                )
                .toImmutableList(),
          )
        }
      }
    }
  }

  @Test
  fun backupCodeViewSnapshotTestDark() {
    paparazzi.snapshot {
      Clerk.customTheme = ClerkTheme(colors = DefaultColors.dark)
      Box(modifier = Modifier.height(700.dp)) {
        ClerkMaterialTheme {
          BackupCodesView(
            mfaType = MfaType.AuthenticatorApp,
            codes =
              listOf(
                  "jsdwz752",
                  "abxkq983",
                  "abxkq983",
                  "mpltk294",
                  "mpltk294",
                  "qwert678",
                  "dj2b5ugx",
                  "xyztj501",
                  "qwert678",
                  "4nb52vql",
                )
                .toImmutableList(),
          )
        }
      }
    }
  }
}
