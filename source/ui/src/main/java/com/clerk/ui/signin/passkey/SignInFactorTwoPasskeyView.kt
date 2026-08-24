package com.clerk.ui.signin.passkey

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.theme.ClerkThemeOverrideProvider

/** Displays passkey authentication for the second factor of an in-progress sign-in. */
@Composable
fun SignInFactorTwoPasskeyView(
  factor: Factor,
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  onAuthComplete: () -> Unit,
) {
  ClerkThemeOverrideProvider(clerkTheme) {
    SignInFactorOnePasskeyViewImpl(
      modifier = modifier,
      factor = factor,
      isSecondFactor = true,
      onAuthComplete = onAuthComplete,
    )
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  PreviewAuthStateProvider {
    SignInFactorTwoPasskeyView(
      factor = Factor(strategy = StrategyKeys.PASSKEY),
      onAuthComplete = {},
    )
  }
}
