package com.clerk.ui.core.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.core.common.dimens.dp8
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors

@Composable
fun TextButton(
  text: String,
  modifier: Modifier = Modifier,
  textColor: Color = ClerkMaterialTheme.colors.primary,
  textStyle: TextStyle = ClerkMaterialTheme.typography.titleSmall,
  onClick: () -> Unit,
) {
  ClerkMaterialTheme {
    Box(modifier = Modifier.padding(horizontal = dp8).then(modifier).clickable { onClick() }) {
      Text(text = text, color = textColor, style = textStyle)
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewTextButton() {
  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
  ClerkMaterialTheme { TextButton(text = "Text Button", onClick = {}) }
}
