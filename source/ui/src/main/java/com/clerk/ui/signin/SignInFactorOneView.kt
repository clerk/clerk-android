package com.clerk.ui.signin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.network.model.factor.Factor
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun SignInFactorOneComponent(factor: Factor, modifier: Modifier = Modifier) {

  ClerkMaterialTheme {
    when (factor.strategy) {
      "passkey" -> TODO()
      "password" -> SignInFactorOnePasswordView(onContinue = {})
      "email_code",
      "phone_code",
      "reset_password_email_code",
      "reset_password_phone_code" -> TODO()
      else -> TODO()
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewSignInComponent() {
  SignInFactorOneComponent(Factor("passkey"))
}
