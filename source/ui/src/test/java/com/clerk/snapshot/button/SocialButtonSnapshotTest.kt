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
import com.clerk.api.OAuthProvider
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.core.button.social.ClerkSocialButton
import com.clerk.ui.core.button.social.ClerkSocialRow
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp4
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkTheme
import com.clerk.ui.theme.DefaultColors
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class SocialButtonSnapshotTest : BaseSnapshotTest() {

  @Test
  fun socialButtonSnapshotTestLight() {
    snapshotTheme = null
    val provider = OAuthProvider.Google
    snapshot {
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
    snapshotTheme = ClerkTheme(colors = DefaultColors.dark)
    val provider = OAuthProvider.Google
    snapshot {
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
    snapshotTheme = ClerkTheme(colors = DefaultColors.dark)
    val provider = OAuthProvider.Google
    snapshot {
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
    snapshotTheme = ClerkTheme(colors = DefaultColors.light)
    val provider = OAuthProvider.Google
    snapshot {
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
    snapshotTheme = ClerkTheme(colors = DefaultColors.light)
    val provider = OAuthProvider.Google
    snapshot {
      ClerkMaterialTheme {
        ClerkSocialRow(
          providers = persistentListOf(provider, provider, provider, provider, provider)
        )
      }
    }
  }

  @Test
  fun socialRowSnapshotTestLightTwoProviders() {
    snapshotTheme = ClerkTheme(colors = DefaultColors.light)
    val provider = OAuthProvider.Google
    snapshot {
      ClerkMaterialTheme {
        ClerkSocialRow(providers = persistentListOf(provider, provider))
      }
    }
  }

  @Test
  fun socialRowSnapshotTestLightThreeProviders() {
    snapshotTheme = ClerkTheme(colors = DefaultColors.light)
    val provider = OAuthProvider.Google
    snapshot {
      ClerkMaterialTheme {
        ClerkSocialRow(providers = persistentListOf(provider, provider, provider))
      }
    }
  }

  @Test
  fun socialRowSnapshotTestLightFourProviders() {
    snapshotTheme = ClerkTheme(colors = DefaultColors.light)
    val provider = OAuthProvider.Google
    snapshot {
      ClerkMaterialTheme {
        ClerkSocialRow(providers = persistentListOf(provider, provider, provider, provider))
      }
    }
  }

  @Test
  fun socialRowSnapshotTestDark() {
    snapshotTheme = ClerkTheme(colors = DefaultColors.dark)
    val provider = OAuthProvider.Google
    snapshot {
      ClerkMaterialTheme {
        Column(
          modifier =
            Modifier.background(color = ClerkMaterialTheme.colors.background)
              .padding(horizontal = dp4)
        ) {
          ClerkSocialRow(
            providers = persistentListOf(provider, provider, provider, provider, provider)
          )
        }
      }
    }
  }
}
