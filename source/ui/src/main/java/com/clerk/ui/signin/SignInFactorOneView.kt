package com.clerk.ui.signin

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.network.model.factor.Factor
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.signin.code.SignInFactorCodeView
import com.clerk.ui.signin.help.SignInGetHelpView
import com.clerk.ui.signin.passkey.SignInFactorOnePasskeyView
import com.clerk.ui.signin.password.set.SignInFactorOnePasswordView
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun SignInFactorOneView(factor: Factor, onBackPressed: () -> Unit) {

  ClerkMaterialTheme {
    when (factor.strategy) {
      StrategyKeys.PASSKEY -> SignInFactorOnePasskeyView(factor = factor)
      StrategyKeys.PASSWORD -> SignInFactorOnePasswordView(onContinue = {}, email = "sam@clerk.dev")
      StrategyKeys.EMAIL_CODE,
      StrategyKeys.PHONE_CODE,
      StrategyKeys.RESET_PASSWORD_PHONE_CODE,
      StrategyKeys.RESET_PASSWORD_EMAIL_CODE -> SignInFactorCodeView(factor = factor)
      else -> SignInGetHelpView(onBackPressed = onBackPressed)
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewSignInComponent() {
  SignInFactorOneView(Factor("passkey"), onBackPressed = {})
}
