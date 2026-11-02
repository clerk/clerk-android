package com.clerk.ui.signin.clienttrust

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.auth.types.Strategy
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.auth.SignInFactorScreen
import com.clerk.ui.auth.clientTrustScreen
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.signin.code.SignInFactorCodeView
import com.clerk.ui.signin.help.SignInGetHelpView
import com.clerk.ui.signin.passkey.SignInFactorTwoPasskeyView
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkThemeOverrideProvider

@Composable
internal fun SignInClientTrustView(
  factor: Factor,
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  onAuthComplete: () -> Unit,
) {
  ClerkThemeOverrideProvider(clerkTheme) {
    when (clientTrustScreen(factor.strategyType)) {
      SignInFactorScreen.Code ->
        SignInFactorCodeView(
          factor = factor,
          isSecondFactor = true,
          isClientTrust = true,
          modifier = modifier,
          onAuthComplete = onAuthComplete,
        )
      SignInFactorScreen.Passkey ->
        SignInFactorTwoPasskeyView(
          factor = factor,
          modifier = modifier,
          onAuthComplete = onAuthComplete,
        )
      SignInFactorScreen.Password,
      SignInFactorScreen.EmailLink,
      SignInFactorScreen.BackupCode,
      SignInFactorScreen.GetHelp -> SignInGetHelpView(modifier = modifier)
    }
  }
}

@Composable
internal fun ClientTrustWarningMessage(modifier: Modifier = Modifier) {
  Text(
    text = stringResource(R.string.signing_in_from_new_device),
    color = ClerkMaterialTheme.colors.warning,
    style = ClerkMaterialTheme.typography.bodySmall,
    textAlign = TextAlign.Center,
    modifier = modifier.padding(bottom = dp16),
  )
}

@PreviewLightDark
@Composable
private fun Preview() {
  PreviewAuthStateProvider {
    SignInClientTrustView(
      factor = Factor(Strategy.EmailCode.value, safeIdentifier = "user@example.com"),
      onAuthComplete = {},
    )
  }
}
