package com.clerk.ui.organizationprofile.root

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clerk.api.organizations.Organization
import com.clerk.ui.R
import com.clerk.ui.core.avatar.AvatarSize
import com.clerk.ui.core.avatar.AvatarType
import com.clerk.ui.core.avatar.AvatarView
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun OrganizationProfileHeaderView(
  organization: Organization,
  showsUpdateProfile: Boolean,
  onUpdateProfile: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    AvatarView(
      size = AvatarSize.X_LARGE,
      shape = ClerkMaterialTheme.shape,
      avatarType = AvatarType.ORGANIZATION,
      imageUrl = organization.imageUrl.takeIf { it.isNotBlank() },
    )
    Spacers.Vertical.Spacer12()
    Text(
      text = organization.name,
      style = ClerkMaterialTheme.typography.titleLarge.withMediumWeight(),
      color = ClerkMaterialTheme.colors.foreground,
    )
    organization.slug
      ?.takeIf { it.isNotBlank() }
      ?.let { slug ->
        Spacers.Vertical.Spacer4()
        Text(
          text = "@$slug",
          style = ClerkMaterialTheme.typography.bodyMedium,
          color = ClerkMaterialTheme.colors.mutedForeground,
        )
      }
    if (showsUpdateProfile) {
      Spacers.Vertical.Spacer8()
      ClerkButton(
        modifier = Modifier.defaultMinSize(minWidth = 120.dp),
        text = stringResource(R.string.update_profile),
        onClick = onUpdateProfile,
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
