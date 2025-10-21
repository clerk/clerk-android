package com.clerk.ui.userprofile.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.user.fullName
import com.clerk.ui.R
import com.clerk.ui.core.avatar.AvatarSize
import com.clerk.ui.core.avatar.AvatarType
import com.clerk.ui.core.avatar.AvatarView
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.dimens.dp0
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun UserProfileAccountView(
  onClick: (UserProfileAction) -> Unit,
  modifier: Modifier = Modifier,
  onBackPressed: () -> Unit,
) {

  UserProfileAccountViewImpl(
    modifier = modifier,
    imageUrl = Clerk.user?.imageUrl,
    userFullName = Clerk.user?.fullName(),
    onClick = onClick,
    onBackPressed = onBackPressed,
  )
}

@Composable
private fun UserProfileAccountViewImpl(
  userFullName: String?,
  onClick: (UserProfileAction) -> Unit,
  onBackPressed: () -> Unit,
  modifier: Modifier = Modifier,
  imageUrl: String? = null,
  viewModel: UserProfileAccountViewModel = viewModel(),
) {
  ClerkMaterialTheme {
    ClerkThemedProfileScaffold(
      modifier = modifier,
      title = stringResource(R.string.account),
      backgroundColor = ClerkMaterialTheme.colors.muted,
      hasBackButton = true,
      horizontalPadding = dp0,
      onBackPressed = onBackPressed,
      content = {
        AvatarHeaderView(userFullName = userFullName, imageUrl = imageUrl)
        MainProfileActions(onClick = onClick)
      },
      bottomContent = {
        HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
        UserProfileAccountActionRow(
          backgroundColor = ClerkMaterialTheme.colors.background,
          iconResId = R.drawable.ic_sign,
          text = stringResource(R.string.log_out),
          onClick = { viewModel.signOut() },
        )
        HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
      },
    )
  }
}

@Composable
private fun AvatarHeaderView(
  userFullName: String?,
  imageUrl: String?,
  mode: AvatarMode = AvatarMode.VIEW,
) {
  Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    AvatarView(
      size = AvatarSize.X_LARGE,
      shape = CircleShape,
      avatarType = AvatarType.USER,
      imageUrl = imageUrl,
    )
    Spacers.Vertical.Spacer12()
    if (mode == AvatarMode.VIEW) {
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
    }

    Spacers.Vertical.Spacer32()
    HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
  }
}

enum class AvatarMode {
  VIEW,
  EDIT,
}

@Composable
private fun MainProfileActions(onClick: (UserProfileAction) -> Unit) {
  Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    UserProfileAccountActionRow(
      iconResId = R.drawable.ic_user,
      text = stringResource(R.string.profile),
      onClick = { onClick(UserProfileAction.Profile) },
    )
    HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
    UserProfileAccountActionRow(
      iconResId = R.drawable.ic_lock,
      text = stringResource(R.string.security),
      onClick = { onClick(UserProfileAction.Security) },
    )
    HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
  }
}

internal enum class UserProfileAction {
  Profile,
  Security,
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    UserProfileAccountViewImpl(userFullName = "Cameron Walker", onClick = {}, onBackPressed = {})
  }
}
