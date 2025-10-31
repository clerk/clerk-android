package com.clerk.ui.userprofile.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp4
import com.clerk.ui.core.extensions.withSemiBoldWeight
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun BottomSheetTopBar(title: String, onClosePressed: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = dp4),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    IconButton(onClick = {}) {}
    Spacer(modifier = Modifier.weight(1f))
    Text(
      modifier = Modifier.padding(bottom = dp12),
      text = title,
      style = ClerkMaterialTheme.typography.titleMedium.withSemiBoldWeight(),
      color = ClerkMaterialTheme.colors.foreground,
    )
    Spacer(modifier = Modifier.weight(1f))
    IconButton(onClosePressed) {
      Icon(
        modifier = Modifier.size(dp24),
        painter = painterResource(R.drawable.ic_cross),
        contentDescription = stringResource(R.string.close),
      )
    }
  }
  HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
}
