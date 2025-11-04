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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp32
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import com.clerk.ui.userprofile.common.UserProfileButtonRow

@Composable
internal fun UserProfilePasswordSection(
  modifier: Modifier = Modifier,
  onClick: (PasswordAction) -> Unit,
  isPasswordEnabled: Boolean,
) {
  UserProfilePasswordSectionImpl(
    modifier = modifier,
    onClick = onClick,
    isPasswordEnabled = isPasswordEnabled,
  )
}

@Composable
internal fun UserProfilePasswordSectionImpl(
  modifier: Modifier = Modifier,
  isPasswordEnabled: Boolean,
  onClick: (PasswordAction) -> Unit,
) {
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.background)
          .padding(top = dp32)
    ) {
      Text(
        modifier = Modifier.padding(horizontal = dp24).then(modifier),
        text = stringResource(R.string.password).uppercase(),
        color = ClerkMaterialTheme.colors.mutedForeground,
        style = ClerkMaterialTheme.typography.bodySmall.withMediumWeight(),
      )
      if (isPasswordEnabled) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = dp16).padding(horizontal = dp24)
        ) {
          Icon(
            modifier = Modifier.size(dp24),
            painter = painterResource(R.drawable.ic_lock),
            contentDescription = null,
            tint = ClerkMaterialTheme.colors.mutedForeground,
          )
          Spacers.Horizontal.Spacer16()
          Text(
            text = stringResource(R.string.password_filler),
            style = ClerkMaterialTheme.typography.bodyLarge,
            color = ClerkMaterialTheme.colors.mutedForeground,
          )
        }
        UserProfileButtonRow(
          text = stringResource(R.string.change_password),
          onClick = { onClick(PasswordAction.Reset) },
        )
      } else {
        UserProfileButtonRow(
          text = stringResource(R.string.add_password),
          onClick = { onClick(PasswordAction.Add) },
        )
      }
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
      UserProfilePasswordSectionImpl(isPasswordEnabled = true, onClick = {})
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewAddPassword() {
  Clerk.customTheme = null
  ClerkMaterialTheme {
    Box(
      modifier =
        Modifier.fillMaxWidth().background(color = ClerkMaterialTheme.colors.muted).padding(dp24)
    ) {
      UserProfilePasswordSectionImpl(onClick = {}, isPasswordEnabled = true)
    }
  }
}

enum class PasswordAction {
  Add,
  Reset,
}
