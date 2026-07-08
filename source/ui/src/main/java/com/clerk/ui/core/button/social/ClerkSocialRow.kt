package com.clerk.ui.core.button.social

import androidx.compose.foundation.layout.Arrangement
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

/**
 * A composable row layout for displaying multiple social authentication buttons.
 *
 * This component uses the same layout rules as Clerk's web components: social providers are evenly
 * distributed into rows of up to five buttons, a last-used provider can be separated into its own
 * first row, and one/two-provider rows render as full-width block buttons on mobile.
 *
 * @param providers List of [OAuthProvider]s to display as social login buttons.
 * @param modifier Optional [Modifier] for theming and styling.
 * @param clerkTheme Optional [ClerkTheme] for theming.
 * @param onClick Lambda to be invoked when any button is clicked, passing the selected
 *   [OAuthProvider].
 * @param allowSingleProviderFullWidth Whether a single provider should render as a full-width
 *   button with text. Defaults to `true`.
 * @param lastUsedProvider Optional provider to separate into its own first row.
 */
@Composable
fun ClerkSocialRow(
  providers: ImmutableList<OAuthProvider>,
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  onClick: (OAuthProvider) -> Unit = {},
  allowSingleProviderFullWidth: Boolean = true,
  lastUsedProvider: OAuthProvider? = null,
) {
  val socialButtonRows =
    distributeSocialButtonsIntoRows(providers = providers, lastUsedProvider = lastUsedProvider)

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(dp8)) {
    socialButtonRows.rows.forEach { rowProviders ->
      val shouldUseBlockButtons =
        allowSingleProviderFullWidth &&
          (rowProviders.size == 1 ||
            (!socialButtonRows.lastUsedProviderPresent && providers.size == 2))

      if (shouldUseBlockButtons) {
        rowProviders.forEach { provider ->
          ClerkSocialButton(
            provider = provider,
            isEnabled = true,
            onClick = onClick,
            forceIconOnly = false,
            modifier = Modifier.fillMaxWidth(),
            clerkTheme = clerkTheme,
          )
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(dp8, Alignment.CenterHorizontally),
        ) {
          rowProviders.forEach { provider ->
            ClerkSocialButton(
              provider = provider,
              isEnabled = true,
              onClick = onClick,
              forceIconOnly = true,
              modifier = Modifier.weight(1f),
              clerkTheme = clerkTheme,
            )
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
