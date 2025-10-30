package com.clerk.ui.core.button.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.clerk.api.sso.OAuthProvider
import com.clerk.ui.core.dimens.dp8
import kotlinx.collections.immutable.ImmutableList

private const val MAX_BUTTONS_PER_ROW = 3

/**
 * A composable row layout for displaying multiple social authentication buttons.
 *
 * This component arranges social login buttons in rows of exactly 3 buttons each. When there are
 * more than 3 buttons, they wrap to additional rows. Each button takes equal width within its row
 * and is displayed in icon-only mode for consistent, compact sizing.
 *
 * @param providers List of [OAuthProvider]s to display as social login buttons.
 * @param modifier Optional [Modifier] for theming and styling.
 * @param onClick Lambda to be invoked when any button is clicked, passing the selected
 *   [OAuthProvider].
 */
@Composable
fun ClerkSocialRow(
  providers: ImmutableList<OAuthProvider>,
  modifier: Modifier = Modifier,
  onClick: (OAuthProvider) -> Unit = {},
) {
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(dp8)) {
    providers.chunked(MAX_BUTTONS_PER_ROW).forEach { rowProviders ->
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dp8)) {
        rowProviders.forEach { provider ->
          ClerkSocialButton(
            provider = provider,
            isEnabled = true,
            onClick = onClick,
            forceIconOnly = true,
            modifier = Modifier.weight(1f),
          )
        }
        // Add empty spaces to maintain equal spacing when less than 3 buttons in a row
        repeat(MAX_BUTTONS_PER_ROW - rowProviders.size) { Spacer(modifier = Modifier.weight(1f)) }
      }
    }
  }
}
