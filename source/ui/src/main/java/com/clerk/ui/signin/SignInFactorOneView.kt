package com.clerk.ui.signin

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.factor.isResetFactor
import com.clerk.api.signin.startingFirstFactor
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.auth.SignInFactorScreen
import com.clerk.ui.auth.firstFactorScreen
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
      when (firstFactorScreen(effectiveFactor.strategy)) {
        SignInFactorScreen.Passkey ->
          SignInFactorOnePasskeyView(factor = effectiveFactor, onAuthComplete = onAuthComplete)
        SignInFactorScreen.Password ->
          SignInFactorOnePasswordView(factor = effectiveFactor, onAuthComplete = onAuthComplete)
        SignInFactorScreen.EmailLink ->
          SignInFactorOneEmailLinkView(factor = effectiveFactor, onAuthComplete = onAuthComplete)
        SignInFactorScreen.Code ->
          SignInFactorCodeView(factor = effectiveFactor, onAuthComplete = onAuthComplete)
        SignInFactorScreen.BackupCode,
        SignInFactorScreen.GetHelp -> SignInGetHelpView()
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
      supportedFactors.factorForStrategy(currentSignIn.firstFactorVerification?.strategy)
    } else {
      null
    }
  val fallbackIsSupported = hasSignInContext && supportedFactors.hasStrategy(fallback.strategy)

  return if (!hasSignInContext) {
    fallback
  } else {
    if (fallbackIsSupported) fallback
    else preparedFactor ?: currentSignIn.startingFirstFactor ?: fallback
  }
}

private fun List<Factor>.factorForStrategy(strategy: String?): Factor? {
  val preparedStrategy = strategy?.takeIf { it.isNotBlank() } ?: return null
  return firstOrNull { it.strategy == preparedStrategy }
}

private fun List<Factor>.hasStrategy(strategy: String): Boolean {
  return any { it.strategy == strategy }
}

@PreviewLightDark
@Composable
private fun PreviewSignInComponent() {
  PreviewAuthStateProvider { SignInFactorOneView(factor = Factor("passkey"), onAuthComplete = {}) }
}
