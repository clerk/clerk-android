package com.clerk.snapshot.updaterequired

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.clerk.api.Clerk
import com.clerk.api.appversion.AppVersionSupportStatus
import com.clerk.api.ui.ClerkTheme
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import com.clerk.ui.updaterequired.UpdateRequiredContent
import org.junit.Test

class UpdateRequiredViewSnapshotTest : BaseSnapshotTest() {

  @Test
  fun updateRequired() {
    snapshot(
      AppVersionSupportStatus(
        isSupported = false,
        currentVersion = 41,
        minimumVersion = 42,
        updateUrl = "https://play.google.com/store/apps/details?id=com.example.app",
      ),
      versionName = "4.8.4",
      supportEmail = "support@example.com",
    )
  }

  @Test
  fun updateRequiredWithoutStoreLinkOrSupportEmail() {
    snapshot(
      AppVersionSupportStatus(isSupported = false, currentVersion = 41),
      versionName = null,
      supportEmail = null,
    )
  }

  private fun snapshot(
    status: AppVersionSupportStatus,
    versionName: String?,
    supportEmail: String?,
  ) {
    paparazzi.snapshot {
      Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
      Box(Modifier.size(740.dp)) {
        ClerkMaterialTheme {
          UpdateRequiredContent(
            status = status,
            appName = "Acme",
            versionName = versionName,
            supportEmail = supportEmail,
            modifier = Modifier.fillMaxSize(),
            logo = {
              Box(
                Modifier.size(44.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(ClerkMaterialTheme.colors.primary)
              )
            },
          )
        }
      }
    }
  }
}
