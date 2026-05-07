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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.clerk.ui.userprofile.custom.CustomRowView
import com.clerk.ui.userprofile.custom.UserProfileCustomRow
import com.clerk.ui.userprofile.custom.UserProfileListRow
import com.clerk.ui.userprofile.custom.UserProfileRow
import com.clerk.ui.userprofile.custom.UserProfileSection
import com.clerk.ui.userprofile.custom.buildRenderedRows
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun UserProfileAccountView(
  onClick: (UserProfileAction) -> Unit,
  onBackPressed: () -> Unit,
  onClickEdit: () -> Unit,
  modifier: Modifier = Modifier,
  customRows: ImmutableList<UserProfileCustomRow> = persistentListOf(),
  onCustomRowClick: (routeKey: String) -> Unit = {},
) {
  val sessions = Clerk.sessionsFlow.collectAsStateWithLifecycle().value

  UserProfileAccountViewImpl(
    modifier = modifier,
    imageUrl = Clerk.user?.imageUrl,
    userFullName = Clerk.user?.fullName(),
    username = Clerk.user?.username,
    multiSessionModeIsEnabled = Clerk.multiSessionModeIsEnabled,
    sessionCount = sessions.size,
    onClick = onClick,
    onBackPressed = onBackPressed,
    onEditAvatarClick = onClickEdit,
    customRows = customRows,
    onCustomRowClick = onCustomRowClick,
  )
}

@Composable
@Suppress("LongParameterList")
private fun UserProfileAccountViewImpl(
  userFullName: String?,
  username: String?,
  multiSessionModeIsEnabled: Boolean,
  sessionCount: Int,
  onClick: (UserProfileAction) -> Unit,
  onBackPressed: () -> Unit,
  onEditAvatarClick: () -> Unit,
  modifier: Modifier = Modifier,
  imageUrl: String? = null,
  viewModel: UserProfileAccountViewModel = viewModel(),
  customRows: ImmutableList<UserProfileCustomRow> = persistentListOf(),
  onCustomRowClick: (routeKey: String) -> Unit = {},
) {
  val handleAccountClick: (UserProfileAction) -> Unit = { action ->
    if (action == UserProfileAction.SignOut) {
      viewModel.signOut()
    }
    onClick(action)
  }

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
        ProfileSectionRows(
          onClick = onClick,
          customRows = customRows,
          onCustomRowClick = onCustomRowClick,
        )
      },
      bottomContent = {
        AccountSectionRows(
          multiSessionModeIsEnabled = multiSessionModeIsEnabled,
          sessionCount = sessionCount,
          onClick = handleAccountClick,
          customRows = customRows,
          onCustomRowClick = onCustomRowClick,
        )
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
        Text(
          text = name,
          style = ClerkMaterialTheme.typography.titleLarge.withMediumWeight(),
          color = ClerkMaterialTheme.colors.foreground,
        )
        if (uname != null) {
          Spacers.Vertical.Spacer4()
          Text(
            text = uname,
            style = ClerkMaterialTheme.typography.bodyMedium,
            color = ClerkMaterialTheme.colors.mutedForeground,
          )
        }
      } else if (uname != null) {
        Text(
          text = uname,
          style = ClerkMaterialTheme.typography.titleLarge.withMediumWeight(),
          color = ClerkMaterialTheme.colors.foreground,
        )
      }

      Spacers.Vertical.Spacer8()
      ClerkButton(
        modifier = Modifier.defaultMinSize(minWidth = 120.dp),
        text = stringResource(R.string.edit_profile),
        onClick = onClickEdit,
        isEnabled = true,
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

internal enum class AvatarMode {
  VIEW,
  EDIT,
}

@Composable
private fun ProfileSectionRows(
  onClick: (UserProfileAction) -> Unit,
  customRows: ImmutableList<UserProfileCustomRow>,
  onCustomRowClick: (routeKey: String) -> Unit,
) {
  val rows =
    buildRenderedRows(
      builtInRows = listOf(UserProfileRow.ManageAccount, UserProfileRow.Security),
      section = UserProfileSection.Profile,
      customRows = customRows,
    )
  Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    rows.forEach { row ->
      when (row) {
        is UserProfileListRow.BuiltIn ->
          when (row.row) {
            UserProfileRow.ManageAccount ->
              UserProfileIconActionRow(
                iconResId = R.drawable.ic_user,
                text = stringResource(R.string.manage_account),
                onClick = { onClick(UserProfileAction.Profile) },
              )
            UserProfileRow.Security ->
              UserProfileIconActionRow(
                iconResId = R.drawable.ic_lock,
                text = stringResource(R.string.security),
                onClick = { onClick(UserProfileAction.Security) },
              )
            UserProfileRow.SwitchAccount,
            UserProfileRow.AddAccount,
            UserProfileRow.SignOut -> {} // Handled in account section
          }
        is UserProfileListRow.Custom ->
          CustomRowView(
            customRow = row.customRow,
            onClick = { onCustomRowClick(row.customRow.routeKey) },
          )
      }
      HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
    }
  }
}

@Composable
private fun AccountSectionRows(
  multiSessionModeIsEnabled: Boolean,
  sessionCount: Int,
  onClick: (UserProfileAction) -> Unit,
  customRows: ImmutableList<UserProfileCustomRow>,
  onCustomRowClick: (routeKey: String) -> Unit,
) {
  val builtInRows = buildList {
    if (multiSessionModeIsEnabled) {
      if (sessionCount > 1) {
        add(UserProfileRow.SwitchAccount)
      }
      add(UserProfileRow.AddAccount)
    }
    add(UserProfileRow.SignOut)
  }
  val rows =
    buildRenderedRows(
      builtInRows = builtInRows,
      section = UserProfileSection.Account,
      customRows = customRows,
    )
  rows.forEach { row ->
    HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
    when (row) {
      is UserProfileListRow.BuiltIn ->
        when (row.row) {
          UserProfileRow.SwitchAccount ->
            UserProfileIconActionRow(
              backgroundColor = ClerkMaterialTheme.colors.background,
              iconResId = R.drawable.ic_switch,
              text = stringResource(R.string.switch_account),
              onClick = { onClick(UserProfileAction.SwitchAccount) },
            )
          UserProfileRow.AddAccount ->
            UserProfileIconActionRow(
              backgroundColor = ClerkMaterialTheme.colors.background,
              iconResId = R.drawable.ic_plus,
              text = stringResource(R.string.add_account),
              onClick = { onClick(UserProfileAction.AddAccount) },
            )
          UserProfileRow.SignOut ->
            UserProfileIconActionRow(
              backgroundColor = ClerkMaterialTheme.colors.background,
              iconResId = R.drawable.ic_sign,
              text = stringResource(R.string.sign_out),
              onClick = { onClick(UserProfileAction.SignOut) },
            )
          UserProfileRow.ManageAccount,
          UserProfileRow.Security -> Unit
        }
      is UserProfileListRow.Custom ->
        CustomRowView(
          customRow = row.customRow,
          onClick = { onCustomRowClick(row.customRow.routeKey) },
        )
    }
  }
  HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
}

internal enum class UserProfileAction {
  Profile,
  Security,
  SwitchAccount,
  AddAccount,
  SignOut,
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    UserProfileAccountViewImpl(
      userFullName = "Cameron Walker",
      username = "cameronw",
      multiSessionModeIsEnabled = true,
      sessionCount = 2,
      onClick = {},
      onBackPressed = {},
      onEditAvatarClick = {},
    )
  }
}
