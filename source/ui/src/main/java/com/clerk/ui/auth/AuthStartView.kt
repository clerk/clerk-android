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
import androidx.lifecycle.viewmodel.compose.viewModel
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
  authViewModel: AuthViewModel = viewModel(),
) {
  var authStartPhoneNumber by rememberSaveable { mutableStateOf("") }
  var authStartIdentifier by rememberSaveable { mutableStateOf("") }
  var phoneNumberFieldIsActive by rememberSaveable { mutableStateOf(false) }

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
        AuthInputField(
          authViewHelper = authViewHelper,
          phoneNumberFieldIsActive = phoneNumberFieldIsActive,
          authStartPhoneNumber = authStartPhoneNumber,
          authStartIdentifier = authStartIdentifier,
          onPhoneNumberChange = { authStartPhoneNumber = it },
          onIdentifierChange = { authStartIdentifier = it },
        )

        AuthActionButtons(
          authMode = authMode,
          authViewModel = authViewModel,
          authViewHelper = authViewHelper,
          authStartIdentifier = authStartIdentifier,
          authStartPhoneNumber = authStartPhoneNumber,
          phoneNumberFieldIsActive = phoneNumberFieldIsActive,
          onPhoneNumberFieldToggle = { phoneNumberFieldIsActive = !phoneNumberFieldIsActive },
        )
      }
    }
  }
}

@Composable
private fun AuthInputField(
  authViewHelper: AuthViewHelper,
  phoneNumberFieldIsActive: Boolean,
  authStartPhoneNumber: String,
  authStartIdentifier: String,
  onPhoneNumberChange: (String) -> Unit,
  onIdentifierChange: (String) -> Unit,
) {
  if (authViewHelper.phoneNumberIsEnabled && phoneNumberFieldIsActive) {
    ClerkPhoneNumberField(
      value = authStartPhoneNumber,
      modifier = Modifier,
      onValueChange = onPhoneNumberChange,
    )
  } else {
    ClerkTextField(
      value = authStartIdentifier,
      onValueChange = onIdentifierChange,
      label = authViewHelper.emailOrUsernamePlaceholder(),
    )
  }
}

@Composable
private fun AuthActionButtons(
  authMode: AuthMode,
  authViewModel: AuthViewModel,
  authViewHelper: AuthViewHelper,
  authStartIdentifier: String,
  authStartPhoneNumber: String,
  phoneNumberFieldIsActive: Boolean,
  onPhoneNumberFieldToggle: () -> Unit,
) {
  ClerkButton(
    modifier = Modifier.fillMaxWidth(),
    text = stringResource(R.string.continue_text),
    icons =
      ClerkButtonDefaults.icons(
        trailingIcon = R.drawable.ic_triangle_right,
        trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
      ),
    onClick = {
      authViewModel.startAuth(
        authMode = authMode,
        identifier = authStartIdentifier,
        phoneNumber = authStartPhoneNumber,
        isPhoneNumberFieldActive = phoneNumberFieldIsActive,
      )
    },
  )

  if (authViewHelper.showIdentifierSwitcher) {
    ClerkTextButton(
      text = authViewHelper.identifierSwitcherString(phoneNumberFieldIsActive),
      onClick = onPhoneNumberFieldToggle,
    )
  }

  if (authViewHelper.showOrDivider) {
    TextDivider(stringResource(R.string.or))
  }

  ClerkSocialRow(
    providers = authViewHelper.authenticatableSocialProviders.toImmutableList(),
    onClick = { authViewModel.authenticateWithSocialProvider(it) },
  )
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
