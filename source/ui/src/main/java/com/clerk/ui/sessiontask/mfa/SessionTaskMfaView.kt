package com.clerk.ui.sessiontask.mfa

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.api.signin.startingSecondFactor
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.core.common.StrategyKeys
import com.clerk.ui.signin.backupcode.SignInFactorTwoBackupCodeView
import com.clerk.ui.signin.code.SignInFactorCodeView
import com.clerk.ui.signin.help.SignInGetHelpView
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun SessionTaskMfaView(
  modifier: Modifier = Modifier,
  onAuthComplete: () -> Unit,
) {
  val factor = Clerk.auth.currentSignIn?.startingSecondFactor
  if (factor == null) {
    SignInGetHelpView(modifier = modifier)
    return
  }

  when (factor.strategy) {
    StrategyKeys.TOTP,
    StrategyKeys.PHONE_CODE,
    StrategyKeys.EMAIL_CODE ->
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

@PreviewLightDark
@Composable
private fun PreviewSessionTaskMfaView() {
  PreviewAuthStateProvider {
    ClerkMaterialTheme { SessionTaskMfaView(onAuthComplete = {}) }
  }
}
