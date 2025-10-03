package com.clerk.ui.userprofile.addemail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
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
import com.clerk.ui.core.common.Spacers
import com.clerk.ui.core.common.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun UserProfileAddEmailView(modifier: Modifier = Modifier) {
  var email by remember { mutableStateOf("") }
  ClerkThemedProfileScaffold(
    modifier = modifier,
    title = stringResource(R.string.add_email_address),
  ) {
    Text(
      text = stringResource(R.string.you_ll_need_to_verify_this_email_address),
      style = ClerkMaterialTheme.typography.bodyMedium,
    )
    Spacers.Vertical.Spacer24()
    ClerkTextField(
      value = email,
      onValueChange = { email = it },
      label = stringResource(R.string.enter_your_email),
    )
    Spacers.Vertical.Spacer24()
    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.continue_text),
      onClick = {},
      icons = ClerkButtonDefaults.icons(trailingIcon = R.drawable.ic_triangle_right),
    )
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    Box(modifier = Modifier.background(color = ClerkMaterialTheme.colors.background)) {
      UserProfileAddEmailView()
    }
  }
}
