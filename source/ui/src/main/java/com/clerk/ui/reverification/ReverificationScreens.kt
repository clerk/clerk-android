@file:Suppress("TooManyFunctions")

package com.clerk.ui.reverification

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.clerk.api.auth.types.Strategy
import com.clerk.api.network.model.factor.Factor
import com.clerk.ui.ClerkTestTags
import com.clerk.ui.R
import com.clerk.ui.auth.VerificationUiState
import com.clerk.ui.auth.verificationState
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp72
import com.clerk.ui.core.input.ClerkCodeInputField
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.scaffold.ClerkThemedAuthScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.signin.code.SignInFactorCodeUiHelper
import com.clerk.ui.signin.password.forgot.AlternativeFactorList
import com.clerk.ui.signin.password.reset.SignOutOfOtherDevicesRow
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.util.TextIconHelper
import com.clerk.ui.util.formattedAsPhoneNumberIfPossible
import kotlinx.collections.immutable.ImmutableList

@Composable
private fun ReverificationScaffold(
  chrome: ReverificationChrome,
  title: String,
  subtitle: String? = null,
  identifier: String? = null,
  content: @Composable () -> Unit,
) {
  ClerkThemedAuthScaffold(
    title = title,
    subtitle = subtitle,
    identifier = identifier,
    identifierEditable = false,
    snackbarHostState = chrome.snackbarHostState,
    hasBackButton = chrome.canGoBack,
    onBackPressed = chrome.onBack,
    showSignedInUserButton = false,
    trailingContent = { CloseButton(chrome.onClose) },
    content = content,
  )
}

@Composable
private fun CloseButton(onClose: () -> Unit) {
  IconButton(
    onClick = onClose,
    modifier = Modifier.testTag(ClerkTestTags.Reverification.closeButton),
  ) {
    Icon(
      modifier = Modifier.size(dp24),
      painter = painterResource(R.drawable.ic_cross),
      contentDescription = stringResource(R.string.close),
      tint = ClerkMaterialTheme.colors.foreground,
    )
  }
}

@Composable
private fun ContinueButton(
  onClick: () -> Unit,
  isLoading: Boolean,
  isEnabled: Boolean = true,
  text: String = stringResource(R.string.continue_text),
  testTag: String = ClerkTestTags.Reverification.continueButton,
) {
  ClerkButton(
    modifier = Modifier.fillMaxWidth().testTag(testTag),
    text = text,
    onClick = onClick,
    isLoading = isLoading,
    isEnabled = isEnabled,
    icons =
      ClerkButtonDefaults.icons(
        trailingIcon = R.drawable.ic_triangle_right,
        trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
      ),
  )
}

@Composable
private fun UseAnotherMethodButton(onClick: (() -> Unit)?) {
  if (onClick == null) return
  Spacers.Vertical.Spacer24()
  ClerkTextButton(
    modifier = Modifier.testTag(ClerkTestTags.Reverification.useAnotherMethodButton),
    text = stringResource(R.string.use_another_method),
    onClick = onClick,
  )
}

@Composable
internal fun ReverificationPasswordScreen(
  chrome: ReverificationChrome,
  password: String,
  state: ReverificationScreenState,
  onPasswordChange: (String) -> Unit,
  onSubmit: () -> Unit,
  onResetPassword: (() -> Unit)?,
  onUseAnotherMethod: (() -> Unit)?,
) {
  ReverificationScaffold(
    chrome = chrome,
    title = stringResource(R.string.reverification_title),
    subtitle = stringResource(R.string.reverification_password_subtitle),
  ) {
    ClerkTextField(
      modifier = Modifier.testTag(ClerkTestTags.Reverification.password),
      value = password,
      onValueChange = onPasswordChange,
      label = stringResource(R.string.enter_your_password),
      isError = state.fieldError != null,
      supportingText = state.fieldError,
      visualTransformation = PasswordVisualTransformation(),
      inputContentType = ContentType.Password,
      keyboardOptions =
        KeyboardOptions(
          autoCorrectEnabled = false,
          keyboardType = KeyboardType.Password,
          imeAction = ImeAction.Go,
        ),
      keyboardActions = KeyboardActions(onGo = { if (password.isNotEmpty()) onSubmit() }),
    )
    Spacers.Vertical.Spacer24()
    if (onResetPassword != null) {
      ContinueButton(
        onClick = onResetPassword,
        isLoading = false,
        text = stringResource(R.string.reset_password),
        testTag = ClerkTestTags.Reverification.resetPasswordButton,
      )
    } else {
      ContinueButton(
        onClick = onSubmit,
        isLoading = state.isLoading,
        isEnabled = password.isNotEmpty(),
      )
    }
    UseAnotherMethodButton(onUseAnotherMethod)
  }
}

@Composable
internal fun ReverificationCodeScreen(
  chrome: ReverificationChrome,
  factor: Factor,
  codeState: VerificationUiState,
  onCodeChange: (String) -> Unit,
  onResend: () -> Unit,
  onUseAnotherMethod: (() -> Unit)?,
) {
  ReverificationScaffold(
    chrome = chrome,
    title = stringResource(R.string.reverification_title),
    subtitle = codeSubtitle(factor),
    identifier = factor.safeIdentifier?.formattedAsPhoneNumberIfPossible,
  ) {
    ClerkCodeInputField(
      modifier = Modifier.testTag(ClerkTestTags.Reverification.code),
      verificationState = codeState.verificationState(),
      onTextChange = onCodeChange,
      showResend = SignInFactorCodeUiHelper.showResend(factor, codeState.verificationState()),
      onClickResend = onResend,
    )
    UseAnotherMethodButton(onUseAnotherMethod)
  }
}

@Composable
private fun codeSubtitle(factor: Factor): String =
  when (factor.strategyType) {
    Strategy.EmailCode,
    Strategy.ResetPasswordEmailCode -> stringResource(R.string.reverification_email_code_subtitle)
    Strategy.PhoneCode,
    Strategy.ResetPasswordPhoneCode -> stringResource(R.string.reverification_phone_code_subtitle)
    else -> stringResource(R.string.reverification_totp_subtitle)
  }

@Composable
internal fun ReverificationPasskeyScreen(
  chrome: ReverificationChrome,
  isLoading: Boolean,
  onContinue: () -> Unit,
  onUseAnotherMethod: (() -> Unit)?,
) {
  ReverificationScaffold(
    chrome = chrome,
    title = stringResource(R.string.use_your_passkey),
    subtitle = stringResource(R.string.using_your_passkey),
  ) {
    Icon(
      modifier = Modifier.size(dp72),
      painter = painterResource(R.drawable.ic_android_passkey),
      contentDescription = stringResource(R.string.passkey_icon),
      tint = ClerkMaterialTheme.colors.primary,
    )
    Spacers.Vertical.Spacer32()
    ContinueButton(onClick = onContinue, isLoading = isLoading)
    UseAnotherMethodButton(onUseAnotherMethod)
  }
}

@Composable
internal fun ReverificationBiometricScreen(
  chrome: ReverificationChrome,
  isLoading: Boolean,
  onContinue: () -> Unit,
  onUseAnotherMethod: (() -> Unit)?,
) {
  ReverificationScaffold(
    chrome = chrome,
    title = stringResource(R.string.reverification_title),
    subtitle = stringResource(R.string.reverification_biometric_subtitle),
  ) {
    Icon(
      modifier = Modifier.size(dp72),
      painter = painterResource(R.drawable.ic_fingerprint),
      contentDescription = null,
      tint = ClerkMaterialTheme.colors.primary,
    )
    Spacers.Vertical.Spacer32()
    ContinueButton(
      onClick = onContinue,
      isLoading = isLoading,
      text = stringResource(R.string.continue_with_biometrics),
      testTag = ClerkTestTags.Reverification.biometricButton,
    )
    UseAnotherMethodButton(onUseAnotherMethod)
  }
}

@Composable
internal fun ReverificationBackupCodeScreen(
  chrome: ReverificationChrome,
  code: String,
  state: ReverificationScreenState,
  onCodeChange: (String) -> Unit,
  onSubmit: () -> Unit,
  onUseAnotherMethod: (() -> Unit)?,
) {
  ReverificationScaffold(
    chrome = chrome,
    title = stringResource(R.string.enter_a_backup_code),
    subtitle = stringResource(R.string.your_backup_code),
  ) {
    ClerkTextField(
      modifier = Modifier.fillMaxWidth().testTag(ClerkTestTags.Reverification.backupCode),
      value = code,
      onValueChange = onCodeChange,
      label = stringResource(R.string.backup_code),
      isError = state.fieldError != null,
      supportingText = state.fieldError,
      keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Go),
      keyboardActions = KeyboardActions(onGo = { if (code.isNotBlank()) onSubmit() }),
    )
    Spacers.Vertical.Spacer24()
    ContinueButton(onClick = onSubmit, isLoading = state.isLoading, isEnabled = code.isNotBlank())
    UseAnotherMethodButton(onUseAnotherMethod)
  }
}

@Composable
internal fun ReverificationAlternativeMethodsScreen(
  chrome: ReverificationChrome,
  factors: ImmutableList<Factor>,
  onSelect: (Factor) -> Unit,
) {
  val context = LocalContext.current
  ReverificationScaffold(
    chrome = chrome,
    title = stringResource(R.string.use_another_method),
    subtitle = stringResource(R.string.reverification_alternative_methods_subtitle),
  ) {
    AlternativeFactorList(
      alternativeFactors = factors,
      textIconHelper = ReverificationTextIconHelper,
      context = context,
      onClickFactor = onSelect,
      testTag = ClerkTestTags.Reverification::alternativeMethodButton,
    )
  }
}

private object ReverificationTextIconHelper : TextIconHelper() {
  override fun actionText(factor: Factor, context: Context): String? =
    when (factor.strategyType) {
      Strategy.Password -> context.getString(R.string.use_your_password)
      Strategy.Passkey -> context.getString(R.string.use_your_passkey)
      Strategy.TrustedDevice -> context.getString(R.string.continue_with_biometrics)
      else -> super.actionText(factor, context)
    }

  override fun iconResource(factor: Factor): Int? =
    when (factor.strategyType) {
      Strategy.TrustedDevice -> R.drawable.ic_fingerprint
      else -> super.iconResource(factor)
    }
}

@Composable
internal fun ReverificationNewPasswordScreen(
  chrome: ReverificationChrome,
  reason: PasswordResetReason,
  newPassword: String,
  confirmNewPassword: String,
  signOutOfOtherSessions: Boolean,
  error: String?,
  onNewPasswordChange: (String) -> Unit,
  onConfirmNewPasswordChange: (String) -> Unit,
  onSignOutOfOtherSessionsChange: (Boolean) -> Unit,
  onContinue: () -> Unit,
) {
  val confirmFocusRequester = remember { FocusRequester() }
  val passwordsMatch = confirmNewPassword.isEmpty() || newPassword == confirmNewPassword
  val canContinue = newPassword.isNotBlank() && newPassword == confirmNewPassword
  ReverificationScaffold(chrome = chrome, title = stringResource(R.string.set_new_password)) {
    Text(
      text =
        when (reason) {
          PasswordResetReason.PWNED -> stringResource(R.string.reverification_password_pwned)
          PasswordResetReason.COMPROMISED ->
            stringResource(R.string.reverification_password_compromised)
        },
      style = ClerkMaterialTheme.typography.bodyMedium,
      color = ClerkMaterialTheme.colors.warning,
      textAlign = TextAlign.Center,
    )
    Spacers.Vertical.Spacer24()
    ClerkTextField(
      modifier = Modifier.testTag(ClerkTestTags.Reverification.newPassword),
      value = newPassword,
      onValueChange = onNewPasswordChange,
      label = stringResource(R.string.new_password),
      isError = error != null,
      supportingText = error,
      visualTransformation = PasswordVisualTransformation(),
      inputContentType = ContentType.NewPassword,
      keyboardOptions =
        KeyboardOptions(
          autoCorrectEnabled = false,
          keyboardType = KeyboardType.Password,
          imeAction = ImeAction.Next,
        ),
      keyboardActions = KeyboardActions(onNext = { confirmFocusRequester.requestFocus() }),
    )
    Spacers.Vertical.Spacer24()
    ClerkTextField(
      modifier =
        Modifier.focusRequester(confirmFocusRequester)
          .testTag(ClerkTestTags.Reverification.confirmPassword),
      value = confirmNewPassword,
      onValueChange = onConfirmNewPasswordChange,
      label = stringResource(R.string.confirm_password),
      isError = !passwordsMatch,
      supportingText = if (passwordsMatch) null else stringResource(R.string.passwords_dont_match),
      visualTransformation = PasswordVisualTransformation(),
      inputContentType = ContentType.NewPassword,
      keyboardOptions =
        KeyboardOptions(
          autoCorrectEnabled = false,
          keyboardType = KeyboardType.Password,
          imeAction = ImeAction.Done,
        ),
      keyboardActions = KeyboardActions(onDone = { if (canContinue) onContinue() }),
    )
    Spacers.Vertical.Spacer24()
    SignOutOfOtherDevicesRow(signOutOfOtherSessions, onSignOutOfOtherSessionsChange)
    Spacers.Vertical.Spacer24()
    ContinueButton(onClick = onContinue, isLoading = false, isEnabled = canContinue)
  }
}

@Composable
internal fun ReverificationGetHelpScreen(chrome: ReverificationChrome, onEmailSupport: () -> Unit) {
  ReverificationScaffold(
    chrome = chrome,
    title = stringResource(R.string.get_help),
    subtitle = stringResource(R.string.reverification_get_help_subtitle),
  ) {
    ClerkButton(
      modifier = Modifier.fillMaxWidth().testTag(ClerkTestTags.Reverification.emailSupportButton),
      text = stringResource(R.string.email_support),
      onClick = onEmailSupport,
    )
  }
}

@Composable
internal fun ReverificationFailedScreen(chrome: ReverificationChrome, message: String) {
  ReverificationScaffold(
    chrome = chrome,
    title = stringResource(R.string.reverification_title),
    subtitle = message,
  ) {
    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.close),
      onClick = chrome.onClose,
      configuration =
        ClerkButtonDefaults.configuration(style = ClerkButtonConfiguration.ButtonStyle.Secondary),
    )
  }
}
