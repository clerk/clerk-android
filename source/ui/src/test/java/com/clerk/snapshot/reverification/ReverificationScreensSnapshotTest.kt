package com.clerk.snapshot.reverification

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clerk.api.Clerk
import com.clerk.api.auth.types.Strategy
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.ui.ClerkTheme
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.auth.VerificationUiState
import com.clerk.ui.reverification.PasswordResetReason
import com.clerk.ui.reverification.ReverificationAlternativeMethodsScreen
import com.clerk.ui.reverification.ReverificationBackupCodeScreen
import com.clerk.ui.reverification.ReverificationBiometricScreen
import com.clerk.ui.reverification.ReverificationChrome
import com.clerk.ui.reverification.ReverificationCodeScreen
import com.clerk.ui.reverification.ReverificationFailedScreen
import com.clerk.ui.reverification.ReverificationGetHelpScreen
import com.clerk.ui.reverification.ReverificationNewPasswordScreen
import com.clerk.ui.reverification.ReverificationPasskeyScreen
import com.clerk.ui.reverification.ReverificationPasswordScreen
import com.clerk.ui.reverification.ReverificationScreenState
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class ReverificationScreensSnapshotTest : BaseSnapshotTest() {

  private fun snapshot(
    canGoBack: Boolean = false,
    content: @Composable (ReverificationChrome) -> Unit,
  ) {
    paparazzi.snapshot {
      Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
      Box(Modifier.size(740.dp)) {
        ClerkMaterialTheme {
          content(
            ReverificationChrome(
              snackbarHostState = SnackbarHostState(),
              canGoBack = canGoBack,
              onBack = {},
              onClose = {},
            )
          )
        }
      }
    }
  }

  @Test
  fun password() = snapshot {
    ReverificationPasswordScreen(
      chrome = it,
      password = "",
      state = ReverificationScreenState(),
      onPasswordChange = {},
      onSubmit = {},
      onResetPassword = null,
      onUseAnotherMethod = {},
    )
  }

  @Test
  fun pwnedPasswordOffersAReset() = snapshot {
    ReverificationPasswordScreen(
      chrome = it,
      password = "password123",
      state =
        ReverificationScreenState(
          fieldError = "Password has been found in an online data breach.",
          passwordResetReason = PasswordResetReason.PWNED,
        ),
      onPasswordChange = {},
      onSubmit = {},
      onResetPassword = {},
      onUseAnotherMethod = {},
    )
  }

  @Test
  fun emailCode() = snapshot {
    ReverificationCodeScreen(
      chrome = it,
      factor = Factor(strategy = Strategy.EmailCode.value, safeIdentifier = "sam@clerk.dev"),
      codeState = VerificationUiState.Idle,
      onCodeChange = {},
      onResend = {},
      onUseAnotherMethod = {},
    )
  }

  @Test
  fun incorrectMfaPhoneCode() = snapshot {
    ReverificationCodeScreen(
      chrome = it,
      factor = Factor(strategy = Strategy.PhoneCode.value, safeIdentifier = "+15555550100"),
      codeState = VerificationUiState.IncorrectCode,
      onCodeChange = {},
      onResend = {},
      onUseAnotherMethod = {},
    )
  }

  @Test
  fun totp() = snapshot {
    ReverificationCodeScreen(
      chrome = it,
      factor = Factor(strategy = Strategy.Totp.value),
      codeState = VerificationUiState.Idle,
      onCodeChange = {},
      onResend = {},
      onUseAnotherMethod = null,
    )
  }

  @Test
  fun resetPasswordCode() =
    snapshot(canGoBack = true) {
      ReverificationCodeScreen(
        chrome = it,
        factor =
          Factor(
            strategy = Strategy.ResetPasswordEmailCode.value,
            safeIdentifier = "sam@clerk.dev",
          ),
        codeState = VerificationUiState.Idle,
        onCodeChange = {},
        onResend = {},
        onUseAnotherMethod = null,
      )
    }

  @Test
  fun passkey() = snapshot {
    ReverificationPasskeyScreen(chrome = it, isLoading = false, onContinue = {}) {}
  }

  @Test
  fun biometrics() = snapshot {
    ReverificationBiometricScreen(chrome = it, isLoading = false, onContinue = {}) {}
  }

  @Test
  fun backupCode() = snapshot {
    ReverificationBackupCodeScreen(
      chrome = it,
      code = "",
      state = ReverificationScreenState(),
      onCodeChange = {},
      onSubmit = {},
      onUseAnotherMethod = {},
    )
  }

  @Test
  fun alternativeMethods() =
    snapshot(canGoBack = true) {
      ReverificationAlternativeMethodsScreen(
        chrome = it,
        factors =
          persistentListOf(
            Factor(strategy = Strategy.Passkey.value),
            Factor(strategy = Strategy.TrustedDevice.value),
            Factor(strategy = Strategy.EmailCode.value, safeIdentifier = "sam@clerk.dev"),
            Factor(strategy = Strategy.PhoneCode.value, safeIdentifier = "+15555550100"),
            Factor(strategy = Strategy.Password.value),
          ),
        onSelect = {},
      )
    }

  @Test
  fun newPassword() =
    snapshot(canGoBack = true) {
      ReverificationNewPasswordScreen(
        chrome = it,
        reason = PasswordResetReason.COMPROMISED,
        newPassword = "new-password",
        confirmNewPassword = "new-pass",
        signOutOfOtherSessions = true,
        error = null,
        onNewPasswordChange = {},
        onConfirmNewPasswordChange = {},
        onSignOutOfOtherSessionsChange = {},
        onContinue = {},
      )
    }

  @Test fun getHelp() = snapshot { ReverificationGetHelpScreen(chrome = it, onEmailSupport = {}) }

  @Test
  fun failedToStart() = snapshot {
    ReverificationFailedScreen(chrome = it, message = "Something went wrong. Please try again.")
  }
}
