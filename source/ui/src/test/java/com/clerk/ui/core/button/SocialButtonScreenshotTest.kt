package com.clerk.ui.core.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.clerk.api.Clerk
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.setLogoUrl
import com.clerk.api.ui.ClerkTheme
import com.clerk.base.BaseScreenshotTest
import com.clerk.ui.core.button.social.SocialButton
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import org.junit.Test

class SocialButtonScreenshotTest : BaseScreenshotTest() {

  @Test
  fun socialButtonScreenshotTestLight() {
    Clerk.customTheme = null
    val provider = OAuthProvider.GOOGLE
    provider.setLogoUrl(null)
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Column(
          Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(dp12),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(dp12, Alignment.CenterVertically),
        ) {
          SocialButton(provider = provider)
          SocialButton(provider = provider, isPressed = true)
          SocialButton(provider = provider, isEnabled = false)
        }
      }
    }
  }

  @Test
  fun socialButtonScreenshotTestDark() {
    Clerk.customTheme = ClerkTheme(colors = DefaultColors.darkColors)
    val provider = OAuthProvider.GOOGLE
    provider.setLogoUrl(null)
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Column(
          Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(dp12),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(dp12, Alignment.CenterVertically),
        ) {
          SocialButton(provider = provider)
          SocialButton(provider = provider, isPressed = true)
          SocialButton(provider = provider, isEnabled = false)
        }
      }
    }
  }
}
