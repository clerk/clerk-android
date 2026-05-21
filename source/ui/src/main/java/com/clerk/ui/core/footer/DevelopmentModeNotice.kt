package com.clerk.ui.core.footer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.extensions.withSemiBoldWeight
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun ClerkFooterView(
  modifier: Modifier = Modifier,
  showDevModeWarning: Boolean = Clerk.shouldShowDevModeWarning,
  isBranded: Boolean = Clerk.isBranded,
) {
  if (!showDevModeWarning && !isBranded) return

  ClerkMaterialTheme {
    Column(
      modifier = Modifier.then(modifier),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(dp8),
    ) {
      DevelopmentModeNotice(showDevModeWarning = showDevModeWarning)
      SecuredByClerkView(isBranded = isBranded)
    }
  }
}

@Composable
internal fun DevelopmentModeNotice(
  modifier: Modifier = Modifier,
  showDevModeWarning: Boolean = Clerk.shouldShowDevModeWarning,
) {
  if (!showDevModeWarning) return

  Text(
    modifier = Modifier.then(modifier),
    text = stringResource(R.string.development_mode),
    style = ClerkMaterialTheme.typography.bodyMedium.withSemiBoldWeight(),
    color = ClerkMaterialTheme.colors.warning,
  )
}

@PreviewLightDark
@Composable
private fun PreviewDevelopmentModeNotice() {
  ClerkMaterialTheme { DevelopmentModeNotice(showDevModeWarning = true) }
}
