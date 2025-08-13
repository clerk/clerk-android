package com.clerk.ui.core.button

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.clerk.ui.R as ClerkR
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
fun ClerkButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  buttonConfig: ClerkButtonConfig = ClerkButtonConfig(),
  @DrawableRes leadingIcon: Int? = null,
  @DrawableRes trailingIcon: Int? = null,
  buttonStyle: ButtonStyle = ButtonStyle.Primary,
) {
  ClerkMaterialTheme {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val resolvedStyle =
      when (buttonStyle) {
        ButtonStyle.Primary ->
          PrimaryButtonStyle(
            config = buttonConfig,
            clerkColors = ClerkThemeAccess.colors,
            clerkDesign = ClerkThemeAccess.design,
            clerkTypography = ClerkThemeAccess.typography,
            computedColors = ClerkThemeAccess.computed,
            isPressed = pressed,
          )
        ButtonStyle.Secondary ->
          SecondaryButtonStyle(
            config = buttonConfig,
            clerkColors = ClerkThemeAccess.colors,
            clerkDesign = ClerkThemeAccess.design,
            clerkTypography = ClerkThemeAccess.typography,
            computedColors = ClerkThemeAccess.computed,
            isPressed = pressed,
          )
        ButtonStyle.Negative ->
          NegativeButtonStyle(
            config = buttonConfig,
            clerkColors = ClerkThemeAccess.colors,
            clerkDesign = ClerkThemeAccess.design,
            clerkTypography = ClerkThemeAccess.typography,
            computedColors = ClerkThemeAccess.computed,
            isPressed = pressed,
          )
      }

    val container = resolvedStyle.backgroundColor()
    val content = resolvedStyle.foregroundColor()

    Button(
      onClick = onClick,
      modifier = Modifier.height(resolvedStyle.height()).fillMaxWidth().then(modifier),
      interactionSource = interactionSource,
      colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
      shape = RoundedCornerShape(ClerkThemeAccess.design.borderRadius),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
      ) {
        leadingIcon?.let { Icon(painter = painterResource(it), contentDescription = null) }
        Text(text = text, style = resolvedStyle.textStyle())
        trailingIcon?.let { Icon(painter = painterResource(it), contentDescription = null) }
      }
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewButton() {
  //  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .background(color = MaterialTheme.colorScheme.background)
          .padding(16.dp)
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
      ) {
        // Primary - High
        item {
          ClerkButton(
            text = "Primary • High • Large",
            onClick = {},
            buttonStyle = ButtonStyle.Primary,
            leadingIcon = ClerkR.drawable.ic_triangle_right,
            trailingIcon = ClerkR.drawable.ic_triangle_right,
          )
        }
        item {
          ClerkButton(
            text = "Primary • High • Small",
            buttonConfig = ClerkButtonConfig(size = ClerkButtonConfig.Size.Small),
            onClick = {},
            buttonStyle = ButtonStyle.Primary,
            leadingIcon = ClerkR.drawable.ic_triangle_right,
            trailingIcon = ClerkR.drawable.ic_triangle_right,
          )
        }
        // Primary - Low
        item {
          ClerkButton(
            text = "Primary • Low • Large",
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.Low,
                size = ClerkButtonConfig.Size.Large,
              ),
            onClick = {},
            buttonStyle = ButtonStyle.Primary,
            leadingIcon = ClerkR.drawable.ic_triangle_right,
            trailingIcon = ClerkR.drawable.ic_triangle_right,
          )
        }
        item {
          ClerkButton(
            text = "Primary • Low • Small",
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.Low,
                size = ClerkButtonConfig.Size.Small,
              ),
            onClick = {},
            buttonStyle = ButtonStyle.Primary,
            leadingIcon = ClerkR.drawable.ic_triangle_right,
            trailingIcon = ClerkR.drawable.ic_triangle_right,
          )
        }
        // Primary - None
        item {
          ClerkButton(
            text = "Primary • None • Large",
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Large,
              ),
            onClick = {},
            buttonStyle = ButtonStyle.Primary,
            leadingIcon = ClerkR.drawable.ic_triangle_right,
            trailingIcon = ClerkR.drawable.ic_triangle_right,
          )
        }
        item {
          ClerkButton(
            text = "Primary • None • Small",
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Small,
              ),
            onClick = {},
            buttonStyle = ButtonStyle.Primary,
            leadingIcon = ClerkR.drawable.ic_triangle_right,
            trailingIcon = ClerkR.drawable.ic_triangle_right,
          )
        }

        // Secondary - High
        item {
          ClerkButton(
            text = "Secondary • High • Large",
            onClick = {},
            buttonStyle = ButtonStyle.Secondary,
            leadingIcon = ClerkR.drawable.ic_triangle_right,
            trailingIcon = ClerkR.drawable.ic_triangle_right,
          )
        }
        item {
          ClerkButton(
            text = "Secondary • High • Small",
            buttonConfig = ClerkButtonConfig(size = ClerkButtonConfig.Size.Small),
            onClick = {},
            buttonStyle = ButtonStyle.Secondary,
            leadingIcon = ClerkR.drawable.ic_triangle_right,
            trailingIcon = ClerkR.drawable.ic_triangle_right,
          )
        }
        // Secondary - Low
        item {
          ClerkButton(
            text = "Secondary • Low • Large",
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.Low,
                size = ClerkButtonConfig.Size.Large,
              ),
            onClick = {},
            buttonStyle = ButtonStyle.Secondary,
            leadingIcon = ClerkR.drawable.ic_triangle_right,
            trailingIcon = ClerkR.drawable.ic_triangle_right,
          )
        }
        item {
          ClerkButton(
            text = "Secondary • Low • Small",
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.Low,
                size = ClerkButtonConfig.Size.Small,
              ),
            onClick = {},
            buttonStyle = ButtonStyle.Secondary,
            leadingIcon = ClerkR.drawable.ic_triangle_right,
            trailingIcon = ClerkR.drawable.ic_triangle_right,
          )
        }
        // Secondary - None
        item {
          ClerkButton(
            text = "Secondary • None • Large",
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Large,
              ),
            onClick = {},
            buttonStyle = ButtonStyle.Secondary,
            leadingIcon = ClerkR.drawable.ic_triangle_right,
            trailingIcon = ClerkR.drawable.ic_triangle_right,
          )
        }
        item {
          ClerkButton(
            text = "Secondary • None • Small",
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Small,
              ),
            onClick = {},
            buttonStyle = ButtonStyle.Secondary,
            leadingIcon = ClerkR.drawable.ic_triangle_right,
            trailingIcon = ClerkR.drawable.ic_triangle_right,
          )
        }

        // Negative - High
        item {
          ClerkButton(
            text = "Negative • High • Large",
            onClick = {},
            buttonStyle = ButtonStyle.Negative,
            leadingIcon = ClerkR.drawable.ic_triangle_right,
            trailingIcon = ClerkR.drawable.ic_triangle_right,
          )
        }
        item {
          ClerkButton(
            text = "Negative • High • Small",
            buttonConfig = ClerkButtonConfig(size = ClerkButtonConfig.Size.Small),
            onClick = {},
            buttonStyle = ButtonStyle.Negative,
            leadingIcon = ClerkR.drawable.ic_triangle_right,
            trailingIcon = ClerkR.drawable.ic_triangle_right,
          )
        }
        // Negative - Low
        item {
          ClerkButton(
            text = "Negative • Low • Large",
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.Low,
                size = ClerkButtonConfig.Size.Large,
              ),
            onClick = {},
            buttonStyle = ButtonStyle.Negative,
            leadingIcon = ClerkR.drawable.ic_triangle_right,
            trailingIcon = ClerkR.drawable.ic_triangle_right,
          )
        }
        item {
          ClerkButton(
            text = "Negative • Low • Small",
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.Low,
                size = ClerkButtonConfig.Size.Small,
              ),
            onClick = {},
            buttonStyle = ButtonStyle.Negative,
            leadingIcon = ClerkR.drawable.ic_triangle_right,
            trailingIcon = ClerkR.drawable.ic_triangle_right,
          )
        }
        // Negative - None
        item {
          ClerkButton(
            text = "Negative • None • Large",
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Large,
              ),
            onClick = {},
            buttonStyle = ButtonStyle.Negative,
            leadingIcon = ClerkR.drawable.ic_triangle_right,
            trailingIcon = ClerkR.drawable.ic_triangle_right,
          )
        }
        item {
          ClerkButton(
            text = "Negative • None • Small",
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Small,
              ),
            onClick = {},
            buttonStyle = ButtonStyle.Negative,
            leadingIcon = ClerkR.drawable.ic_triangle_right,
            trailingIcon = ClerkR.drawable.ic_triangle_right,
          )
        }
      }
    }
  }
}
