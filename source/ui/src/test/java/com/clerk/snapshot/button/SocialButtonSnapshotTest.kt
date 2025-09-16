package com.clerk.snapshot.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clerk.api.Clerk
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.setLogoUrl
import com.clerk.api.ui.ClerkTheme
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.core.button.social.ClerkSocialButton
import com.clerk.ui.core.button.social.ClerkSocialRow
import com.clerk.ui.core.common.dimens.dp12
import com.clerk.ui.core.common.dimens.dp4
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import kotlinx.collections.immutable.toImmutableList
import org.junit.Test

class SocialButtonSnapshotTest : BaseSnapshotTest() {

  @Test
  fun socialButtonSnapshotTestLight() {
    Clerk.customTheme = null
    val provider = OAuthProvider.GOOGLE
    provider.setLogoUrl(null)
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Column(
          Modifier.fillMaxSize().background(ClerkMaterialTheme.colors.background).padding(dp12),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(dp12, Alignment.CenterVertically),
        ) {
          ClerkSocialButton(provider = provider)
          ClerkSocialButton(provider = provider, isPressed = true)
          ClerkSocialButton(provider = provider, isEnabled = false)
        }
      }
    }
  }

  @Test
  fun socialButtonSnapshotTestDark() {
    Clerk.customTheme = ClerkTheme(colors = DefaultColors.dark)
    val provider = OAuthProvider.GOOGLE
    provider.setLogoUrl(null)
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Column(
          Modifier.fillMaxSize().background(ClerkMaterialTheme.colors.background).padding(dp12),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(dp12, Alignment.CenterVertically),
        ) {
          ClerkSocialButton(provider = provider)
          ClerkSocialButton(provider = provider, isPressed = true)
          ClerkSocialButton(provider = provider, isEnabled = false)
        }
      }
    }
  }

  @Test
  fun socialButtonSnapshotTestDarkShortForm() {
    Clerk.customTheme = ClerkTheme(colors = DefaultColors.dark)
    val provider = OAuthProvider.GOOGLE
    provider.setLogoUrl(null)
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Column(
          Modifier.width(200.dp).background(ClerkMaterialTheme.colors.background).padding(dp12),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(dp12, Alignment.CenterVertically),
        ) {
          ClerkSocialButton(provider = provider)
          ClerkSocialButton(provider = provider, isPressed = true)
          ClerkSocialButton(provider = provider, isEnabled = false)
        }
      }
    }
  }

  @Test
  fun socialButtonSnapshotTestLightShortForm() {
    Clerk.customTheme = ClerkTheme(colors = DefaultColors.light)
    val provider = OAuthProvider.GOOGLE
    provider.setLogoUrl(null)
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Column(
          Modifier.width(200.dp).background(ClerkMaterialTheme.colors.background).padding(dp12),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(dp12, Alignment.CenterVertically),
        ) {
          ClerkSocialButton(provider = provider)
          ClerkSocialButton(provider = provider, isPressed = true)
          ClerkSocialButton(provider = provider, isEnabled = false)
        }
      }
    }
  }

  @Test
  fun socialRowSnapshotTestLight() {
    Clerk.customTheme = ClerkTheme(colors = DefaultColors.light)
    val provider = OAuthProvider.GOOGLE
    provider.setLogoUrl(null)
    paparazzi.snapshot {
      ClerkMaterialTheme {
        ClerkSocialRow(
          providers = listOf(provider, provider, provider, provider, provider).toImmutableList()
        )
      }
    }
  }

  @Test
  fun socialRowSnapshotTestDark() {
    Clerk.customTheme = ClerkTheme(colors = DefaultColors.dark)
    val provider = OAuthProvider.GOOGLE
    provider.setLogoUrl(null)
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Column(
          modifier =
            Modifier.background(color = ClerkMaterialTheme.colors.background)
              .padding(horizontal = dp4)
        ) {
          ClerkSocialRow(
            providers = listOf(provider, provider, provider, provider, provider).toImmutableList()
          )
        }
      }
    }
  }
}
