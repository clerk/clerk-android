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
import com.clerk.ui.userprofile.UserProfileDismissButtonStyle

internal fun userProfileDismissTrailingContent(
  isDismissible: Boolean,
  dismissButtonStyle: UserProfileDismissButtonStyle,
  onDismiss: () -> Unit,
): (@Composable () -> Unit)? {
  if (!shouldShowUserProfileCloseButton(isDismissible, dismissButtonStyle)) return null

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

internal fun shouldShowUserProfileBackButton(
  isDismissible: Boolean,
  dismissButtonStyle: UserProfileDismissButtonStyle,
): Boolean = isDismissible && dismissButtonStyle == UserProfileDismissButtonStyle.Back

internal fun shouldShowUserProfileCloseButton(
  isDismissible: Boolean,
  dismissButtonStyle: UserProfileDismissButtonStyle,
): Boolean = isDismissible && dismissButtonStyle == UserProfileDismissButtonStyle.Close
