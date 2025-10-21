package com.clerk.ui.userprofile.update

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.ui.R
import com.clerk.ui.core.avatar.AvatarSize
import com.clerk.ui.core.avatar.AvatarType
import com.clerk.ui.core.avatar.AvatarView
import com.clerk.ui.core.dimens.dp0
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.account.UserProfileAccountActionRow

@Composable
internal fun UserProfileUpdateProfileView(modifier: Modifier = Modifier) {
  UserProfileUpdateProfileViewImpl(modifier = modifier)
}

@Composable
private fun UserProfileUpdateProfileViewImpl(
  modifier: Modifier = Modifier,
  viewModel: UpdateProfileViewModel = viewModel(),
) {
  val user by viewModel.user.collectAsState()
  val state by viewModel.state.collectAsState()
  val errorMessage = (state as? UpdateProfileViewModel.State.Error)?.message

  ClerkMaterialTheme {
    ClerkThemedProfileScaffold(
      modifier = modifier,
      title = stringResource(R.string.account),
      backgroundColor = ClerkMaterialTheme.colors.muted,
      hasBackButton = true,
      horizontalPadding = dp0,
      onBackPressed = {},
      errorMessage = errorMessage,
      content = {
        Box(modifier = Modifier.fillMaxWidth()) {
          AvatarView(
            hasEditButton = true,
            modifier = Modifier.align(Alignment.Center),
            size = AvatarSize.X_LARGE,
            shape = CircleShape,
            avatarType = AvatarType.USER,
            imageUrl = user?.imageUrl,
            onEditTakePhoto = {},
            onEditChoosePhoto = {},
            onEditRemovePhoto = { viewModel.removeProfileImage() },
          )
        }
        Spacers.Vertical.Spacer12()
      },
      bottomContent = {
        HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
        UserProfileAccountActionRow(
          backgroundColor = ClerkMaterialTheme.colors.background,
          iconResId = R.drawable.ic_sign,
          text = stringResource(R.string.log_out),
          onClick = {},
        )
        HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
      },
    )
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileUpdateProfileViewImpl()
}
