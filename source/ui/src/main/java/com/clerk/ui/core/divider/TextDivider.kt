package com.clerk.ui.core.divider

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.theme.ClerkMaterialTheme

/**
 * A composable that displays a horizontal divider with text in the middle.
 *
 * This is typically used to separate sections of a UI, for example, to separate social providers
 * from other sign-in methods.
 *
 * @param text The text to display in the middle of the divider.
 * @param modifier The [Modifier] to be applied to the divider row.
 */
@Composable
fun TextDivider(text: String, modifier: Modifier = Modifier, clerkTheme: ClerkTheme? = null) {
  ClerkMaterialTheme(clerkTheme = clerkTheme) {
    Row(
      modifier = Modifier.fillMaxWidth().then(modifier),
      horizontalArrangement = Arrangement.spacedBy(dp16, alignment = Alignment.CenterHorizontally),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      HorizontalDivider(
        modifier = Modifier.weight(1f),
        thickness = dp1,
        color = ClerkMaterialTheme.computedColors.border,
      )
      Text(
        text = text,
        style = ClerkMaterialTheme.typography.bodyMedium,
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
      HorizontalDivider(
        modifier = Modifier.weight(1f),
        thickness = dp1,
        color = ClerkMaterialTheme.computedColors.border,
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.background)
          .padding(dp16)
    ) {
      TextDivider(text = " Or, sign in with another method")
    }
  }
}
