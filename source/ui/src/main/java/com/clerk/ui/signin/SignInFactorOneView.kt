package com.clerk.ui.signin

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.startingFirstFactor
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.signin.code.SignInFactorCodeView
import com.clerk.ui.signin.emaillink.SignInFactorOneEmailLinkView
import com.clerk.ui.signin.help.SignInGetHelpView
import com.clerk.ui.signin.passkey.SignInFactorOnePasskeyView
import com.clerk.ui.signin.password.set.SignInFactorOnePasswordView
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkThemeOverrideProvider

@Composable
fun SignInFactorOneView(
  factor: Factor,
  clerkTheme: ClerkTheme? = null,
  onAuthComplete: () -> Unit,
) {
  val effectiveFactor = resolveFirstFactor(factor)
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

internal fun resolveFirstFactor(fallback: Factor): Factor {
  val currentSignIn = Clerk.auth.currentSignIn
  val supportedFactors = currentSignIn?.supportedFirstFactors.orEmpty()
  val hasSignInContext = currentSignIn != null && supportedFactors.isNotEmpty()

  val preparedFactor =
    if (hasSignInContext) {
      supportedFactors.factorForStrategy(currentSignIn.firstFactorVerification?.strategy)
    } else {
      null
    }
  val emailLinkFactor =
    if (hasSignInContext) {
      currentSignIn.preferredEmailLinkFactor(fallback, supportedFactors)
    } else {
      null
    }
  val fallbackIsSupported = hasSignInContext && supportedFactors.hasStrategy(fallback.strategy)

  return if (!hasSignInContext) {
    fallback
  } else {
    emailLinkFactor
      ?: preparedFactor
      ?: if (fallbackIsSupported) fallback else currentSignIn.startingFirstFactor ?: fallback
  }
}

private fun List<Factor>.factorForStrategy(strategy: String?): Factor? {
  val preparedStrategy = strategy?.takeIf { it.isNotBlank() } ?: return null
  return firstOrNull { it.strategy == preparedStrategy }
}

private fun List<Factor>.hasStrategy(strategy: String): Boolean {
  return any { it.strategy == strategy }
}

private fun SignIn.preferredEmailLinkFactor(
  fallback: Factor,
  supportedFactors: List<Factor>,
): Factor? {
  return if (isEmailIdentifierSignIn(fallback, supportedFactors)) {
    supportedFactors.firstOrNull { it.strategy == StrategyKeys.EMAIL_LINK }
  } else {
    null
  }
}

private fun SignIn.isEmailIdentifierSignIn(
  fallback: Factor,
  supportedFactors: List<Factor>,
): Boolean {
  return (fallback.strategy == StrategyKeys.EMAIL_CODE && fallback.emailAddressId != null) ||
    identifier?.contains("@") == true ||
    supportedFactors.any {
      (it.strategy == StrategyKeys.EMAIL_LINK || it.strategy == StrategyKeys.EMAIL_CODE) &&
        it.safeIdentifier?.contains("@") == true
    }
}

@PreviewLightDark
@Composable
private fun PreviewSignInComponent() {
  PreviewAuthStateProvider { SignInFactorOneView(factor = Factor("passkey"), onAuthComplete = {}) }
}
