package com.clerk.ui.userprofile.custom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp18
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.account.UserProfileIconActionRow

@Composable
internal fun CustomRowView(
  customRow: UserProfileCustomRow,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  when (val icon = customRow.icon) {
    is UserProfileRowIcon.Resource -> {
      UserProfileIconActionRow(
        iconResId = icon.resId,
        text = customRow.title,
        onClick = onClick,
        modifier = modifier,
      )
    }
    is UserProfileRowIcon.Vector -> {
      val interactionSource = remember { MutableInteractionSource() }
      Row(
        modifier =
          modifier
            .fillMaxWidth()
            .background(color = ClerkMaterialTheme.colors.background)
            .clickable(
              interactionSource = interactionSource,
              indication =
                ripple(color = ClerkMaterialTheme.colors.mutedForeground.copy(alpha = 0.1f)),
              role = Role.Button,
              onClick = onClick,
            )
            .padding(horizontal = dp24)
            .padding(vertical = dp16),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          modifier = Modifier.size(dp18),
          imageVector = icon.imageVector,
          contentDescription = null,
          tint = ClerkMaterialTheme.colors.mutedForeground,
        )
        Spacers.Horizontal.Spacer16()
        Text(
          text = customRow.title,
          style = ClerkMaterialTheme.typography.bodyLarge.withMediumWeight(),
          color = ClerkMaterialTheme.colors.foreground,
        )
      }
    }
  }
}
