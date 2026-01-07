package com.clerk.ui.core.button.standard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.clerk.api.ui.ClerkColors
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp2
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp6
import com.clerk.ui.theme.ClerkMaterialTheme

/**
 * A custom button component styled according to Clerk's design system.
 *
 * This button is built on top of Material3's `Button` to provide Clerk's specific look and feel,
 * including variants for style, emphasis, and size. It can display a text label, icons, or a
 * combination of both. It also supports loading and disabled states.
 *
 * @param text The text label displayed on the button. If `null`, only icons will be shown.
 * @param onClick A lambda to be invoked when the button is pressed.
 * @param modifier The `Modifier` to be applied to the button.
 * @param isEnabled When `false`, the button will be visually disabled and will not respond to
 *   clicks.
 * @param isLoading When `true`, a circular progress indicator is shown instead of the button's
 *   content.
 * @param paddingValues The padding to apply to the button's content.
 * @param configuration Configuration object that controls the button's visual appearance, including
 *   its `style`, `size`, and `emphasis`. Use [ClerkButtonDefaults.configuration] to create an
 *   instance.
 * @param icons Configuration for optional leading and trailing icons. Use
 *   [ClerkButtonDefaults.icons] to specify the icons and their colors.
 *
 * Example of a primary button:
 * ```kotlin
 * ClerkButton(
 *   text = "Continue",
 */
@Composable
fun ClerkButton(
  text: String?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isEnabled: Boolean = true,
  isLoading: Boolean = false,
  paddingValues: PaddingValues = PaddingValues(),
  configuration: ClerkButtonConfiguration = ClerkButtonDefaults.configuration(),
  icons: ClerkButtonIcons = ClerkButtonDefaults.icons(),
  clerkTheme: ClerkTheme? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  ClerkButtonImpl(
    text = text,
    onClick = onClick,
    modifier = modifier,
    configuration = configuration,
    clerkButtonState =
      ClerkButtonState(isEnabled = isEnabled, isLoading = isLoading, isPressedCombined = pressed),
    interactionSource = interactionSource,
    paddingValues = paddingValues,
    icons = icons,
    clerkTheme = clerkTheme,
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
  paddingValues: PaddingValues = PaddingValues(),
  configuration: ClerkButtonConfiguration = ClerkButtonDefaults.configuration(),
  icons: ClerkButtonIcons = ClerkButtonDefaults.icons(),
  clerkTheme: ClerkTheme? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  ClerkButtonImpl(
    text = text,
    onClick = onClick,
    modifier = modifier,
    clerkButtonState =
      ClerkButtonState(
        isEnabled = isEnabled,
        isLoading = isLoading,
        isPressedCombined = isPressed || pressed,
      ),
    interactionSource = interactionSource,
    configuration = configuration,
    icons = icons,
    paddingValues = paddingValues,
    clerkTheme = clerkTheme,
  )
}

/**
 * The core implementation of the Clerk button.
 *
 * This private composable handles the button's appearance based on its state and configuration. It
 * combines all parameters to render the final button surface and its content.
 *
 * @param text Label displayed on the button.
 * @param onClick Invoked when the button is pressed.
 * @param clerkButtonState A state object containing `isEnabled`, `isLoading`, and
 *   `isPressedCombined`.
 * @param configuration Configuration controlling size, emphasis, and other visuals.
 * @param modifier Compose `Modifier` for layout and semantics.
 * @param paddingValues The padding to apply to the button content.
 * @param icons Optional leading and trailing icons, including their colors.
 * @param interactionSource The [MutableInteractionSource] that will be used to dispatch press
 *   events.
 */
@Composable
private fun ClerkButtonImpl(
  text: String?,
  onClick: () -> Unit,
  clerkButtonState: ClerkButtonState,
  configuration: ClerkButtonConfiguration,
  paddingValues: PaddingValues,
  interactionSource: MutableInteractionSource,
  modifier: Modifier = Modifier,
  icons: ClerkButtonIcons = ClerkButtonDefaults.icons(),
  clerkTheme: ClerkTheme? = null,
) {
  ClerkMaterialTheme(clerkTheme = clerkTheme) {
    val tokens =
      buildButtonTokens(config = configuration, isPressed = clerkButtonState.isPressedCombined)

    val surfaceModifier =
      Modifier.height(tokens.height).then(modifier).clip(ClerkMaterialTheme.shape)

    Button(
      modifier = surfaceModifier,
      shape = ClerkMaterialTheme.shape,
      enabled = clerkButtonState.isEnabled,
      interactionSource = interactionSource,
      contentPadding = paddingValues,
      border =
        if (configuration.style == ClerkButtonConfiguration.ButtonStyle.Secondary) {
          BorderStroke(dp1, ClerkMaterialTheme.colors.shadow.copy(alpha = 0.08f))
        } else {
          null
        },
      colors =
        ButtonDefaults.buttonColors(
          containerColor =
            if (clerkButtonState.isEnabled) tokens.backgroundColor
            else tokens.backgroundColor.copy(alpha = 0.5f)
        ),
      onClick = onClick,
    ) {
      ButtonContent(
        isLoading = clerkButtonState.isLoading,
        icons = icons,
        isEnabled = clerkButtonState.isEnabled,
        text = text,
        tokens = tokens,
        size = configuration.size,
        paddingValues = paddingValues,
      )
    }
  }
}

data class ClerkButtonState(
  val isLoading: Boolean,
  val isEnabled: Boolean,
  val isPressedCombined: Boolean,
)

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
  icons: ClerkButtonIcons,
  isEnabled: Boolean,
  text: String?,
  size: ClerkButtonConfiguration.Size,
  paddingValues: PaddingValues,
  tokens: ButtonStyleTokens,
) {
  if (isLoading) {
    Box(
      modifier = Modifier.fillMaxWidth().padding(paddingValues),
      contentAlignment = Alignment.Center,
    ) {
      CircularProgressIndicator(
        strokeWidth = dp2,
        color = tokens.foreground.copy(alpha = 0.5f),
        modifier = Modifier.size(dp24),
      )
    }
  } else {
    Row(
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
  ClerkMaterialTheme(clerkTheme = ClerkTheme(darkColors = ClerkColors(primary = Color.Red))) {
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
        onClick = {},
      )
    }
  }
}
