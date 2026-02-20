package com.clerk.ui.core.appbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.core.avatar.OrganizationAvatar
import com.clerk.ui.core.dimens.dp48
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun ClerkTopAppBar(
  onBackPressed: () -> Unit,
  modifier: Modifier = Modifier,
  hasLogo: Boolean = true,
  hasBackButton: Boolean = true,
  title: String? = null,
  backgroundColor: Color? = null, // sensible default
  clerkTheme: ClerkTheme? = null,
  trailingContent: (@Composable () -> Unit)? = null,
) {
  ClerkMaterialTheme(clerkTheme = clerkTheme) {
    val resolvedBackgroundColor = backgroundColor ?: ClerkMaterialTheme.colors.muted
    Box(modifier = Modifier.fillMaxWidth().then(modifier).background(resolvedBackgroundColor)) {
      Spacer(
        Modifier.fillMaxWidth()
          .background(resolvedBackgroundColor)
          .windowInsetsTopHeight(WindowInsets.statusBars)
      )

      Row(
        modifier =
          Modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(vertical = dp8),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (trailingContent != null) {
          TopBarWithTrailingContent(
            hasBackButton = hasBackButton,
            onBackPressed = onBackPressed,
            title = title,
            trailingContent = trailingContent,
          )
        } else {
          TopBarWithLogo(
            hasBackButton = hasBackButton,
            onBackPressed = onBackPressed,
            title = title,
            hasLogo = hasLogo,
            clerkTheme = clerkTheme,
          )
        }
      }
    }
  }
}

@Composable
private fun RowScope.TopBarWithTrailingContent(
  hasBackButton: Boolean,
  onBackPressed: () -> Unit,
  title: String?,
  trailingContent: @Composable () -> Unit,
) {
  Box(modifier = Modifier.size(dp48), contentAlignment = Alignment.Center) {
    BackButton(hasBackButton = hasBackButton, onBackPressed = onBackPressed)
  }
  Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { TopBarTitle(title) }
  Box(modifier = Modifier.size(dp48), contentAlignment = Alignment.Center) { trailingContent() }
}

@Composable
private fun RowScope.TopBarWithLogo(
  hasBackButton: Boolean,
  onBackPressed: () -> Unit,
  title: String?,
  hasLogo: Boolean,
  clerkTheme: ClerkTheme?,
) {
  BackButton(hasBackButton = hasBackButton, onBackPressed = onBackPressed)
  Spacer(Modifier.weight(1f))
  TopBarTitle(title)
  if (hasLogo) OrganizationAvatar(clerkTheme = clerkTheme)
  Spacer(Modifier.weight(1f))
  if (hasBackButton) {
    IconButton(onClick = {}) {}
  }
}

@Composable
private fun BackButton(hasBackButton: Boolean, onBackPressed: () -> Unit) {
  if (hasBackButton) {
    IconButton(onClick = onBackPressed) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = stringResource(R.string.back),
        tint = ClerkMaterialTheme.colors.foreground,
      )
    }
  }
}

@Composable
private fun TopBarTitle(title: String?) {
  title?.let {
    Text(
      text = it,
      style = ClerkMaterialTheme.typography.titleLarge.withMediumWeight(),
      color = ClerkMaterialTheme.colors.foreground,
    )
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
