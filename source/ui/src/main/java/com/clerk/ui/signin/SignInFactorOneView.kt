package com.clerk.ui.signin

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.api.auth.types.Strategy
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.factor.isResetFactor
import com.clerk.api.signin.startingFirstFactor
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.signin.code.SignInFactorCodeView
import com.clerk.ui.signin.emaillink.SignInFactorOneEmailLinkView
import com.clerk.ui.signin.help.SignInGetHelpView
import com.clerk.ui.signin.passkey.SignInFactorOnePasskeyView
import com.clerk.ui.signin.password.set.SignInFactorOnePasswordView
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkThemeOverrideProvider

@Composable
public fun SignInFactorOneView(
  factor: Factor,
  clerkTheme: ClerkTheme? = null,
  onAuthComplete: () -> Unit,
) {
  val effectiveFactor = resolveFirstFactor(factor)
  ClerkThemeOverrideProvider(clerkTheme) {
    ClerkMaterialTheme {
      when (effectiveFactor.strategyType) {
        Strategy.Passkey ->
          SignInFactorOnePasskeyView(factor = effectiveFactor, onAuthComplete = onAuthComplete)
        Strategy.Password ->
          SignInFactorOnePasswordView(factor = effectiveFactor, onAuthComplete = onAuthComplete)
        Strategy.EmailLink ->
          SignInFactorOneEmailLinkView(factor = effectiveFactor, onAuthComplete = onAuthComplete)
        Strategy.EmailCode,
        Strategy.PhoneCode,
        Strategy.ResetPasswordPhoneCode,
        Strategy.ResetPasswordEmailCode ->
          SignInFactorCodeView(factor = effectiveFactor, onAuthComplete = onAuthComplete)
        else -> SignInGetHelpView()
      }
    }
  }
}

internal fun resolveFirstFactor(fallback: Factor): Factor {
  if (fallback.isResetFactor()) return fallback

  val currentSignIn = Clerk.auth.currentSignIn
  val supportedFactors = currentSignIn?.supportedFirstFactors.orEmpty()
  val hasSignInContext = currentSignIn != null && supportedFactors.isNotEmpty()

  val preparedFactor =
    if (hasSignInContext) {
      supportedFactors.factorForStrategy(currentSignIn.firstFactorVerification?.strategyType)
    } else {
      null
    }
  val fallbackIsSupported = hasSignInContext && supportedFactors.hasStrategy(fallback.strategyType)

  return if (!hasSignInContext) {
    fallback
  } else {
    if (fallbackIsSupported) fallback
    else preparedFactor ?: currentSignIn.startingFirstFactor ?: fallback
  }
}

private fun List<Factor>.factorForStrategy(strategy: Strategy?): Factor? {
  val preparedStrategy = strategy?.takeIf { it.value.isNotBlank() } ?: return null
  return firstOrNull { it.strategyType == preparedStrategy }
}

private fun List<Factor>.hasStrategy(strategy: Strategy): Boolean {
  return any { it.strategyType == strategy }
}

@PreviewLightDark
@Composable
private fun PreviewSignInComponent() {
  PreviewAuthStateProvider { SignInFactorOneView(factor = Factor("passkey"), onAuthComplete = {}) }
}
