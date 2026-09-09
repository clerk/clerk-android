package com.clerk.ui.signin

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.ui.auth.FactorSelection
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.auth.firstFactorChoices
import com.clerk.ui.auth.isResetFactor
import com.clerk.ui.auth.startingFirstFactor
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.core.composition.LocalClerk
import com.clerk.ui.signin.code.SignInFactorCodeView
import com.clerk.ui.signin.emaillink.SignInFactorOneEmailLinkView
import com.clerk.ui.signin.help.SignInGetHelpView
import com.clerk.ui.signin.passkey.SignInFactorOnePasskeyView
import com.clerk.ui.signin.password.set.SignInFactorOnePasswordView
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkTheme
import com.clerk.ui.theme.ClerkThemeOverrideProvider

@Composable
fun SignInFactorOneView(
  factor: FactorSelection,
  clerkTheme: ClerkTheme? = null,
  onAuthComplete: () -> Unit,
) {
  val effectiveFactor = resolveFirstFactor(LocalClerk.current, factor)
  ClerkThemeOverrideProvider(clerkTheme) {
    ClerkMaterialTheme {
      when (effectiveFactor.strategy) {
        StrategyKeys.PASSKEY ->
          SignInFactorOnePasskeyView(factor = effectiveFactor, onAuthComplete = onAuthComplete)
        StrategyKeys.PASSWORD ->
          SignInFactorOnePasswordView(factor = effectiveFactor, onAuthComplete = onAuthComplete)
        StrategyKeys.EMAIL_LINK ->
          SignInFactorOneEmailLinkView(factor = effectiveFactor, onAuthComplete = onAuthComplete)
        StrategyKeys.EMAIL_CODE,
        StrategyKeys.PHONE_CODE,
        StrategyKeys.RESET_PASSWORD_PHONE_CODE,
        StrategyKeys.RESET_PASSWORD_EMAIL_CODE ->
          SignInFactorCodeView(factor = effectiveFactor, onAuthComplete = onAuthComplete)
        else -> SignInGetHelpView()
      }
    }
  }
}

internal fun resolveFirstFactor(clerk: Clerk, fallback: FactorSelection): FactorSelection {
  if (fallback.isResetFactor()) return fallback

  val currentSignIn = clerk.signIn.takeIf { it.id != null }
  val supportedFactors = currentSignIn?.firstFactorChoices.orEmpty()
  val hasSignInContext = currentSignIn != null && supportedFactors.isNotEmpty()

  val preparedFactor =
    if (hasSignInContext) {
      supportedFactors.factorForStrategy(currentSignIn.firstFactorVerification?.strategy)
    } else {
      null
    }
  val fallbackIsSupported = hasSignInContext && supportedFactors.hasStrategy(fallback.strategy)

  return if (!hasSignInContext) {
    fallback
  } else {
    if (fallbackIsSupported) fallback
    else preparedFactor ?: currentSignIn.startingFirstFactor(clerk) ?: fallback
  }
}

private fun List<FactorSelection>.factorForStrategy(strategy: String?): FactorSelection? {
  val preparedStrategy = strategy?.takeIf { it.isNotBlank() } ?: return null
  return firstOrNull { it.strategy == preparedStrategy }
}

private fun List<FactorSelection>.hasStrategy(strategy: String): Boolean {
  return any { it.strategy == strategy }
}

@PreviewLightDark
@Composable
private fun PreviewSignInComponent() {
  PreviewAuthStateProvider {
    SignInFactorOneView(factor = FactorSelection("passkey"), onAuthComplete = {})
  }
}
