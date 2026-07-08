package com.clerk.ui.core.button.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.core.dimens.dp8
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private const val SOCIAL_BUTTON_BLOCK_THRESHOLD = 2

/**
 * A composable row layout for displaying multiple social authentication buttons.
 *
 * This component follows the Clerk web social button layout: one or two providers render as
 * full-width buttons with text, while larger provider sets render as balanced icon-only rows with
 * up to five providers per row. Four providers are shown as a 2x2 grid.
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
  val showBlockButtons =
    providers.size in 1..SOCIAL_BUTTON_BLOCK_THRESHOLD &&
      (providers.size > 1 || allowSingleProviderFullWidth)

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(dp8)) {
    if (showBlockButtons) {
      providers.forEach { provider ->
        ClerkSocialButton(
          provider = provider,
          isEnabled = true,
          onClick = onClick,
          forceIconOnly = false,
          modifier = Modifier.fillMaxWidth(),
          clerkTheme = clerkTheme,
        )
      }
      return@Column
    }

    val providerRows = distributeSocialProvidersIntoRows(providers)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val firstRowProviderCount = providerRows.firstOrNull()?.size ?: 1
      val firstRowButtonWidth =
        (maxWidth - (dp8 * (firstRowProviderCount - 1))) / firstRowProviderCount

      Column(verticalArrangement = Arrangement.spacedBy(dp8)) {
        providerRows.forEachIndexed { rowIndex, rowProviders ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dp8, Alignment.CenterHorizontally),
          ) {
            rowProviders.forEach { provider ->
              Box(
                modifier =
                  if (rowIndex == 0) {
                    Modifier.weight(1f)
                  } else {
                    Modifier.width(firstRowButtonWidth)
                  }
              ) {
                ClerkSocialButton(
                  provider = provider,
                  isEnabled = true,
                  onClick = onClick,
                  forceIconOnly = true,
                  modifier = Modifier.fillMaxWidth(),
                  clerkTheme = clerkTheme,
                  expandIconWidth = true,
                )
              }
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

    // Four providers: 2x2 icon grid
    ClerkSocialRow(
      providers =
        persistentListOf(
          OAuthProvider.GOOGLE,
          OAuthProvider.APPLE,
          OAuthProvider.FACEBOOK,
          OAuthProvider.GITHUB,
        )
    )
  }
}
