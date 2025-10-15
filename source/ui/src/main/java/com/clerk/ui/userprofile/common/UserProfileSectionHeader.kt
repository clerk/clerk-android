package com.clerk.ui.userprofile.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun UserProfileSectionHeader(text: String, modifier: Modifier = Modifier) {
  Text(
    modifier = Modifier.padding(horizontal = dp24).then(modifier),
    text = text.uppercase(),
    color = ClerkMaterialTheme.colors.mutedForeground,
    style = ClerkMaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
  )
}
