package com.clerk.ui.core.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.R
import com.clerk.ui.core.avatar.OrganizationAvatar
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun ClerkTopAppBar(
  onBackPressed: () -> Unit,
  modifier: Modifier = Modifier,
  hasLogo: Boolean = true,
  hasBackButton: Boolean = true,
  title: String? = null,
  backgroundColor: Color = ClerkMaterialTheme.colors.muted, // sensible default
) {
  ClerkMaterialTheme {
    Box(modifier = Modifier.fillMaxWidth().then(modifier).background(backgroundColor)) {
      // 1) Paint behind the status bar (exact height)
      Spacer(
        Modifier.fillMaxWidth()
          .background(backgroundColor)
          .windowInsetsTopHeight(WindowInsets.statusBars)
      )

      // 2) Pad content down by the status-bar inset
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(vertical = dp8),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (hasBackButton) {
          IconButton(onClick = onBackPressed) {
            Icon(
              imageVector = Icons.AutoMirrored.Default.ArrowBack,
              contentDescription = stringResource(R.string.back),
              tint = ClerkMaterialTheme.colors.foreground,
            )
          }
        }

        Spacer(Modifier.weight(1f))
        title?.let {
          Text(
            text = it,
            style = ClerkMaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
            color = ClerkMaterialTheme.colors.foreground,
          )
        }
        if (hasLogo) OrganizationAvatar()
        Spacer(Modifier.weight(1f))

        // keep layout symmetric
        if (hasBackButton) {
          IconButton(onClick = {}) {}
        }
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

@PreviewLightDark
@Composable
private fun PreviewTitle() {
  ClerkMaterialTheme {
    Box(modifier = Modifier.background(color = ClerkMaterialTheme.colors.background)) {
      ClerkTopAppBar(
        onBackPressed = {},
        hasBackButton = true,
        backgroundColor = ClerkMaterialTheme.colors.danger,
        title = "Add email address",
        hasLogo = false,
      )
    }
  }
}
