package com.clerk.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.clerk.api.Clerk
import com.clerk.ui.R

class AuthViewHelper {

  @Composable
  fun titleString(authMode: AuthMode): String {
    return when (authMode) {
      AuthMode.SignIn,
      AuthMode.SignInOrUp -> {
        val appName = Clerk.applicationName
        if (appName != null) {
          stringResource(R.string.continue_to, appName)
        } else {
          stringResource(R.string.continue_text)
        }
      }
      AuthMode.SignUp -> stringResource(R.string.create_your_account)
    }
  }
}
