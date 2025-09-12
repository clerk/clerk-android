package com.clerk.ui.core.button.standard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.core.common.dimens.dp1
import com.clerk.ui.core.common.dimens.dp6
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
 * @param configuration Configuration controlling size, emphasis, and other visuals.
 * @param isEnabled When false, applies disabled styling and prevents clicks.
 * @param icons Optional leading and trailing icons, including their colors.
 *
 * Example:
 * ```kotlin
 * ClerkButton(
 *   text = "Continue",
 *   onClick = { /* action */ },
 *   buttonConfig = ClerkButtonConfig(style = ClerkButtonConfig.ButtonStyle.Primary)
 * )
 * ```
 */
@Composable
fun ClerkButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isEnabled: Boolean = true,
  padding: ClerkButtonPadding = ClerkButtonDefaults.padding(),
  configuration: ClerkButtonConfig = ClerkButtonDefaults.configuration(),
  icons: ClerkButtonIcons = ClerkButtonDefaults.icons(),
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  ClerkButtonImpl(
    text = text,
    onClick = onClick,
    modifier = modifier,
    configuration = configuration,
    isEnabled = isEnabled,
    isPressedCombined = pressed,
    interactionSource = interactionSource,
    icons = icons,
    padding = padding,
  )
}

@Composable
internal fun ClerkButtonWithPressedState(
  text: String,
  onClick: () -> Unit,
  isPressed: Boolean,
  modifier: Modifier = Modifier,
  isEnabled: Boolean = true,
  padding: ClerkButtonPadding = ClerkButtonDefaults.padding(),
  configuration: ClerkButtonConfig = ClerkButtonConfig(),
  icons: ClerkButtonIcons = ClerkButtonDefaults.icons(),
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  ClerkButtonImpl(
    text = text,
    onClick = onClick,
    modifier = modifier,
    isEnabled = isEnabled,
    isPressedCombined = pressed || isPressed,
    interactionSource = interactionSource,
    padding = padding,
    configuration = configuration,
    icons = icons,
  )
}

@Composable
private fun ClerkButtonImpl(
  text: String,
  onClick: () -> Unit,
  isEnabled: Boolean,
  isPressedCombined: Boolean,
  interactionSource: MutableInteractionSource,
  padding: ClerkButtonPadding,
  configuration: ClerkButtonConfig,
  modifier: Modifier = Modifier,
  icons: ClerkButtonIcons = ClerkButtonDefaults.icons(),
) {
  ClerkMaterialTheme {
    val tokens =
      buildButtonTokens(
        config = configuration,
        computed = ClerkMaterialTheme.computedColors,
        isPressed = isPressedCombined,
      )

    val surfaceModifier =
      Modifier.height(tokens.height)
        .then(modifier)
        .let { mod ->
          if (tokens.hasShadow) {
            mod.shadow(elevation = dp1, shape = ClerkMaterialTheme.shape)
          } else {
            mod
          }
        }
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          enabled = isEnabled,
          role = Role.Button,
          onClick = onClick,
        )

    Surface(
      modifier = surfaceModifier,
      shape = ClerkMaterialTheme.shape,
      color = if (isEnabled) tokens.backgroundColor else tokens.backgroundColor.copy(alpha = 0.5f),
      border = BorderStroke(tokens.borderWidth, tokens.borderColor),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = padding.horizontal, padding.vertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dp6, Alignment.CenterHorizontally),
      ) {
        icons.leadingIcon?.let {
          Icon(
            painter = painterResource(it),
            contentDescription = null,
            tint =
              if (isEnabled) icons.leadingIconColor else icons.leadingIconColor.copy(alpha = 0.5f),
          )
        }
        Text(
          text = text,
          style = tokens.textStyle,
          color = if (isEnabled) tokens.foreground else tokens.foreground.copy(alpha = 0.5f),
        )
        icons.trailingIcon?.let {
          Icon(
            painter = painterResource(it),
            contentDescription = null,
            tint =
              if (isEnabled) icons.trailingIconColor else icons.trailingIconColor.copy(alpha = 0.5f),
          )
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
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item {
        ClerkButtonWithPressedState(
          modifier = Modifier.fillMaxWidth(),
          text = "High - Large - Primary - Pressed",
          onClick = {},
          isPressed = true,
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "High - Large - Primary - Disabled",
          isEnabled = false,
          onClick = {},
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item { HorizontalDivider(modifier = Modifier.fillMaxWidth()) }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Large - Primary",
          onClick = {},
          configuration = ClerkButtonConfig(emphasis = ClerkButtonConfig.Emphasis.None),
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item {
        ClerkButtonWithPressedState(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Large - Primary - Pressed",
          onClick = {},
          isPressed = true,
          configuration = ClerkButtonConfig(emphasis = ClerkButtonConfig.Emphasis.None),
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Large - Primary - Disabled",
          isEnabled = false,
          onClick = {},
          configuration = ClerkButtonConfig(emphasis = ClerkButtonConfig.Emphasis.None),
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item { HorizontalDivider(modifier = Modifier.fillMaxWidth()) }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "Low - Small - Primary",
          onClick = {},
          configuration =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.Low,
              size = ClerkButtonConfig.Size.Small,
            ),
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item {
        ClerkButtonWithPressedState(
          modifier = Modifier.fillMaxWidth(),
          text = "Low - Small - Primary - Pressed",
          onClick = {},
          isPressed = true,
          configuration =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.Low,
              size = ClerkButtonConfig.Size.Small,
            ),
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "Low - Small - Primary - Disabled",
          isEnabled = false,
          onClick = {},
          configuration =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.Low,
              size = ClerkButtonConfig.Size.Small,
            ),
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item { HorizontalDivider(modifier = Modifier.fillMaxWidth()) }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Small - Primary",
          onClick = {},
          configuration =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Small,
            ),
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item {
        ClerkButtonWithPressedState(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Small - Primary - Pressed",
          onClick = {},
          isPressed = true,
          configuration =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Small,
            ),
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Small - Primary - Disabled",
          isEnabled = false,
          onClick = {},
          configuration =
            ClerkButtonConfig(
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Small,
            ),
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item { HorizontalDivider(modifier = Modifier.fillMaxWidth()) }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "High - Large - Secondary",
          onClick = {},
          configuration =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Secondary,
              emphasis = ClerkButtonConfig.Emphasis.High,
              size = ClerkButtonConfig.Size.Large,
            ),
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item {
        ClerkButtonWithPressedState(
          modifier = Modifier.fillMaxWidth(),
          text = "High - Large - Secondary - Pressed",
          onClick = {},
          isPressed = true,
          configuration =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Secondary,
              emphasis = ClerkButtonConfig.Emphasis.High,
              size = ClerkButtonConfig.Size.Large,
            ),
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "High - Large - Secondary - Disabled",
          isEnabled = false,
          onClick = {},
          configuration =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Secondary,
              emphasis = ClerkButtonConfig.Emphasis.High,
              size = ClerkButtonConfig.Size.Large,
            ),
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item { HorizontalDivider(modifier = Modifier.fillMaxWidth()) }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Large - Secondary",
          onClick = {},
          configuration =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Secondary,
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Large,
            ),
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item {
        ClerkButtonWithPressedState(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Large - Secondary - Pressed",
          onClick = {},
          isPressed = true,
          configuration =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Secondary,
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Large,
            ),
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Large - Secondary - Disabled",
          isEnabled = false,
          onClick = {},
          configuration =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Secondary,
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Large,
            ),
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item { HorizontalDivider(modifier = Modifier.fillMaxWidth()) }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "High - Large - Negative",
          onClick = {},
          configuration =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Negative,
              emphasis = ClerkButtonConfig.Emphasis.High,
              size = ClerkButtonConfig.Size.Large,
            ),
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item {
        ClerkButtonWithPressedState(
          modifier = Modifier.fillMaxWidth(),
          text = "High - Large - Negative - Pressed",
          onClick = {},
          isPressed = true,
          configuration =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Negative,
              emphasis = ClerkButtonConfig.Emphasis.High,
              size = ClerkButtonConfig.Size.Large,
            ),
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "High - Large - Negative - Disabled",
          onClick = {},
          isEnabled = false,
          configuration =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Negative,
              emphasis = ClerkButtonConfig.Emphasis.High,
              size = ClerkButtonConfig.Size.Large,
            ),
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item { HorizontalDivider(modifier = Modifier.fillMaxWidth()) }
      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Large - Negative",
          onClick = {},
          configuration =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Negative,
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Large,
            ),
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item {
        ClerkButtonWithPressedState(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Large - Negative - Pressed",
          onClick = {},
          isPressed = true,
          configuration =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Negative,
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Large,
            ),
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Large - Negative - Disabled",
          onClick = {},
          isEnabled = false,
          configuration =
            ClerkButtonConfig(
              style = ClerkButtonConfig.ButtonStyle.Negative,
              emphasis = ClerkButtonConfig.Emphasis.None,
              size = ClerkButtonConfig.Size.Large,
            ),
          icons =
            ClerkButtonDefaults.icons(
              // Changed
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }
    }
  }
}
