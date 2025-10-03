package com.clerk.ui.core.common.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.common.Spacers
import com.clerk.ui.core.common.dimens.dp18
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun ClerkThemedProfileScaffold(
  modifier: Modifier = Modifier,
  hasLogo: Boolean = false,
  hasBackButton: Boolean = true,
  title: String? = null,
  onBackPressed: () -> Unit = {},
  content: @Composable () -> Unit,
) {
  ClerkMaterialTheme {
    Scaffold(modifier = Modifier.then(modifier)) { innerPadding ->
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
