package com.clerk.ui.signin

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.network.model.factor.Factor
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.signin.code.SignInFactorCodeView
import com.clerk.ui.signin.help.SignInGetHelpView
import com.clerk.ui.signin.passkey.SignInFactorOnePasskeyView
import com.clerk.ui.signin.password.set.SignInFactorOnePasswordView
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun SignInFactorOneView(factor: Factor, onAuthComplete: () -> Unit) {

  ClerkMaterialTheme {
    when (factor.strategy) {
      StrategyKeys.PASSKEY ->
        SignInFactorOnePasskeyView(factor = factor, onAuthComplete = onAuthComplete)
      StrategyKeys.PASSWORD ->
        SignInFactorOnePasswordView(factor = factor, onAuthComplete = onAuthComplete)
      StrategyKeys.EMAIL_CODE,
      StrategyKeys.PHONE_CODE,
      StrategyKeys.RESET_PASSWORD_PHONE_CODE,
      StrategyKeys.RESET_PASSWORD_EMAIL_CODE ->
        SignInFactorCodeView(factor = factor, onAuthComplete = onAuthComplete)
      else -> SignInGetHelpView()
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewSignInComponent() {
  PreviewAuthStateProvider { SignInFactorOneView(Factor("passkey"), onAuthComplete = {}) }
}
