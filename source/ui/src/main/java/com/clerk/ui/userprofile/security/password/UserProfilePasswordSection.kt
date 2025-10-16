package com.clerk.ui.userprofile.security.password

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp32
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import com.clerk.ui.userprofile.common.UserProfileButtonRow

@Composable
fun UserProfilePasswordSection(modifier: Modifier = Modifier) {
  UserProfilePasswordSectionImpl(modifier = modifier)
}

@Composable
private fun UserProfilePasswordSectionImpl(modifier: Modifier = Modifier) {
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.background)
          .padding(top = dp32)
    ) {
      Text(
        modifier = Modifier.padding(horizontal = dp16).padding(bottom = dp16).then(modifier),
        text = stringResource(R.string.password).uppercase(),
        color = ClerkMaterialTheme.colors.mutedForeground,
        style = ClerkMaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
      )
      Row(modifier = Modifier.fillMaxWidth().padding(dp16)) {
        Icon(
          modifier = Modifier.size(dp24),
          painter = painterResource(R.drawable.ic_lock),
          contentDescription = null,
          tint = ClerkMaterialTheme.colors.mutedForeground,
        )
        Spacers.Horizontal.Spacer16()
        Text(
          text = "•••••••••••••••••••••••••",
          style = ClerkMaterialTheme.typography.bodyLarge,
          color = ClerkMaterialTheme.colors.mutedForeground,
        )
      }
      UserProfileButtonRow(modifier = Modifier.padding(vertical = dp8), text = "Change password") {}
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
  ClerkMaterialTheme {
    Box(
      modifier =
        Modifier.fillMaxWidth().background(color = ClerkMaterialTheme.colors.muted).padding(dp24)
    ) {
      UserProfilePasswordSectionImpl()
    }
  }
}

internal enum class PasswordAction {
  Add,
  Reset,
}
