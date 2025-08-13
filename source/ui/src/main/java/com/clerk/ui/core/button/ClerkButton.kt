package com.clerk.ui.core.button

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
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
import com.clerk.ui.R
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
  isEnabled: Boolean = true,
  @DrawableRes leadingIcon: Int? = null,
  @DrawableRes trailingIcon: Int? = null,
  buttonStyle: ButtonStyle = ButtonStyle.Primary,
) {
  ClerkMaterialTheme {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val tokens =
      buildButtonTokens(
        style = buttonStyle,
        config = buttonConfig,
        colors = ClerkThemeAccess.colors,
        computed = ClerkThemeAccess.computed,
        typography = ClerkThemeAccess.typography,
        design = ClerkThemeAccess.design,
        isPressed = pressed,
      )

    Button(
      onClick = onClick,
      modifier = Modifier.height(tokens.height).fillMaxWidth().then(modifier),
      interactionSource = interactionSource,
      colors =
        ButtonDefaults.buttonColors(
          containerColor =
            if (isEnabled) tokens.backgroundColor else tokens.backgroundColor.copy(alpha = 0.5f),
          contentColor = if (isEnabled) tokens.foreground else tokens.foreground.copy(alpha = 0.5f),
        ),
      border = BorderStroke(tokens.borderWidth, tokens.borderColor),
      shape = RoundedCornerShape(tokens.cornerRadius),
      elevation =
        if (tokens.hasShadow)
          ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 1.dp)
        else null,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
      ) {
        leadingIcon?.let { Icon(painter = painterResource(it), contentDescription = null) }
        Text(text = text, style = tokens.textStyle)
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
    LazyColumn(
      modifier =
        Modifier.fillMaxSize()
          .background(color = MaterialTheme.colorScheme.background)
          .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
      item {
        ClerkButton(
          text = "Continue",
          onClick = {},
          buttonStyle = ButtonStyle.Primary,
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }
      item {
        ClerkButton(
          text = "Continue",
          isEnabled = false,
          onClick = {},
          buttonStyle = ButtonStyle.Primary,
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }
      item {
        ClerkButton(
          text = "Continue",
          onClick = {},
          buttonConfig = ClerkButtonConfig(emphasis = ClerkButtonConfig.Emphasis.None),
          buttonStyle = ButtonStyle.Primary,
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }
      item {
        ClerkButton(
          text = "Continue",
          isEnabled = false,
          onClick = {},
          buttonConfig = ClerkButtonConfig(emphasis = ClerkButtonConfig.Emphasis.None),
          buttonStyle = ButtonStyle.Primary,
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }
      item {
        ClerkButton(
          text = "Continue",
          onClick = {},
          buttonConfig =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.Low,
              size = ClerkButtonConfig.Size.Small,
            ),
          buttonStyle = ButtonStyle.Primary,
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }
      item {
        ClerkButton(
          text = "Continue",
          isEnabled = false,
          onClick = {},
          buttonConfig =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.Low,
              size = ClerkButtonConfig.Size.Small,
            ),
          buttonStyle = ButtonStyle.Primary,
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }
      item {
        ClerkButton(
          text = "Continue",
          onClick = {},
          buttonConfig =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Small,
            ),
          buttonStyle = ButtonStyle.Primary,
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }
      item {
        ClerkButton(
          text = "Continue",
          isEnabled = false,
          onClick = {},
          buttonConfig =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Small,
            ),
          buttonStyle = ButtonStyle.Primary,
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }
      item {
        ClerkButton(
          text = "Continue",
          onClick = {},
          buttonConfig =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.High,
              size = ClerkButtonConfig.Size.Large,
            ),
          buttonStyle = ButtonStyle.Secondary,
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }
      item {
        ClerkButton(
          text = "Continue",
          onClick = {},
          isEnabled = false,
          buttonConfig =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.High,
              size = ClerkButtonConfig.Size.Large,
            ),
          buttonStyle = ButtonStyle.Secondary,
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }
      item {
        ClerkButton(
          text = "Continue",
          onClick = {},
          buttonConfig =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Large,
            ),
          buttonStyle = ButtonStyle.Secondary,
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }
      item {
        ClerkButton(
          text = "Continue",
          onClick = {},
          isEnabled = false,
          buttonConfig =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Large,
            ),
          buttonStyle = ButtonStyle.Secondary,
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }
      item {
        ClerkButton(
          text = "Continue",
          onClick = {},
          buttonConfig =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.High,
              size = ClerkButtonConfig.Size.Large,
            ),
          buttonStyle = ButtonStyle.Negative,
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }
      item {
        ClerkButton(
          text = "Continue",
          onClick = {},
          isEnabled = false,
          buttonConfig =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.High,
              size = ClerkButtonConfig.Size.Large,
            ),
          buttonStyle = ButtonStyle.Negative,
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }
      item {
        ClerkButton(
          text = "Continue",
          onClick = {},
          buttonConfig =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Large,
            ),
          buttonStyle = ButtonStyle.Negative,
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }
      item {
        ClerkButton(
          text = "Continue",
          onClick = {},
          isEnabled = false,
          buttonConfig =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Large,
            ),
          buttonStyle = ButtonStyle.Negative,
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }
    }
  }
}
