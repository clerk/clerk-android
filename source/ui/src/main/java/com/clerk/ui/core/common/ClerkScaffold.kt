package com.clerk.ui.core.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.common.dimens.dp18
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun ClerkScaffold(
  onBackPressed: () -> Unit,
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier,
  hasLogo: Boolean = true,
  content: @Composable () -> Unit,
) {
  Scaffold(modifier = Modifier.then(modifier)) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(innerPadding)
          .padding(horizontal = dp18)
          .background(ClerkMaterialTheme.colors.background)
    ) {
      ClerkTopAppBar(onBackPressed = onBackPressed, hasLogo = hasLogo)
      Spacers.Vertical.Spacer8()
      HeaderTextView(text = title, type = HeaderType.Title)
      Spacers.Vertical.Spacer8()
      HeaderTextView(text = subtitle, type = HeaderType.Subtitle)
      Spacers.Vertical.Spacer32()
      content()
    }
  }
}
