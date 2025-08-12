package com.clerk.ui.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R as ClerkR
import com.clerk.ui.colors.DefaultColors
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkThemeAccess

/**
 * A button component that uses Clerk's design system.
 *
 * @param text The text to display on the button
 * @param onClick The callback when the button is clicked
 * @param modifier Modifier to be applied to the button
 */
@Composable
fun ClerkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
  ClerkMaterialTheme {
    Button(
      onClick = onClick,
      modifier = modifier.fillMaxWidth(),
      colors =
        ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
      shape = RoundedCornerShape(ClerkThemeAccess.design.borderRadius),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
      ) {
        Icon(
          painter = painterResource(ClerkR.drawable.ic_triangle_right),
          contentDescription = null,
        )
        Text(text = text)
        Icon(
          painter = painterResource(ClerkR.drawable.ic_triangle_right),
          contentDescription = null,
        )
      }
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewButton() {
  ClerkButton("Continue", onClick = {})
}

@PreviewLightDark
@Composable
private fun PreviewButtonWithCustomTheme() {
  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)

  ClerkButton("Custom Theme", onClick = {})
}
