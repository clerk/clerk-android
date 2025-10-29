package com.clerk.ui.userprofile.account

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp18
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun UserProfileIconActionRow(
  @DrawableRes iconResId: Int,
  text: String,
  modifier: Modifier = Modifier,
  backgroundColor: Color = ClerkMaterialTheme.colors.background,
  iconSize: Dp = dp18,
  onClick: () -> Unit,
) {
  ClerkMaterialTheme {
    Row(
      modifier =
        modifier
          .fillMaxWidth()
          .background(color = backgroundColor)
          .clickable { onClick() }
          .padding(horizontal = dp24)
          .padding(vertical = dp16),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        modifier = Modifier.size(iconSize),
        painter = painterResource(iconResId),
        contentDescription = null,
        tint = ClerkMaterialTheme.colors.mutedForeground,
      )
      Spacers.Horizontal.Spacer16()
      Text(
        text = text,
        style = ClerkMaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
        color = ClerkMaterialTheme.colors.foreground,
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    Column(modifier = Modifier.fillMaxWidth()) {
      UserProfileIconActionRow(iconResId = R.drawable.ic_user, text = "Profile", onClick = {})
      UserProfileIconActionRow(iconResId = R.drawable.ic_security, text = "Security", onClick = {})
    }
  }
}
