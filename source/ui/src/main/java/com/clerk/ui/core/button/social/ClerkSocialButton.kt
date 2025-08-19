package com.clerk.ui.core.button.social

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
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
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.LocalClerkColors
import com.clerk.ui.theme.LocalClerkDesign

/**
 * A composable button for social authentication with a specific [OAuthProvider].
 *
 * This button displays the provider's logo and name, and handles click events to initiate the
 * social login flow.
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
  onClick: (OAuthProvider) -> Unit = {},
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  ClerkSocialButtonImpl(
    provider = provider,
    isEnabled = isEnabled,
    isPressedCombined = pressed,
    interactionSource = interactionSource,
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
private fun ClerkSocialButtonImpl(
  provider: OAuthProvider,
  isEnabled: Boolean,
  isPressedCombined: Boolean,
  interactionSource: MutableInteractionSource,
  modifier: Modifier = Modifier,
  onClick: (OAuthProvider) -> Unit = {},
) {
  ClerkMaterialTheme {
    val design = LocalClerkDesign.current
    val colors = LocalClerkColors.current
    val shape = RoundedCornerShape(design.borderRadius)

    Button(
      enabled = isEnabled,
      onClick = { onClick(provider) },
      shape = shape,
      interactionSource = interactionSource,
      elevation = ButtonDefaults.buttonElevation(defaultElevation = dp3),
      colors =
        ButtonDefaults.buttonColors(
          containerColor =
            if (isPressedCombined) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.background,
          contentColor = MaterialTheme.colorScheme.onBackground,
          disabledContainerColor = MaterialTheme.colorScheme.background,
          disabledContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        ),
      contentPadding = ButtonDefaults.ContentPadding,
      modifier =
        modifier
          .shadow(
            elevation = dp3,
            shape = shape,
            clip = true,
            spotColor = colors.shadow?.copy(alpha = 0.8f) ?: Color.Black.copy(alpha = 0.08f),
          )
          .defaultMinSize(minHeight = dp48),
    ) {
      Row(
        modifier =
          Modifier.background(Color.Transparent).padding(horizontal = dp12, vertical = dp6),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dp12, Alignment.CenterHorizontally),
      ) {
        AsyncImage(
          model = provider.logoUrl,
          contentDescription = null, // Decorative, providerName serves as text alternative
          fallback = painterResource(R.drawable.ic_google),
          alpha = if (isEnabled) 1f else 0.5f,
          modifier = Modifier.size(dp24),
        )
        Text(text = provider.providerName, style = MaterialTheme.typography.titleMedium)
      }
    }
  }
}

/**
 * Preview composable for showcasing [ClerkSocialButton] in different states. Displays the button
 * normally, pressed, and disabled.
 */
@SuppressLint("VisibleForTests")
@PreviewLightDark
@Composable
private fun PreviewSocialButton() {
  val provider = OAuthProvider.GOOGLE
  provider.setLogoUrl(null) // Ensure consistent preview if logo URL changes
  ClerkMaterialTheme {
    Column(
      Modifier.background(MaterialTheme.colorScheme.background).padding(dp12),
      verticalArrangement = Arrangement.spacedBy(dp12, Alignment.CenterVertically),
    ) {
      ClerkSocialButton(provider = provider)
      ClerkSocialButton(provider = provider, isPressed = true)
      ClerkSocialButton(provider = provider, isEnabled = false)
    }
  }
}
