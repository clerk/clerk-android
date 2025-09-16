package com.clerk.ui.core.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.R
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.common.dimens.dp18
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun ClerkThemedAuthScaffold(
  onBackPressed: () -> Unit,
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  snackbarHost: @Composable () -> Unit = {},
  hasLogo: Boolean = true,
  identifier: String? = null,
  onClickIdentifier: () -> Unit = {},
  content: @Composable () -> Unit,
) {
  ClerkMaterialTheme {
    Scaffold(modifier = Modifier.then(modifier), snackbarHost = snackbarHost) { innerPadding ->
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .padding(innerPadding)
            .padding(horizontal = dp18)
            .background(ClerkMaterialTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        ClerkTopAppBar(onBackPressed = onBackPressed, hasLogo = hasLogo)
        Spacers.Vertical.Spacer8()
        HeaderTextView(text = title, type = HeaderType.Title)
        subtitle?.let {
          Spacers.Vertical.Spacer8()
          HeaderTextView(text = it, type = HeaderType.Subtitle)
        }
        identifier?.let {
          Spacers.Vertical.Spacer8()
          ClerkButton(
            text = it,
            onClick = onClickIdentifier,
            modifier = Modifier.wrapContentHeight(),
            configuration =
              ClerkButtonConfiguration(style = ClerkButtonConfiguration.ButtonStyle.Secondary),
            icons =
              ClerkButtonDefaults.icons(
                trailingIcon = R.drawable.ic_edit,
                trailingIconColor = ClerkMaterialTheme.colors.mutedForeground,
              ),
          )
        }
        Spacers.Vertical.Spacer32()
        content()
        Spacers.Vertical.Spacer32()
        SecuredByClerk()
      }
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewClerkThemedAuthScaffold() {
  ClerkThemedAuthScaffold(
    onBackPressed = {},
    title = "Welcome back",
    subtitle = "Sign in to continue",
  ) {
    ClerkTextField(value = "", onValueChange = {})
  }
}
