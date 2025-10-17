package com.clerk.ui.userprofile.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors

@Composable
fun UserProfileButtonRow(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
  val interaction = remember { MutableInteractionSource() }

  ClerkMaterialTheme {
    Box(
      modifier =
        modifier
          .clip(ClerkMaterialTheme.shape) // masks ripple to this shape
          .clickable(
            interactionSource = interaction,
            indication = ripple(bounded = true, color = Color.Unspecified),
            role = Role.Button,
            onClick = onClick,
          )
          .padding(horizontal = dp16)
          .padding(vertical = dp16)
    ) {
      Text(
        text = text,
        color = ClerkMaterialTheme.colors.primary,
        style = ClerkMaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
      )
    }
  }
}

@Preview
@Composable
private fun Preview() {
  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
  ClerkMaterialTheme {
    Box(
      modifier = Modifier.background(color = ClerkMaterialTheme.colors.background).padding(dp24)
    ) {
      UserProfileButtonRow(text = "Button Row") {}
    }
  }
}
