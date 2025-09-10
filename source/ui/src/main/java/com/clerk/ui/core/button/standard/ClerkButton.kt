package com.clerk.ui.core.button.standard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.core.common.dimens.dp1
import com.clerk.ui.core.common.dimens.dp4
import com.clerk.ui.core.common.dimens.dp6
import com.clerk.ui.core.common.dimens.dp8
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors

/**
 * Clerk-styled button composable.
 *
 * Renders a Material3 `Button` themed with Clerk tokens and variants.
 *
 * @param text Label displayed on the button.
 * @param onClick Invoked when the button is pressed.
 * @param modifier Compose `Modifier` for layout and semantics.
 * @param buttonConfig Configuration controlling size, emphasis, and other visuals.
 * @param isEnabled When false, applies disabled styling and prevents clicks.
 * @param trailingIcon Optional drawable resource ID for an icon displayed after the text.
 * @param leadingIcon Optional drawable resource ID for an icon displayed before the text.
 * @param trailingIconTint Tint color for the trailing icon. Defaults to no tint.
 * @param leadingIconTint Tint color for the leading icon. Defaults to no tint.
 *
 * Example:
 * ```kotlin
 * ClerkButton(
 *   text = "Continue",
 *   onClick = { /* action */ },
 *   buttonStyle = _root_ide_package_.com.clerk.ui.core.button.standard.ClerkButtonConfig.ButtonStyle.Primary
 * )
 * ```
 */
@Composable
fun ClerkButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  buttonConfig: ClerkButtonConfig = ClerkButtonConfig(),
  isEnabled: Boolean = true,
  trailingIcon: Int? = null,
  leadingIcon: Int? = null,
  trailingIconTint: Color = Color.Unspecified,
  leadingIconTint: Color = Color.Unspecified,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  ClerkButtonImpl(
    text = text,
    onClick = onClick,
    modifier = modifier,
    buttonConfig = buttonConfig,
    isEnabled = isEnabled,
    isPressedCombined = pressed,
    interactionSource = interactionSource,
    trailingIcon = trailingIcon,
    leadingIcon = leadingIcon,
    trailingIconTint = trailingIconTint,
    leadingIconTint = leadingIconTint,
  )
}

@Composable
internal fun ClerkButtonWithPressedState(
  text: String,
  onClick: () -> Unit,
  isPressed: Boolean,
  modifier: Modifier = Modifier,
  buttonConfig: ClerkButtonConfig = ClerkButtonConfig(),
  trailingIcon: Int? = null,
  leadingIcon: Int? = null,
  isEnabled: Boolean = true,
  trailingIconTint: Color = Color.Unspecified,
  leadingIconTint: Color = Color.Unspecified,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  ClerkButtonImpl(
    text = text,
    onClick = onClick,
    modifier = modifier,
    buttonConfig = buttonConfig,
    isEnabled = isEnabled,
    isPressedCombined = pressed || isPressed,
    interactionSource = interactionSource,
    trailingIcon = trailingIcon,
    leadingIcon = leadingIcon,
    trailingIconTint = trailingIconTint,
    leadingIconTint = leadingIconTint,
  )
}

@Composable
private fun ClerkButtonImpl(
  text: String,
  onClick: () -> Unit,
  buttonConfig: ClerkButtonConfig,
  isEnabled: Boolean,
  isPressedCombined: Boolean,
  interactionSource: MutableInteractionSource,
  modifier: Modifier = Modifier,
  trailingIcon: Int? = null,
  leadingIcon: Int? = null,
  trailingIconTint: Color = Color.Unspecified,
  leadingIconTint: Color = Color.Unspecified,
) {
  ClerkMaterialTheme {
    val tokens =
      buildButtonTokens(
        config = buttonConfig,
        computed = ClerkMaterialTheme.computedColors,
        isPressed = isPressedCombined,
      )

    Button(
      contentPadding = PaddingValues(0.dp),
      onClick = onClick,
      modifier = Modifier.height(tokens.height).then(modifier),
      enabled = isEnabled,
      interactionSource = interactionSource,
      colors =
        ButtonDefaults.buttonColors(
          containerColor = tokens.backgroundColor,
          contentColor = tokens.foreground,
          disabledContainerColor = tokens.backgroundColor.copy(alpha = 0.5f),
          disabledContentColor = tokens.foreground.copy(alpha = 0.5f),
        ),
      border = BorderStroke(tokens.borderWidth, tokens.borderColor),
      shape = ClerkMaterialTheme.shape,
      elevation =
        if (tokens.hasShadow)
          ButtonDefaults.buttonElevation(defaultElevation = dp1, pressedElevation = dp1)
        else null,
    ) {
      Row(
        modifier = Modifier.padding(horizontal = dp4, vertical = dp8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dp6, Alignment.CenterHorizontally),
      ) {
        leadingIcon?.let {
          Icon(painter = painterResource(it), contentDescription = null, tint = leadingIconTint)
        }
        Text(text = text, style = tokens.textStyle)
        trailingIcon?.let {
          Icon(painter = painterResource(it), contentDescription = null, tint = trailingIconTint)
        }
      }
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewButton() {
  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
  ClerkMaterialTheme {
    LazyColumn(
      modifier =
        Modifier.fillMaxSize()
          .background(color = ClerkMaterialTheme.colors.background)
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "High - Large - Primary",
          onClick = {},
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item {
        ClerkButtonWithPressedState(
          modifier = Modifier.fillMaxWidth(),
          text = "High - Large - Primary - Pressed",
          onClick = {},
          isPressed = true,
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "High - Large - Primary - Disabled",
          isEnabled = false,
          onClick = {},
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item { HorizontalDivider(modifier = Modifier.fillMaxWidth()) }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Large - Primary",
          onClick = {},
          buttonConfig = ClerkButtonConfig(emphasis = ClerkButtonConfig.Emphasis.None),
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item {
        ClerkButtonWithPressedState(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Large - Primary - Pressed",
          onClick = {},
          isPressed = true,
          buttonConfig = ClerkButtonConfig(emphasis = ClerkButtonConfig.Emphasis.None),
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Large - Primary - Disabled",
          isEnabled = false,
          onClick = {},
          buttonConfig = ClerkButtonConfig(emphasis = ClerkButtonConfig.Emphasis.None),
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item { HorizontalDivider(modifier = Modifier.fillMaxWidth()) }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "Low - Small - Primary",
          onClick = {},
          buttonConfig =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.Low,
              size = ClerkButtonConfig.Size.Small,
            ),
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item {
        ClerkButtonWithPressedState(
          modifier = Modifier.fillMaxWidth(),
          text = "Low - Small - Primary - Pressed",
          onClick = {},
          isPressed = true,
          buttonConfig =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.Low,
              size = ClerkButtonConfig.Size.Small,
            ),
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "Low - Small - Primary - Disabled",
          isEnabled = false,
          onClick = {},
          buttonConfig =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.Low,
              size = ClerkButtonConfig.Size.Small,
            ),
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item { HorizontalDivider(modifier = Modifier.fillMaxWidth()) }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Small - Primary",
          onClick = {},
          buttonConfig =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Small,
            ),
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item {
        ClerkButtonWithPressedState(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Small - Primary - Pressed",
          onClick = {},
          isPressed = true,
          buttonConfig =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Small,
            ),
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Small - Primary - Disabled",
          isEnabled = false,
          onClick = {},
          buttonConfig =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Small,
            ),
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item { HorizontalDivider(modifier = Modifier.fillMaxWidth()) }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "High - Large - Secondary",
          onClick = {},
          buttonConfig =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Secondary,
              emphasis = ClerkButtonConfig.Emphasis.High,
              size = ClerkButtonConfig.Size.Large,
            ),
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item {
        ClerkButtonWithPressedState(
          modifier = Modifier.fillMaxWidth(),
          text = "High - Large - Secondary - Pressed",
          onClick = {},
          isPressed = true,
          buttonConfig =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Secondary,
              emphasis = ClerkButtonConfig.Emphasis.High,
              size = ClerkButtonConfig.Size.Large,
            ),
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "High - Large - Secondary - Disabled",
          isEnabled = false,
          onClick = {},
          buttonConfig =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Secondary,
              emphasis = ClerkButtonConfig.Emphasis.High,
              size = ClerkButtonConfig.Size.Large,
            ),
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item { HorizontalDivider(modifier = Modifier.fillMaxWidth()) }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Large - Secondary",
          onClick = {},
          buttonConfig =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Secondary,
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Large,
            ),
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item {
        ClerkButtonWithPressedState(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Large - Secondary - Pressed",
          onClick = {},
          isPressed = true,
          buttonConfig =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Secondary,
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Large,
            ),
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Large - Secondary - Disabled",
          isEnabled = false,
          onClick = {},
          buttonConfig =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Secondary,
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Large,
            ),
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item { HorizontalDivider(modifier = Modifier.fillMaxWidth()) }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "High - Large - Negative",
          onClick = {},
          buttonConfig =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Negative,
              emphasis = ClerkButtonConfig.Emphasis.High,
              size = ClerkButtonConfig.Size.Large,
            ),
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item {
        ClerkButtonWithPressedState(
          modifier = Modifier.fillMaxWidth(),
          text = "High - Large - Negative - Pressed",
          onClick = {},
          isPressed = true,
          buttonConfig =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Negative,
              emphasis = ClerkButtonConfig.Emphasis.High,
              size = ClerkButtonConfig.Size.Large,
            ),
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "High - Large - Negative - Disabled",
          onClick = {},
          isEnabled = false,
          buttonConfig =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Negative,
              emphasis = ClerkButtonConfig.Emphasis.High,
              size = ClerkButtonConfig.Size.Large,
            ),
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item { HorizontalDivider(modifier = Modifier.fillMaxWidth()) }
      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Large - Negative",
          onClick = {},
          buttonConfig =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Negative,
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Large,
            ),
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item {
        ClerkButtonWithPressedState(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Large - Negative - Pressed",
          onClick = {},
          isPressed = true,
          buttonConfig =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Negative,
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Large,
            ),
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Large - Negative - Disabled",
          onClick = {},
          isEnabled = false,
          buttonConfig =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Negative,
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Large,
            ),
          leadingIcon = R.drawable.ic_triangle_right,
          trailingIcon = R.drawable.ic_triangle_right,
        )
      }
    }
  }
}
