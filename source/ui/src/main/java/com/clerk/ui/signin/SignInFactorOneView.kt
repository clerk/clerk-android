package com.clerk.ui.signin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.network.model.factor.Factor
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.signin.password.SignInFactorOnePasswordView
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun SignInFactorOneComponent(factor: Factor, modifier: Modifier = Modifier) {

  ClerkMaterialTheme {
    when (factor.strategy) {
      StrategyKeys.PASSKEY -> TODO()
      StrategyKeys.PASSWORD -> SignInFactorOnePasswordView(onContinue = {}, email = "sam@clerk.dev")
      StrategyKeys.EMAIL_CODE,
      StrategyKeys.PHONE_CODE,
      StrategyKeys.RESET_PASSWORD_PHONE_CODE,
      StrategyKeys.RESET_PASSWORD_EMAIL_CODE -> TODO()
      else -> TODO()
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewSignInComponent() {
  SignInFactorOneComponent(Factor("passkey"))
}
