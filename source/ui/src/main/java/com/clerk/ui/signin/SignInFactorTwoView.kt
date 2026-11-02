package com.clerk.ui.signin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.auth.types.Strategy
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.auth.SignInFactorScreen
import com.clerk.ui.auth.secondFactorScreen
import com.clerk.ui.signin.backupcode.SignInFactorTwoBackupCodeView
import com.clerk.ui.signin.code.SignInFactorCodeView
import com.clerk.ui.signin.help.SignInGetHelpView
import com.clerk.ui.signin.passkey.SignInFactorTwoPasskeyView
import com.clerk.ui.theme.ClerkThemeOverrideProvider

@Composable
internal fun SignInFactorTwoView(
  factor: Factor,
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  onAuthComplete: () -> Unit,
) {
  ClerkThemeOverrideProvider(clerkTheme) {
    when (secondFactorScreen(factor.strategyType)) {
      SignInFactorScreen.Code ->
        SignInFactorCodeView(
          factor = factor,
          isSecondFactor = true,
          modifier = modifier,
          onAuthComplete = onAuthComplete,
        )
      SignInFactorScreen.BackupCode ->
        SignInFactorTwoBackupCodeView(
          modifier = modifier,
          factor = factor,
          onAuthComplete = onAuthComplete,
        )
      SignInFactorScreen.Passkey ->
        SignInFactorTwoPasskeyView(
          factor = factor,
          modifier = modifier,
          onAuthComplete = onAuthComplete,
        )
      SignInFactorScreen.Password,
      SignInFactorScreen.EmailLink,
      SignInFactorScreen.GetHelp -> SignInGetHelpView(modifier = modifier)
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  PreviewAuthStateProvider {
    SignInFactorTwoView(factor = Factor(Strategy.Totp.value), onAuthComplete = {})
  }
}
