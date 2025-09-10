package com.clerk.ui.core.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.R
import com.clerk.ui.core.common.dimens.dp8
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun SecuredByClerk(modifier: Modifier = Modifier) {
  ClerkMaterialTheme {
    Row(
      modifier = Modifier.then(modifier),
      horizontalArrangement = Arrangement.spacedBy(dp8, alignment = Alignment.CenterHorizontally),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Secured by",
        style = ClerkMaterialTheme.typography.bodyMedium,
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
      Image(painter = painterResource(R.drawable.ic_clerk_logo), contentDescription = "Clerk")
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewSecuredByClerk() {
  ClerkMaterialTheme {
    Box(modifier = Modifier.background(color = ClerkMaterialTheme.colors.background)) {
      SecuredByClerk()
    }
  }
}
