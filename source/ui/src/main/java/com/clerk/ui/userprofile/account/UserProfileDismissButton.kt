package com.clerk.ui.userprofile.account

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.theme.ClerkMaterialTheme

internal fun userProfileDismissTrailingContent(
  isDismissible: Boolean,
  onDismiss: () -> Unit,
): (@Composable () -> Unit)? {
  if (!shouldShowUserProfileCloseButton(isDismissible)) return null

  return { UserProfileCloseButton(onDismiss) }
}

@Composable
private fun UserProfileCloseButton(onDismiss: () -> Unit) {
  IconButton(onClick = onDismiss) {
    Icon(
      modifier = Modifier.size(dp24),
      painter = painterResource(R.drawable.ic_cross),
      contentDescription = stringResource(R.string.close),
      tint = ClerkMaterialTheme.colors.foreground,
    )
  }
}

internal fun shouldShowUserProfileCloseButton(isDismissible: Boolean): Boolean = isDismissible
