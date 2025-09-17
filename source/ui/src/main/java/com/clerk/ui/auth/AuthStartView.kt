package com.clerk.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.core.common.ClerkThemedAuthScaffold
import com.clerk.ui.core.input.ClerkPhoneNumberField
import com.clerk.ui.core.input.ClerkTextField

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
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  val authViewHelper = AuthViewHelper()

  authViewHelper.setTestValues(
    enabledFirstFactorAttributes = listOf("email_address", "phone_number", "username"),
    applicationName = "Acme Co",
  )

  AuthStartViewImpl(authMode = AuthMode.SignIn, authViewHelper = authViewHelper)
}
