package com.clerk.ui.core.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.R
import com.clerk.ui.core.avatar.OrganizationAvatar
import com.clerk.ui.core.common.dimens.dp24
import com.clerk.ui.core.common.dimens.dp8
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun ClerkTopAppBar(
  modifier: Modifier = Modifier,
  hasLogo: Boolean = true,
  hasBackButton: Boolean = true,
  onBackPressed: () -> Unit,
) {
  ClerkMaterialTheme {
    Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = dp8).then(modifier),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (hasBackButton) {
        Icon(
          modifier = Modifier.clickable { onBackPressed() },
          imageVector = Icons.AutoMirrored.Default.ArrowBack,
          contentDescription = stringResource(R.string.back),
          tint = ClerkMaterialTheme.colors.foreground,
        )
      }

      Spacer(modifier = Modifier.weight(1f))
      if (hasLogo) {
        OrganizationAvatar()
      }
      Spacer(modifier = Modifier.weight(1f))
      if (hasBackButton) {
        Box(modifier = Modifier.size(dp24))
      }
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

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    Box(modifier = Modifier.background(color = ClerkMaterialTheme.colors.background)) {
      ClerkTopAppBar(onBackPressed = {}, hasBackButton = false)
    }
  }
}
