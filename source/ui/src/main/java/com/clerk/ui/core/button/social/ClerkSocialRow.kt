package com.clerk.ui.core.button.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.core.dimens.dp8
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private const val MAX_BUTTONS_PER_ROW = 3

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
 * @param clerkTheme Optional [ClerkTheme] for theming.
 * @param onClick Lambda to be invoked when any button is clicked, passing the selected
 *   [OAuthProvider].
 * @param allowSingleProviderFullWidth Whether a single provider should render as a full-width
 *   button with text. Defaults to `true`.
 */
@Composable
fun ClerkSocialRow(
  providers: ImmutableList<OAuthProvider>,
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  onClick: (OAuthProvider) -> Unit = {},
  allowSingleProviderFullWidth: Boolean = true,
) {
  val isSingleProvider = providers.size == 1 && allowSingleProviderFullWidth

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(dp8)) {
    val chunks = providers.chunked(MAX_BUTTONS_PER_ROW)

    // Special-case: single provider gets a full-width button with text
    if (isSingleProvider) {
      Row(modifier = Modifier.fillMaxWidth()) {
        ClerkSocialButton(
          provider = providers.first(),
          isEnabled = true,
          onClick = onClick,
          forceIconOnly = false,
          modifier = Modifier.fillMaxWidth(),
          clerkTheme = clerkTheme,
        )
      }
      return@Column
    }

    chunks.forEachIndexed { rowIndex, rowProviders ->
      val isLastRow = rowIndex == chunks.size - 1

      // Build exactly 3 equal-width slots for every row. For the last row, use a brick pattern
      // so that 1 item is centered and 2 items sit at the edges.
      val rowSlots: List<OAuthProvider?> =
        when {
          // Full rows: fill left-to-right, pad trailing with nulls (placeholders)
          !isLastRow || rowProviders.size == MAX_BUTTONS_PER_ROW ->
            rowProviders + List(MAX_BUTTONS_PER_ROW - rowProviders.size) { null }
          // Last row with 1 item: center
          rowProviders.size == 1 -> listOf(null, rowProviders[0], null)
          // Last row with 2 items: no middle spacer, buttons take full width
          rowProviders.size == 2 -> rowProviders
          else -> rowProviders
        }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
          when {
            isLastRow && rowProviders.size == 2 -> Arrangement.spacedBy(dp8)
            else -> Arrangement.spacedBy(dp8, Alignment.CenterHorizontally)
          },
      ) {
        rowSlots.forEach { slotProvider ->
          Box(modifier = Modifier.weight(1f)) {
            if (slotProvider != null) {
              ClerkSocialButton(
                provider = slotProvider,
                isEnabled = true,
                onClick = onClick,
                forceIconOnly = true,
                modifier = Modifier.fillMaxWidth(),
                clerkTheme = clerkTheme,
                expandIconWidth = isLastRow && rowProviders.size == 2,
              )
            }
          }
        }
      }
    }
  }
}

@Preview
@Composable
private fun Preview() {
  Column(verticalArrangement = Arrangement.spacedBy(dp8)) {
    // One provider: full button with text
    ClerkSocialRow(providers = persistentListOf(OAuthProvider.GOOGLE))

    // Multiple providers: icon-only layout
    ClerkSocialRow(providers = persistentListOf(OAuthProvider.GOOGLE, OAuthProvider.FACEBOOK))
  }
}
