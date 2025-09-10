package com.clerk.ui.core.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.core.avatar.OrganizationAvatar
import com.clerk.ui.core.common.dimens.dp40
import com.clerk.ui.core.common.dimens.dp8
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun ClerkTopAppBar(
  modifier: Modifier = Modifier,
  hasLogo: Boolean = true,
  onBackPressed: () -> Unit,
) {
  ClerkMaterialTheme {
    Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = dp8).then(modifier),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBackPressed) {
        Icon(
          imageVector = Icons.AutoMirrored.Default.ArrowBack,
          contentDescription = "Back",
          tint = ClerkMaterialTheme.colors.foreground,
        )
      }
      Spacer(modifier = Modifier.weight(1f))
      if (hasLogo) {
        OrganizationAvatar()
      }
      Spacer(modifier = Modifier.weight(1f))
      // Add invisible spacer with same size as IconButton to balance spacing
      Box(modifier = Modifier.size(dp40))
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewClerkTopAppBarr() {
  ClerkMaterialTheme {
    Box(modifier = Modifier.background(color = ClerkMaterialTheme.colors.background)) {
      ClerkTopAppBar(onBackPressed = {})
    }
  }
}
