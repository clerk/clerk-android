package com.clerk.ui.userprofile.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors

@Composable
fun UserProfileButtonRow(
  text: String,
  modifier: Modifier = Modifier,
  textColor: Color = ClerkMaterialTheme.colors.primary,
  textStyle: TextStyle = ClerkMaterialTheme.typography.bodyLarge.withMediumWeight(),
  onClick: () -> Unit,
) {

  ClerkMaterialTheme {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(ClerkMaterialTheme.colors.background)
          .clickable { onClick() }
          .padding(vertical = dp16)
          .padding(horizontal = dp24)
          .then(modifier)
    ) {
      Text(text = text, color = textColor, style = textStyle)
    }
  }
}

@Preview
@Composable
private fun Preview() {
  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
  ClerkMaterialTheme { UserProfileButtonRow(text = "Button Row") {} }
}
