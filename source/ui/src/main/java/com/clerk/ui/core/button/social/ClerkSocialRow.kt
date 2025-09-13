package com.clerk.ui.core.button.social

import android.annotation.SuppressLint
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.clerk.api.sso.OAuthProvider
import com.clerk.ui.core.common.dimens.dp8

/**
 * A composable row layout for displaying multiple social authentication buttons.
 *
 * This component arranges social login buttons in a flexible row layout using [FlowRow], which
 * automatically wraps buttons to the next line when horizontal space is constrained. All buttons in
 * the row are displayed in icon-only mode for consistent, compact sizing.
 *
 * @param providers List of [OAuthProvider]s to display as social login buttons.
 * @param modifier Optional [Modifier] for theming and styling.
 * @param onClick Lambda to be invoked when any button is clicked, passing the selected
 *   [OAuthProvider].
 */
@Composable
fun ClerkSocialRow(
  @SuppressLint("ComposeUnstableCollections") providers: List<OAuthProvider>,
  modifier: Modifier = Modifier,
  onClick: (OAuthProvider) -> Unit = {},
) {
  FlowRow(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(dp8),
    verticalArrangement = Arrangement.spacedBy(dp8),
  ) {
    providers.forEach { provider ->
      ClerkSocialButtonImpl(
        provider = provider,
        isEnabled = true,
        isPressedCombined = false,
        interactionSource = remember { MutableInteractionSource() },
        forceIconOnly = true,
        onClick = onClick,
      )
    }
  }
}
