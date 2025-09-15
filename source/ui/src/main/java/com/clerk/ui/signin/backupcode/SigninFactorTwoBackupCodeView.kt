package com.clerk.ui.signin.backupcode

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.common.ClerkThemedAuthScaffold
import com.clerk.ui.core.common.Spacers
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun SignInFactorTwoBackupCodeView(modifier: Modifier = Modifier, onBackPressed: () -> Unit) {
  SignInFactorTwoBackupCodeViewImpl(onBackPressed, modifier)
}

@Composable
private fun SignInFactorTwoBackupCodeViewImpl(
  onBackPressed: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var backupCode by remember { mutableStateOf("") }
  ClerkThemedAuthScaffold(
    onBackPressed = onBackPressed,
    modifier = modifier,
    hasLogo = false,
    title = stringResource(R.string.enter_a_backup_code),
    subtitle = stringResource(R.string.your_backup_code),
  ) {
    ClerkTextField(
      modifier = Modifier.fillMaxWidth(),
      value = backupCode,
      onValueChange = { backupCode = it },
      label = stringResource(R.string.backup_code),
    )
    Spacers.Vertical.Spacer24()
    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.continue_text),
      onClick = {},
      icons = ClerkButtonDefaults.icons(trailingIcon = R.drawable.ic_triangle_right),
    )
    Spacers.Vertical.Spacer24()
    ClerkTextButton(text = stringResource(R.string.use_another_method)) {}
  }
}

@PreviewLightDark
@Composable
private fun PreviewSignInFactorTwoBackupCodeView() {
  ClerkMaterialTheme { SignInFactorTwoBackupCodeView(onBackPressed = {}) }
}
