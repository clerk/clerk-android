package com.clerk.ui.core.button.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.sso.OAuthProvider
import com.clerk.ui.core.dimens.dp8
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private const val MAX_BUTTONS_PER_ROW = 3

private const val ROW_WEIGHT = 0.5f

/**
 * A composable row layout for displaying multiple social authentication buttons.
 *
 * This component arranges social login buttons in rows of exactly 3 buttons each. When there are
 * more than 3 buttons, they wrap to additional rows. Each button takes equal width within its row
 * and is displayed in icon-only mode for consistent, compact sizing.
 *
 * For the last row, if there's an odd number of buttons remaining, they are offset in a brick
 * pattern with the first button centered and any additional buttons spread evenly.
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
    val chunks = providers.chunked(MAX_BUTTONS_PER_ROW)
    chunks.forEachIndexed { rowIndex, rowProviders ->
      val isLastRow = rowIndex == chunks.size - 1
      val shouldOffset = isLastRow && rowProviders.size != MAX_BUTTONS_PER_ROW

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
          if (shouldOffset) Arrangement.spacedBy(dp8, Alignment.CenterHorizontally)
          else Arrangement.spacedBy(dp8),
      ) {
        if (shouldOffset) {
          // Add offset spacer for brick pattern
          Spacer(modifier = Modifier.weight(ROW_WEIGHT))

          rowProviders.forEach { provider ->
            ClerkSocialButton(
              provider = provider,
              isEnabled = true,
              onClick = onClick,
              forceIconOnly = true,
              modifier = Modifier.weight(1f),
            )
          }

          Spacer(modifier = Modifier.weight(ROW_WEIGHT))
        } else {
          // Normal layout for full rows
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
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkSocialRow(
    providers =
      persistentListOf(
        OAuthProvider.GOOGLE,
        OAuthProvider.FACEBOOK,
        OAuthProvider.TWITTER,
        OAuthProvider.GITHUB,
        OAuthProvider.LINKEDIN,
      )
  )
}
