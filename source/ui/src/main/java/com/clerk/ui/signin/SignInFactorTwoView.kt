package com.clerk.ui.signin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.signin.backupcode.SignInFactorTwoBackupCodeView
import com.clerk.ui.signin.code.SignInFactorCodeView
import com.clerk.ui.signin.help.SignInGetHelpView
import com.clerk.ui.theme.ClerkThemeOverrideProvider

/**
 * A composable that acts as a router for displaying the appropriate second-factor authentication
 * view.
 *
 * Based on the `strategy` of the provided [factor], this composable will delegate rendering to the
 * corresponding view, such as [SignInFactorCodeView] for TOTP or phone code, or
 * [SignInFactorTwoBackupCodeView] for backup codes.
 *
 * @param factor The second factor to be verified.
 * @param modifier The [Modifier] to be applied to the view.
 */
@Composable
fun SignInFactorTwoView(
  factor: Factor,
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  onAuthComplete: () -> Unit,
) {
  ClerkThemeOverrideProvider(clerkTheme) {
    when (factor.strategy) {
      StrategyKeys.TOTP,
      StrategyKeys.PHONE_CODE ->
        SignInFactorCodeView(
          factor = factor,
          isSecondFactor = true,
          modifier = modifier,
          onAuthComplete = onAuthComplete,
        )
      StrategyKeys.BACKUP_CODE ->
        SignInFactorTwoBackupCodeView(
          modifier = modifier,
          factor = factor,
          onAuthComplete = onAuthComplete,
        )
      else -> SignInGetHelpView(modifier = modifier)
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  PreviewAuthStateProvider {
    SignInFactorTwoView(factor = Factor(StrategyKeys.TOTP), onAuthComplete = {})
  }
}
