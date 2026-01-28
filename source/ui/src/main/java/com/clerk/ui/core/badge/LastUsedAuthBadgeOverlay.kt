package com.clerk.ui.core.badge

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp8

@Composable
internal fun LastUsedAuthBadgeOverlay(
  isVisible: Boolean,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Box(modifier = modifier) {
    content()
    if (isVisible) {
      Badge(
        text = stringResource(R.string.last_used_badge),
        badgeType = ClerkBadgeType.Secondary,
        modifier = Modifier.align(Alignment.TopEnd).padding(end = dp8).offset(y = -dp8),
      )
    }
  }
}
