package com.clerk.ui.userprofile.custom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun UserProfileCustomDestination(
  onBackPressed: () -> Unit,
  modifier: Modifier = Modifier,
  title: String? = null,
  content: @Composable () -> Unit,
) {
  ClerkMaterialTheme {
    Scaffold(
      modifier = modifier.fillMaxSize(),
      containerColor = ClerkMaterialTheme.colors.background,
      topBar = {
        ClerkTopAppBar(
          onBackPressed = onBackPressed,
          hasLogo = false,
          title = title,
          backgroundColor = ClerkMaterialTheme.colors.background,
        )
      },
    ) { innerPadding ->
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) { content() }
    }
  }
}
