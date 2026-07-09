package com.clerk.ui.core.footer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun SecuredByClerkView(
  modifier: Modifier = Modifier,
  hideWhenDevelopmentModeWarning: Boolean = true,
) {
  val isInitialized by Clerk.isInitialized.collectAsStateWithLifecycle()
  val shouldHideForDevelopmentMode =
    hideWhenDevelopmentModeWarning && isInitialized && Clerk.shouldShowDevelopmentModeWarning

  if (Clerk.isBranded && !shouldHideForDevelopmentMode) {
    ClerkMaterialTheme {
      Row(
        modifier = Modifier.then(modifier),
        horizontalArrangement = Arrangement.spacedBy(dp8, alignment = Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.secured_by),
          style = ClerkMaterialTheme.typography.bodyMedium,
          color = ClerkMaterialTheme.colors.mutedForeground,
        )
        Image(
          painter = painterResource(R.drawable.ic_clerk_logo),
          contentDescription = "Clerk",
          colorFilter = ColorFilter.tint(ClerkMaterialTheme.colors.mutedForeground),
        )
      }
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewSecuredByClerk() {
  ClerkMaterialTheme {
    Box(modifier = Modifier.background(color = ClerkMaterialTheme.colors.background)) {
      SecuredByClerkView()
    }
  }
}
