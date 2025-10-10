package com.clerk.ui.core.button.standard

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.core.dimens.dp6
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors

/**
 * A simple composable that displays a clickable text.
 *
 * This button is styled to look like a hyperlink or a subtle action item. It uses a [Box] with a
 * [Text] element inside, made clickable.
 *
 * @param text The string to display on the button.
 * @param modifier The [Modifier] to be applied to the button's [Box] container.
 * @param textColor The color of the text. Defaults to the primary color from [ClerkMaterialTheme].
 * @param textStyle The style of the text. Defaults to `titleSmall` typography from
 *   [ClerkMaterialTheme].
 * @param onClick Lambda to be invoked when the button is clicked.
 */
@Composable
fun ClerkTextButton(
  text: String,
  modifier: Modifier = Modifier,
  textColor: Color = ClerkMaterialTheme.colors.primary,
  textStyle: TextStyle = ClerkMaterialTheme.typography.titleSmall,
  boundedRipple: Boolean = true,
  rippleColor: Color = Color.Unspecified, // Unspecified -> uses LocalContentColor
  onClick: () -> Unit,
) {
  val interaction = remember { MutableInteractionSource() }

  ClerkMaterialTheme {
    Box(
      modifier =
        modifier
          .padding(horizontal = dp8)
          .clip(ClerkMaterialTheme.shape) // masks ripple to this shape
          .clickable(
            interactionSource = interaction,
            indication = ripple(bounded = boundedRipple, color = rippleColor),
            role = Role.Button,
            onClick = onClick,
          )
          .padding(horizontal = dp8, vertical = dp6)
    ) {
      Text(text = text, color = textColor, style = textStyle)
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewTextButton() {
  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
  ClerkMaterialTheme { ClerkTextButton(text = "Text Button", onClick = {}) }
}
