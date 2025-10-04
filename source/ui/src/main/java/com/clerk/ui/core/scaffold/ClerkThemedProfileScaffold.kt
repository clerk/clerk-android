package com.clerk.ui.core.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.dimens.dp18
import com.clerk.ui.core.error.ClerkErrorSnackbar
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun ClerkThemedProfileScaffold(
  modifier: Modifier = Modifier,
  errorMessage: String? = null,
  hasLogo: Boolean = false,
  hasBackButton: Boolean = true,
  title: String? = null,
  onBackPressed: () -> Unit = {},
  content: @Composable () -> Unit,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(errorMessage) {
    if (errorMessage != null) {
      snackbarHostState.showSnackbar(errorMessage)
    }
  }

  ClerkMaterialTheme {
    Scaffold(
      modifier = Modifier.then(modifier),
      snackbarHost = { ClerkErrorSnackbar(snackbarHostState) },
    ) { innerPadding ->
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .padding(innerPadding)
            .padding(horizontal = dp18)
            .background(ClerkMaterialTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        ClerkTopAppBar(
          hasLogo = hasLogo,
          hasBackButton = hasBackButton,
          title = title,
          onBackPressed = onBackPressed,
        )
        Spacers.Vertical.Spacer24()
        content()
      }
    }
  }
}
