package com.clerk.ui.core.button.standard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkColors
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp0
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp2
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp6
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors

/**
 * A custom button component styled according to Clerk's design system.
 *
 * This button is built on top of Material3's [Surface] to provide Clerk's specific look and feel,
 * including variants for style, emphasis, and size.
 *
 * @param text Label displayed on the button.
 * @param onClick Invoked when the button is pressed.
 * @param modifier Compose `Modifier` for layout and semantics.
 * @param isEnabled When false, applies disabled styling and prevents clicks.
 * @param isLoading When true, shows a loading indicator instead of the button content.
 * @param padding The padding to apply to the button content.
 * @param configuration Configuration controlling size, emphasis, and other visuals.
 * @param icons Optional leading and trailing icons, including their colors.
 *
 * Example:
 * ```kotlin
 * ClerkButton(
 *   text = "Continue",
 *   onClick = { /* action */ },
 *   configuration = ClerkButtonDefaults.configuration(style = ClerkButtonConfig.ButtonStyle.Primary)
 * )
 * ```
 */
@Composable
fun ClerkButton(
  text: String?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isEnabled: Boolean = true,
  isLoading: Boolean = false,
  padding: ClerkButtonPadding = ClerkButtonDefaults.padding(),
  configuration: ClerkButtonConfiguration = ClerkButtonDefaults.configuration(),
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
    isLoading = isLoading,
    isPressedCombined = pressed,
    interactionSource = interactionSource,
    icons = icons,
    padding = padding,
  )
}

/**
 * An internal variant of [ClerkButton] that allows for direct control over the pressed state.
 *
 * This is primarily used for previews and testing to visualize the button in a pressed state
 * without requiring user interaction.
 *
 * @param isPressed Explicitly sets the pressed state of the button.
 */
@Composable
internal fun ClerkButtonWithPressedState(
  text: String,
  onClick: () -> Unit,
  isPressed: Boolean,
  modifier: Modifier = Modifier,
  isEnabled: Boolean = true,
  isLoading: Boolean = false,
  padding: ClerkButtonPadding = ClerkButtonDefaults.padding(),
  configuration: ClerkButtonConfiguration = ClerkButtonDefaults.configuration(),
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
    isLoading = isLoading,
  )
}

/**
 * The core implementation of the Clerk button.
 *
 * This private composable handles the button's appearance based on its state (enabled, pressed,
 * loading) and configuration. It combines all parameters to render the final button surface and its
 * content.
 *
 * @param isPressedCombined The combined pressed state from user interaction and explicit state.
 * @param interactionSource The [MutableInteractionSource] for tracking interactions.
 */
@Composable
private fun ClerkButtonImpl(
  text: String?,
  onClick: () -> Unit,
  isEnabled: Boolean,
  isPressedCombined: Boolean,
  interactionSource: MutableInteractionSource,
  isLoading: Boolean,
  padding: ClerkButtonPadding,
  configuration: ClerkButtonConfiguration,
  modifier: Modifier = Modifier,
  icons: ClerkButtonIcons = ClerkButtonDefaults.icons(),
) {
  ClerkMaterialTheme {
    val tokens = buildButtonTokens(config = configuration, isPressed = isPressedCombined)

    val surfaceModifier =
      Modifier.height(tokens.height)
        .then(modifier)
        .let { mod ->
          if (tokens.hasShadow && !isPressedCombined) {
            mod.shadow(elevation = dp1, shape = ClerkMaterialTheme.shape)
          } else {
            mod
          }
        }
        .clip(ClerkMaterialTheme.shape)
        .clickable(
          interactionSource = interactionSource,
          role = Role.Button,
          onClick = onClick,
          enabled = isEnabled,
        )

    Surface(
      modifier = surfaceModifier,
      shape = ClerkMaterialTheme.shape,
      color = if (isEnabled) tokens.backgroundColor else tokens.backgroundColor.copy(alpha = 0.5f),
      border = BorderStroke(tokens.borderWidth, tokens.borderColor),
    ) {
      ButtonContent(
        isLoading = isLoading,
        padding = padding,
        icons = icons,
        isEnabled = isEnabled,
        text = text,
        tokens = tokens,
        size = configuration.size,
      )
    }
  }
}

/**
 * Renders the content inside the button, which can be either the text with icons, or a loading
 * indicator.
 *
 * @param isLoading If true, shows a [CircularProgressIndicator]. Otherwise, shows the button's
 *   [text] and [icons].
 * @param padding The padding to apply around the content.
 * @param icons The icons to display.
 * @param isEnabled Controls the alpha of the content to reflect the enabled state.
 * @param text The text to display.
 * @param tokens The style tokens determining the appearance of the content (colors, text style).
 */
@Composable
private fun ButtonContent(
  isLoading: Boolean,
  padding: ClerkButtonPadding,
  icons: ClerkButtonIcons,
  isEnabled: Boolean,
  text: String?,
  size: ClerkButtonConfiguration.Size,
  tokens: ButtonStyleTokens,
) {
  if (isLoading) {
    Box(modifier = Modifier.fillMaxWidth().padding(dp12), contentAlignment = Alignment.Center) {
      CircularProgressIndicator(
        strokeWidth = dp2,
        color = tokens.foreground.copy(alpha = 0.5f),
        modifier = Modifier.size(dp24),
      )
    }
  } else {
    Row(
      modifier = Modifier.padding(horizontal = padding.horizontal, vertical = padding.vertical),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(dp6, Alignment.CenterHorizontally),
    ) {
      val iconSize = if (size == ClerkButtonConfiguration.Size.Small) dp12 else dp24
      icons.leadingIcon?.let {
        val iconColor = icons.leadingIconColor ?: ClerkMaterialTheme.colors.primaryForeground
        Icon(
          modifier = Modifier.size(iconSize),
          painter = painterResource(it),
          contentDescription = null,
          tint = if (isEnabled) iconColor else iconColor.copy(alpha = 0.5f),
        )
      }
      text?.let {
        Text(
          maxLines = 1,
          text = text,
          style = tokens.textStyle,
          overflow = TextOverflow.Ellipsis,
          color = if (isEnabled) tokens.foreground else tokens.foreground.copy(alpha = 0.5f),
        )
      }
      icons.trailingIcon?.let {
        val iconColor = icons.trailingIconColor ?: ClerkMaterialTheme.colors.primaryForeground
        Icon(
          painter = painterResource(it),
          contentDescription = null,
          tint = if (isEnabled) iconColor else iconColor.copy(alpha = 0.5f),
        )
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
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
              trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
              leadingIconColor = ClerkMaterialTheme.colors.primaryForeground,
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
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
              trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
              leadingIconColor = ClerkMaterialTheme.colors.primaryForeground,
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
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
              trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
              leadingIconColor = ClerkMaterialTheme.colors.primaryForeground,
            ),
        )
      }

      item { HorizontalDivider(modifier = Modifier.fillMaxWidth()) }

      item {
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "None - Large - Primary",
          onClick = {},
          configuration =
            ClerkButtonDefaults.configuration(emphasis = ClerkButtonConfiguration.Emphasis.None),
          icons =
            ClerkButtonDefaults.icons(
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
          configuration =
            ClerkButtonDefaults.configuration(emphasis = ClerkButtonConfiguration.Emphasis.None),
          icons =
            ClerkButtonDefaults.icons(
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
          configuration =
            ClerkButtonDefaults.configuration(emphasis = ClerkButtonConfiguration.Emphasis.None),
          icons =
            ClerkButtonDefaults.icons(
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
            ClerkButtonDefaults.configuration(
              emphasis = ClerkButtonConfiguration.Emphasis.Low,
              size = ClerkButtonConfiguration.Size.Small,
            ),
          icons =
            ClerkButtonDefaults.icons(
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
            ClerkButtonDefaults.configuration(
              emphasis = ClerkButtonConfiguration.Emphasis.Low,
              size = ClerkButtonConfiguration.Size.Small,
            ),
          icons =
            ClerkButtonDefaults.icons(
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
            ClerkButtonDefaults.configuration(
              emphasis = ClerkButtonConfiguration.Emphasis.Low,
              size = ClerkButtonConfiguration.Size.Small,
            ),
          icons =
            ClerkButtonDefaults.icons(
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
            ClerkButtonDefaults.configuration(
              emphasis = ClerkButtonConfiguration.Emphasis.None,
              size = ClerkButtonConfiguration.Size.Small,
            ),
          icons =
            ClerkButtonDefaults.icons(
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
            ClerkButtonDefaults.configuration(
              emphasis = ClerkButtonConfiguration.Emphasis.None,
              size = ClerkButtonConfiguration.Size.Small,
            ),
          icons =
            ClerkButtonDefaults.icons(
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
            ClerkButtonDefaults.configuration(
              emphasis = ClerkButtonConfiguration.Emphasis.None,
              size = ClerkButtonConfiguration.Size.Small,
            ),
          icons =
            ClerkButtonDefaults.icons(
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
            ClerkButtonDefaults.configuration(
              style = ClerkButtonConfiguration.ButtonStyle.Secondary,
              emphasis = ClerkButtonConfiguration.Emphasis.High,
              size = ClerkButtonConfiguration.Size.Large,
            ),
          icons =
            ClerkButtonDefaults.icons(
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
            ClerkButtonDefaults.configuration(
              style = ClerkButtonConfiguration.ButtonStyle.Secondary,
              emphasis = ClerkButtonConfiguration.Emphasis.High,
              size = ClerkButtonConfiguration.Size.Large,
            ),
          icons =
            ClerkButtonDefaults.icons(
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
            ClerkButtonDefaults.configuration(
              style = ClerkButtonConfiguration.ButtonStyle.Secondary,
              emphasis = ClerkButtonConfiguration.Emphasis.High,
              size = ClerkButtonConfiguration.Size.Large,
            ),
          icons =
            ClerkButtonDefaults.icons(
              leadingIcon = R.drawable.ic_triangle_right,
              trailingIcon = R.drawable.ic_triangle_right,
            ),
        )
      }
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  Clerk.customTheme = ClerkTheme(darkColors = ClerkColors(primary = Color.Red))
  ClerkMaterialTheme {
    Box(modifier = Modifier.background(ClerkMaterialTheme.colors.background).padding(dp12)) {
      ClerkButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.continue_text),
        isLoading = false,
        isEnabled = false,
        icons =
          ClerkButtonDefaults.icons(
            trailingIcon = R.drawable.ic_triangle_right,
            trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
          ),
        onClick = {},
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewIconOnly() {
  ClerkMaterialTheme {
    Box(modifier = Modifier.background(ClerkMaterialTheme.colors.background).padding(dp12)) {
      ClerkButton(
        text = null,
        configuration =
          ClerkButtonConfiguration(
            size = ClerkButtonConfiguration.Size.Small,
            emphasis = ClerkButtonConfiguration.Emphasis.High,
            style = ClerkButtonConfiguration.ButtonStyle.Secondary,
          ),
        isEnabled = true,
        icons =
          ClerkButtonDefaults.icons(
            trailingIcon = R.drawable.ic_edit,
            trailingIconColor = ClerkMaterialTheme.colors.mutedForeground,
          ),
        padding = ClerkButtonPadding(horizontal = dp8, vertical = dp0),
        onClick = {},
      )
    }
  }
}
