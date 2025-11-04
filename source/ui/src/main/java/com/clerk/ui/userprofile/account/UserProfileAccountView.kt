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
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun UserProfileAccountView(
  onClick: (UserProfileAction) -> Unit,
  onBackPressed: () -> Unit,
  modifier: Modifier = Modifier,
  onClickEdit: () -> Unit,
) {

  UserProfileAccountViewImpl(
    modifier = modifier,
    imageUrl = Clerk.user?.imageUrl,
    userFullName = Clerk.user?.fullName(),
    username = Clerk.user?.username,
    onClick = onClick,
    onBackPressed = onBackPressed,
    onEditAvatarClick = onClickEdit,
  )
}

@Composable
private fun UserProfileAccountViewImpl(
  userFullName: String?,
  username: String?,
  onClick: (UserProfileAction) -> Unit,
  onBackPressed: () -> Unit,
  modifier: Modifier = Modifier,
  imageUrl: String? = null,
  viewModel: UserProfileAccountViewModel = viewModel(),
  onEditAvatarClick: () -> Unit,
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
        Spacers.Vertical.Spacer32()
        AvatarHeaderView(
          userFullName = userFullName,
          username = username,
          imageUrl = imageUrl,
          onClickEdit = onEditAvatarClick,
        )
        MainProfileActions(onClick = onClick)
      },
      bottomContent = {
        HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
        UserProfileIconActionRow(
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
  username: String?,
  imageUrl: String?,
  mode: AvatarMode = AvatarMode.VIEW,
  onClickEdit: () -> Unit,
) {
  val name = userFullName?.takeIf { it.isNotBlank() }
  val uname = username?.takeIf { it.isNotBlank() }

  Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    AvatarView(
      size = AvatarSize.X_LARGE,
      shape = CircleShape,
      avatarType = AvatarType.USER,
      imageUrl = imageUrl,
    )
    Spacers.Vertical.Spacer12()

    if (mode == AvatarMode.VIEW) {
      if (name != null) {
        // Show full name as title
        Text(
          text = name,
          style = ClerkMaterialTheme.typography.titleLarge.withMediumWeight(),
          color = ClerkMaterialTheme.colors.foreground,
        )
        // If username exists, show it under the full name (current behavior)
        if (uname != null) {
          Spacers.Vertical.Spacer4()
          Text(
            text = uname,
            style = ClerkMaterialTheme.typography.bodyMedium,
            color = ClerkMaterialTheme.colors.mutedForeground,
          )
        }
      } else if (uname != null) {
        // No full name: promote username to the same style as full name
        Text(
          text = uname,
          style = ClerkMaterialTheme.typography.titleLarge.withMediumWeight(),
          color = ClerkMaterialTheme.colors.foreground,
        )
      }

      Spacers.Vertical.Spacer8()
      ClerkButton(
        modifier = Modifier.defaultMinSize(minWidth = 120.dp),
        text = stringResource(R.string.update_profile),
        onClick = onClickEdit,
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
    UserProfileIconActionRow(
      iconResId = R.drawable.ic_user,
      text = stringResource(R.string.profile),
      onClick = { onClick(UserProfileAction.Profile) },
    )
    HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
    UserProfileIconActionRow(
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
    UserProfileAccountViewImpl(
      userFullName = "Cameron Walker",
      username = "cameronw",
      onClick = {},
      onBackPressed = {},
      onEditAvatarClick = {},
    )
  }
}
