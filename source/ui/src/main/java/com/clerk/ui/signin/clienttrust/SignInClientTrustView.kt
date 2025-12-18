package com.clerk.ui.signin.clienttrust

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.signin.code.FactorMode
import com.clerk.ui.signin.code.SignInFactorCodeView
import com.clerk.ui.signin.help.SignInGetHelpView
import com.clerk.ui.theme.ClerkThemeOverrideProvider

@Composable
fun SignInClientTrustView(
  factor: Factor,
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  onAuthComplete: () -> Unit,
) {
  ClerkThemeOverrideProvider(clerkTheme) {
    when (factor.strategy) {
      StrategyKeys.PHONE_CODE,
      StrategyKeys.EMAIL_CODE ->
        SignInFactorCodeView(
          factor = factor,
          mode = FactorMode.ClientTrust,
          modifier = modifier,
          onAuthComplete = onAuthComplete,
        )
      else -> SignInGetHelpView(modifier = modifier)
    }
  }
}

