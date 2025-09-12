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
import com.clerk.ui.R
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfig
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.common.dimens.dp18
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun ClerkScaffold(
  onBackPressed: () -> Unit,
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier,
  hasLogo: Boolean = true,
  identifier: String? = null,
  onClickIdentifier: () -> Unit = {},
  content: @Composable () -> Unit,
) {
  Scaffold(modifier = Modifier.then(modifier)) { innerPadding ->
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
      Spacers.Vertical.Spacer8()
      HeaderTextView(text = subtitle, type = HeaderType.Subtitle)
      identifier?.let {
        Spacers.Vertical.Spacer8()
        ClerkButton(
          text = it,
          onClick = onClickIdentifier,
          modifier = Modifier.wrapContentHeight(),
          configuration = ClerkButtonConfig(style = ClerkButtonConfig.ButtonStyle.Secondary),
          icons =
            ClerkButtonDefaults.icons(
              trailingIcon = R.drawable.ic_edit,
              trailingIconColor = ClerkMaterialTheme.colors.mutedForeground,
            ),
        )
      }
      Spacers.Vertical.Spacer32()
      content()
    }
  }
}
