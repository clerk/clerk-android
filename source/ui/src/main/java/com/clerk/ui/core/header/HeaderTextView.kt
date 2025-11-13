package com.clerk.ui.core.header

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.sp
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.theme.ClerkElementTheme
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.mergeElementTheme

@Composable
internal fun HeaderTextView(
  text: String,
  type: HeaderType,
  modifier: Modifier = Modifier,
  elementTheme: ClerkElementTheme? = null,
) {
  ClerkMaterialTheme {
    val mergedTheme = mergeElementTheme(elementTheme)
    val (style, color) =
      when (type) {
        HeaderType.Title ->
          mergedTheme.typography.titleMedium
            .withMediumWeight()
            .copy(fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp) to
            mergedTheme.colors.foreground
        HeaderType.Subtitle ->
          mergedTheme.typography.bodyLarge to mergedTheme.colors.mutedForeground
      }

    Text(
      text = text,
      style = style,
      color = color,
      modifier = Modifier.then(modifier),
      textAlign = TextAlign.Center,
    )
  }
}

enum class HeaderType {
  Title,
  Subtitle,
}

@PreviewLightDark
@Composable
private fun PreviewHeaderTextView() {
  ClerkMaterialTheme {
    Column(
      modifier = Modifier.background(color = ClerkMaterialTheme.colors.background),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(dp8, Alignment.CenterVertically),
    ) {
      HeaderTextView(text = "Enter password", type = HeaderType.Title)
      HeaderTextView(
        text = "Enter the password associated with your account",
        type = HeaderType.Subtitle,
      )
    }
  }
}
