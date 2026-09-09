package com.clerk.snapshot.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.composition.LocalClerkLogoContent
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.theme.ClerkDesign
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkTheme
import org.junit.Test

class ClerkTopAppBarSnapshotTest : BaseSnapshotTest() {
  @Test
  fun authTopBar_preservesWideLogoAspectRatio() {
    snapshot {
      Box(Modifier.size(width = 360.dp, height = 72.dp)) {
        ClerkMaterialTheme {
          ClerkTopAppBar(
            onBackPressed = {},
            hasLogo = true,
            hasBackButton = true,
            logoUrl = WIDE_LOGO_DATA_URI,
          )
        }
      }
    }
  }

  @Test
  fun authTopBar_respectsLogoMaxHeightOverride() {
    snapshot {
      Box(Modifier.size(width = 360.dp, height = 96.dp)) {
        ClerkMaterialTheme {
          ClerkTopAppBar(
            onBackPressed = {},
            hasLogo = true,
            hasBackButton = true,
            // Placeholder renders at the configured logo height, keeping this deterministic.
            logoUrl = null,
            clerkTheme = ClerkTheme(design = ClerkDesign(logoMaxHeight = 24.dp)),
          )
        }
      }
    }
  }

  @Test
  fun authTopBar_rendersCustomLogoWithoutSdkSizing() {
    snapshot {
      Box(Modifier.size(width = 360.dp, height = 96.dp)) {
        ClerkMaterialTheme {
          CompositionLocalProvider(
            LocalClerkLogoContent provides
              {
                Box(Modifier.size(width = 144.dp, height = 44.dp).background(Color.Red))
              }
          ) {
            ClerkTopAppBar(
              onBackPressed = {},
              hasLogo = true,
              hasBackButton = true,
              logoUrl = WIDE_LOGO_DATA_URI,
            )
          }
        }
      }
    }
  }

  @Test
  fun profileTopBar_placesTrailingActionUsingAndroidSpacing() {
    snapshot {
      Box(Modifier.size(width = 412.dp, height = 72.dp)) {
        ClerkMaterialTheme {
          ClerkTopAppBar(
            onBackPressed = {},
            hasLogo = false,
            title = "Members",
            trailingContent = {
              Text(
                text = "Invite",
                style = ClerkMaterialTheme.typography.bodyLarge.withMediumWeight(),
                color = ClerkMaterialTheme.colors.primary,
              )
            },
          )
        }
      }
    }
  }

  private companion object {
    private const val WIDE_LOGO_DATA_URI =
      "data:image/svg+xml;utf8," +
        "<svg xmlns='http://www.w3.org/2000/svg' width='120' height='24' viewBox='0 0 120 24'>" +
        "<rect width='120' height='24' rx='4' fill='%2308163d'/>" +
        "<rect x='6' y='4' width='16' height='16' rx='2' fill='%2391d43b'/>" +
        "<rect x='28' y='6' width='78' height='12' rx='3' fill='%23ffffff'/>" +
        "</svg>"
  }
}
