package com.clerk.ui.auth

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.core.button.social.ClerkSocialRow
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.common.ClerkThemedAuthScaffold
import com.clerk.ui.core.common.dimens.dp24
import com.clerk.ui.core.divider.TextDivider
import com.clerk.ui.core.input.ClerkPhoneNumberField
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import kotlinx.collections.immutable.toImmutableList

@Composable
fun AuthStartView(authMode: AuthMode, modifier: Modifier = Modifier) {
  AuthStartViewImpl(authMode = authMode, modifier = modifier)
}

@Composable
private fun AuthStartViewImpl(
  authMode: AuthMode,
  modifier: Modifier = Modifier,
  authViewHelper: AuthViewHelper = AuthViewHelper(),
) {
  var authStartPhoneNumber by rememberSaveable { mutableStateOf("") }
  var authStartIdentifier by rememberSaveable { mutableStateOf("") }

  ClerkThemedAuthScaffold(
    modifier = modifier,
    hasBackButton = false,
    title = authViewHelper.titleString(authMode),
    subtitle = authViewHelper.subtitleString(authMode),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(dp24, alignment = Alignment.CenterVertically),
    ) {
      if (authViewHelper.showIdentifierField) {
        if (authViewHelper.phoneNumberIsEnabled) {
          ClerkPhoneNumberField(
            value = authStartPhoneNumber,
            modifier = Modifier,
            onValueChange = { authStartPhoneNumber = it },
          )
        } else {
          ClerkTextField(
            value = authStartIdentifier,
            onValueChange = { authStartIdentifier = it },
            label = authViewHelper.emailOrUsernamePlaceholder(),
          )
        }
        ClerkButton(
          modifier = Modifier.fillMaxWidth(),
          text = "Continue",
          icons =
            ClerkButtonDefaults.icons(
              trailingIcon = R.drawable.ic_triangle_right,
              trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
            ),
          onClick = {},
        )
        if (authViewHelper.showIdentifierSwitcher) {
          ClerkTextButton(text = authViewHelper.identifierSwitcherString(false), onClick = {})
        }
        if (authViewHelper.showOrDivider) {
          TextDivider(stringResource(R.string.or))
        }
        ClerkSocialRow(
          providers = authViewHelper.authenticatableSocialProviders.toImmutableList(),
          onClick = {},
        )
      }
    }
  }
}

@SuppressLint("VisibleForTests")
@PreviewLightDark
@Composable
private fun Preview() {
  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
  val authViewHelper = AuthViewHelper()

  authViewHelper.setTestValues(
    enabledFirstFactorAttributes = listOf("email_address", "phone_number", "username"),
    applicationName = "Acme Co",
    socialProviders = listOf(OAuthProvider.GOOGLE, OAuthProvider.APPLE, OAuthProvider.FACEBOOK),
  )

  AuthStartViewImpl(authMode = AuthMode.SignIn, authViewHelper = authViewHelper)
}
