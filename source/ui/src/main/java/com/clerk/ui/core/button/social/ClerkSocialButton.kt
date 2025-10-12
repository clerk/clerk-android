package com.clerk.ui.core.button.social

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.logoUrl
import com.clerk.api.sso.providerName
import com.clerk.api.sso.setLogoUrl
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp3
import com.clerk.ui.core.dimens.dp48
import com.clerk.ui.core.dimens.dp6
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.collections.immutable.persistentListOf

/**
 * A composable button for social authentication with a specific [OAuthProvider].
 *
 * This button adaptively displays either the provider's logo with "Sign in with [provider name]"
 * text when there's sufficient space (>180dp width), or just the logo when space is constrained.
 * The button always maintains a minimum width of 120dp for consistent sizing.
 *
 * @param provider The [OAuthProvider] to display and use for authentication.
 * @param modifier Optional [Modifier] for theming and styling.
 * @param isEnabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable.
 * @param onClick Lambda to be invoked when the button is clicked, passing the selected
 *   [OAuthProvider].
 */
@Composable
fun ClerkSocialButton(
  provider: OAuthProvider,
  modifier: Modifier = Modifier,
  isEnabled: Boolean = true,
  forceIconOnly: Boolean = false,
  onClick: (OAuthProvider) -> Unit = {},
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  ClerkSocialButtonImpl(
    provider = provider,
    isEnabled = isEnabled,
    isPressedCombined = pressed,
    interactionSource = interactionSource,
    forceIconOnly = forceIconOnly,
    modifier = modifier,
    onClick = onClick,
  )
}

/**
 * Internal composable for [ClerkSocialButton] that allows for direct control of the pressed state.
 * This is primarily used for UI testing and previews.
 *
 * @param provider The [OAuthProvider] to display.
 * @param isPressed Manually controls the visual pressed state of the button.
 * @param modifier Optional [Modifier] for theming and styling.
 * @param isEnabled Controls the enabled state of the button.
 * @param onClick Lambda to be invoked when the button is clicked.
 */
@Composable
@VisibleForTesting
internal fun ClerkSocialButton(
  provider: OAuthProvider,
  isPressed: Boolean,
  modifier: Modifier = Modifier,
  isEnabled: Boolean = true,
  onClick: (OAuthProvider) -> Unit = {},
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  ClerkSocialButtonImpl(
    provider = provider,
    isEnabled = isEnabled,
    isPressedCombined = pressed || isPressed,
    interactionSource = interactionSource,
    modifier = modifier,
    onClick = onClick,
  )
}

/**
 * Private implementation composable for the ClerkSocialButton. This function contains the core UI
 * logic and styling for the social button. This exists so that it's possible to preview the button
 * with a forced pressed state.
 *
 * @param provider The [OAuthProvider] to display.
 * @param isEnabled Controls the enabled state of the button.
 * @param isPressedCombined Reflects the combined pressed state (either actual interaction or forced
 *   via testing).
 * @param interactionSource [MutableInteractionSource] for tracking user interactions.
 * @param modifier Optional [Modifier] for theming and styling.
 * @param onClick Lambda to be invoked when the button is clicked.
 */
@Composable
internal fun ClerkSocialButtonImpl(
  provider: OAuthProvider,
  isEnabled: Boolean,
  isPressedCombined: Boolean,
  interactionSource: MutableInteractionSource,
  modifier: Modifier = Modifier,
  forceIconOnly: Boolean = false,
  onClick: (OAuthProvider) -> Unit = {},
) {
  ClerkMaterialTheme {
    Button(
      enabled = isEnabled,
      onClick = { onClick(provider) },
      shape = ClerkMaterialTheme.shape,
      interactionSource = interactionSource,
      elevation = ButtonDefaults.buttonElevation(defaultElevation = dp3),
      colors = getButtonColors(isPressedCombined),
      contentPadding = ButtonDefaults.ContentPadding,
      modifier =
        modifier
          .fillMaxWidth()
          .shadow(
            elevation = dp3,
            shape = ClerkMaterialTheme.shape,
            clip = true,
            spotColor = ClerkMaterialTheme.colors.shadow.copy(alpha = 0.8f),
          )
          .defaultMinSize(minHeight = dp48, minWidth = 100.dp),
    ) {
      SocialButtonContent(provider = provider, isEnabled = isEnabled, forceIconOnly = forceIconOnly)
    }
  }
}

/** Returns the button colors based on the pressed state. */
@Composable
private fun getButtonColors(isPressedCombined: Boolean) =
  ButtonDefaults.buttonColors(
    containerColor =
      if (isPressedCombined) ClerkMaterialTheme.colors.muted
      else ClerkMaterialTheme.colors.background,
    contentColor = ClerkMaterialTheme.colors.foreground,
    disabledContainerColor = ClerkMaterialTheme.colors.background,
    disabledContentColor = ClerkMaterialTheme.colors.foreground.copy(alpha = 0.5f),
  )

/** Displays the adaptive content of the social button based on available width. */
@Composable
private fun SocialButtonContent(
  provider: OAuthProvider,
  isEnabled: Boolean,
  forceIconOnly: Boolean,
) {
  if (forceIconOnly) {
    SocialButtonIconOnly(provider = provider, isEnabled = isEnabled)
  } else {
    BoxWithConstraints {
      val availableWidth = LocalDensity.current.run { constraints.maxWidth.toDp() }
      if (availableWidth > 180.dp) {
        SocialButtonWithText(provider = provider, isEnabled = isEnabled)
      } else {
        SocialButtonIconOnly(provider = provider, isEnabled = isEnabled)
      }
    }
  }
}

/** Displays the social button with both icon and text. */
@Composable
private fun SocialButtonWithText(provider: OAuthProvider, isEnabled: Boolean) {
  Row(
    modifier = Modifier.background(Color.Transparent).padding(horizontal = dp12, vertical = dp6),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(dp12, Alignment.CenterHorizontally),
  ) {
    SocialButtonIcon(provider = provider, isEnabled = isEnabled, contentDescription = null)
    Text(
      text = stringResource(R.string.sign_in_with, provider.providerName),
      style = ClerkMaterialTheme.typography.titleMedium,
    )
  }
}

/** Displays the social button with icon only. */
@Composable
private fun SocialButtonIconOnly(provider: OAuthProvider, isEnabled: Boolean) {
  SocialButtonIcon(
    provider = provider,
    isEnabled = isEnabled,
    contentDescription = stringResource(R.string.sign_in_with, provider.providerName),
  )
}

/** Displays the social provider icon. */
@Composable
private fun SocialButtonIcon(
  provider: OAuthProvider,
  isEnabled: Boolean,
  contentDescription: String?,
) {
  val mutedForeground = ClerkMaterialTheme.colors.mutedForeground

  val model = provider.logoUrl?.takeUnless { it.isBlank() }

  val fallbackPainter =
    if (provider == OAuthProvider.GOOGLE) painterResource(R.drawable.ic_google)
    else painterResource(R.drawable.ic_globe)

  var showingFallback by remember { mutableStateOf(model == null) }

  AsyncImage(
    model = model,
    contentDescription = contentDescription,
    fallback = fallbackPainter,
    error = fallbackPainter,
    onSuccess = { showingFallback = false },
    onError = { showingFallback = true },
    alpha = if (isEnabled) 1f else 0.5f,
    modifier = Modifier.size(dp24),
    colorFilter =
      if (showingFallback && provider != OAuthProvider.GOOGLE) ColorFilter.tint(mutedForeground)
      else null,
  )
}

/**
 * Preview composable for showcasing [ClerkSocialButton] in different states and widths. Displays
 * the button normally, pressed, disabled, and at different widths to show the adaptive
 * text/icon-only behavior.
 */
@SuppressLint("VisibleForTests")
@PreviewLightDark
@Composable
private fun PreviewSocialButton() {
  val provider = OAuthProvider.GOOGLE
  provider.setLogoUrl(null) // Ensure consistent preview if logo URL changes
  ClerkMaterialTheme {
    Column(
      Modifier.background(ClerkMaterialTheme.colors.background).padding(dp12),
      verticalArrangement = Arrangement.spacedBy(dp12, Alignment.CenterVertically),
    ) {
      // Full width with text (>180dp)
      ClerkSocialButton(provider = provider, modifier = Modifier.widthIn(min = 200.dp))
      // Icon only - constrained width but will be at least 120dp
      ClerkSocialButton(provider = provider, modifier = Modifier.widthIn(max = 150.dp))
      // Icon only - very narrow constraint, button will still be 120dp minimum
      ClerkSocialButton(provider = provider)
      // Pressed state
      ClerkSocialButton(
        provider = provider,
        isPressed = true,
        modifier = Modifier.widthIn(min = 200.dp),
      )
      // Disabled state
      ClerkSocialButton(
        provider = provider,
        isEnabled = false,
        modifier = Modifier.widthIn(min = 200.dp),
      )
    }
  }
}

/**
 * Preview composable for showcasing [ClerkSocialButton] in different states and widths. Displays
 * the button normally, pressed, disabled, and at different widths to show the adaptive
 * text/icon-only behavior.
 */
@SuppressLint("VisibleForTests")
@PreviewLightDark
@Composable
private fun PreviewSocialRow() {
  val provider = OAuthProvider.GOOGLE
  provider.setLogoUrl(null) // Ensure consistent preview if logo URL changes
  ClerkMaterialTheme {
    Column(
      Modifier.background(ClerkMaterialTheme.colors.background).padding(dp8),
      verticalArrangement = Arrangement.spacedBy(dp12, Alignment.CenterVertically),
    ) {
      ClerkSocialRow(
        persistentListOf(
          provider,
          OAuthProvider.APPLE,
          OAuthProvider.HUGGING_FACE,
          OAuthProvider.LINEAR,
          OAuthProvider.BOX,
        )
      )
    }
  }
}
