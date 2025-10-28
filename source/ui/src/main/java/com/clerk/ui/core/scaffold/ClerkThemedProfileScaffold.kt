package com.clerk.ui.core.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.dimens.dp18
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.error.ClerkErrorSnackbar
import com.clerk.ui.core.footer.SecuredByClerkView
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun ClerkThemedProfileScaffold(
  content: @Composable ColumnScope.() -> Unit,
  modifier: Modifier = Modifier,
  errorMessage: String? = null,
  hasLogo: Boolean = false,
  hasBackButton: Boolean = true,
  title: String? = null,
  onBackPressed: () -> Unit = {},
  horizontalPadding: Dp = dp18,
  backgroundColor: Color = ClerkMaterialTheme.colors.background,
  bottomContent: (@Composable () -> Unit)? = null,
  contentTopPadding: Dp = dp24,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(errorMessage) {
    if (errorMessage != null) snackbarHostState.showSnackbar(errorMessage)
  }

  ClerkMaterialTheme {
    Scaffold(
      modifier = Modifier.then(modifier),
      snackbarHost = { ClerkErrorSnackbar(snackbarHostState) },
    ) { innerPadding ->
      Column(
        modifier =
          Modifier.fillMaxWidth().fillMaxSize().background(backgroundColor).padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        ClerkTopAppBar(
          hasLogo = hasLogo,
          hasBackButton = hasBackButton,
          title = title,
          onBackPressed = onBackPressed,
        )
        Spacer(modifier = Modifier.padding(top = contentTopPadding))
        Column(modifier = Modifier.padding(horizontal = horizontalPadding)) { content() }

        Spacer(modifier = Modifier.weight(1f))

        bottomContent?.invoke()

        Spacers.Vertical.Spacer24()

        SecuredByClerkView()

        Spacers.Vertical.Spacer24()
      }
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    ClerkThemedProfileScaffold(
      title = "Security",
      backgroundColor = ClerkMaterialTheme.colors.muted,
      content = {
        /* Content goes here */
      },
    )
  }
}
