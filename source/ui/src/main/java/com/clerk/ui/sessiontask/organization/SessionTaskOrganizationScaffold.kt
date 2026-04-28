package com.clerk.ui.sessiontask.organization

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.error.ClerkErrorSnackbar
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userbutton.UserButton

@Composable
internal fun SessionTaskOrganizationScaffold(
  errorMessage: String?,
  onErrorShown: () -> Unit,
  modifier: Modifier = Modifier,
  hasBackButton: Boolean = false,
  onBackPressed: () -> Unit = {},
  content: @Composable (PaddingValues) -> Unit,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(errorMessage) {
    if (errorMessage != null) {
      snackbarHostState.showSnackbar(errorMessage)
      onErrorShown()
    }
  }

  ClerkMaterialTheme {
    Scaffold(
      modifier = modifier.background(ClerkMaterialTheme.colors.background),
      snackbarHost = { ClerkErrorSnackbar(snackbarHostState) },
      topBar = {
        ClerkTopAppBar(
          backgroundColor = ClerkMaterialTheme.colors.background,
          hasLogo = false,
          hasBackButton = hasBackButton,
          onBackPressed = onBackPressed,
          trailingContent = { UserButton(routeToAuthWhenForcedMfa = false) },
        )
      },
      content = content,
    )
  }
}
