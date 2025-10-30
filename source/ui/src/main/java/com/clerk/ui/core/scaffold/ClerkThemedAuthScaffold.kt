package com.clerk.ui.core.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import com.clerk.ui.R
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.dimens.dp18
import com.clerk.ui.core.dimens.dp32
import com.clerk.ui.core.error.ClerkErrorSnackbar
import com.clerk.ui.core.footer.SecuredByClerkView
import com.clerk.ui.core.header.HeaderTextView
import com.clerk.ui.core.header.HeaderType
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun ClerkThemedAuthScaffold(
  title: String,
  modifier: Modifier = Modifier,
  snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
  onBackPressed: () -> Unit = {},
  subtitle: String? = null,
  hasLogo: Boolean = true,
  hasBackButton: Boolean = true,
  identifier: String? = null,
  onClickIdentifier: () -> Unit = {},
  spacingAfterIdentifier: Dp = dp32,
  content: @Composable () -> Unit,
) {
  ClerkMaterialTheme {
    Scaffold(
      modifier = Modifier.then(modifier),
      snackbarHost = { ClerkErrorSnackbar(snackbarHostState) },
      topBar = {
        ClerkTopAppBar(
          backgroundColor = ClerkMaterialTheme.colors.background,
          onBackPressed = onBackPressed,
          hasLogo = hasLogo,
          hasBackButton = hasBackButton,
        )
      },
    ) { innerPadding ->
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .padding(innerPadding)
            .padding(horizontal = dp18)
            .background(ClerkMaterialTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
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
        Spacer(modifier = Modifier.height(spacingAfterIdentifier))
        content()
        Spacers.Vertical.Spacer32()
        SecuredByClerkView()
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
