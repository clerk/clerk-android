package com.clerk.ui.userprofile.common

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
import com.clerk.ui.core.dimens.dp6
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun UserProfileSectionFooter(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
  val interaction = remember { MutableInteractionSource() }

  ClerkMaterialTheme {
    Box(
      modifier =
        modifier
          .padding(horizontal = dp8)
          .clip(ClerkMaterialTheme.shape) // masks ripple to this shape
          .clickable(
            interactionSource = interaction,
            indication = ripple(bounded = true, color = Color.Unspecified),
            role = Role.Button,
            onClick = onClick,
          )
          .padding(horizontal = dp8, vertical = dp6)
    ) {
      Text(
        text = text,
        color = ClerkMaterialTheme.colors.primary,
        style = ClerkMaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
      )
    }
  }
}
