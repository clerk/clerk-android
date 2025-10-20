package com.clerk.ui.userprofile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.clerk.api.Clerk
import com.clerk.api.user.fullName
import com.clerk.ui.R
import com.clerk.ui.core.avatar.AvatarSize
import com.clerk.ui.core.avatar.AvatarType
import com.clerk.ui.core.avatar.AvatarView
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun UserProfileAccountView(modifier: Modifier = Modifier) {
  UserProfileAccountViewImpl(
    modifier = modifier,
    imageUrl = Clerk.user?.imageUrl,
    userFullName = Clerk.user?.fullName(),
  )
}

@Composable
private fun UserProfileAccountViewImpl(
  userFullName: String?,
  modifier: Modifier = Modifier,
  imageUrl: String? = null,
) {
  ClerkThemedProfileScaffold(
    modifier = modifier,
    title = stringResource(R.string.account),
    backgroundColor = ClerkMaterialTheme.colors.muted,
    hasBackButton = true,
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      AvatarView(
        modifier = Modifier.align(Alignment.Center),
        size = AvatarSize.X_LARGE,
        shape = CircleShape,
        avatarType = AvatarType.USER,
        imageUrl = imageUrl,
      )
    }
    Spacers.Vertical.Spacer32()
    userFullName?.let {
      Text(
        text = it,
        style = ClerkMaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
        color = ClerkMaterialTheme.colors.foreground,
      )
    }
    Spacers.Vertical.Spacer8()
    ClerkButton(
      modifier = Modifier.defaultMinSize(minWidth = 120.dp),
      text = stringResource(R.string.update_profile),
      onClick = {},
      configuration =
        ClerkButtonDefaults.configuration(
          style = ClerkButtonConfiguration.ButtonStyle.Secondary,
          emphasis = ClerkButtonConfiguration.Emphasis.High,
        ),
    )
    Spacers.Vertical.Spacer32()
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme { UserProfileAccountViewImpl(userFullName = "Cameron Walker") }
}
