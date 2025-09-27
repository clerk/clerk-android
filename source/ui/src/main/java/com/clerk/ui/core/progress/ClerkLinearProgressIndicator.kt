package com.clerk.ui.core.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.core.common.dimens.dp12
import com.clerk.ui.core.common.dimens.dp4
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors

/**
 * A linear progress indicator that displays a series of segments, with a certain number of them
 * highlighted to indicate progress.
 *
 * @param progress The number of segments to highlight, starting from the left.
 * @param modifier A [Modifier] to apply to the progress indicator.
 * @param segments The total number of segments to display.
 */
@Composable
internal fun ClerkLinearProgressIndicator(
  progress: Int,
  modifier: Modifier = Modifier,
  segments: Int = 4,
) {
  ClerkMaterialTheme {
    Row(
      modifier = Modifier.fillMaxWidth().then(modifier),
      horizontalArrangement = Arrangement.spacedBy(dp12, alignment = Alignment.CenterHorizontally),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      repeat(segments) { index ->
        val backgroundColor =
          if (index < progress) {
            ClerkMaterialTheme.colors.primary
          } else {
            ClerkMaterialTheme.colors.neutral.copy(alpha = 0.11f)
          }
        Box(
          modifier =
            Modifier.weight(1f).height(dp4).clip(CircleShape).background(color = backgroundColor)
        )
      }
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
  ClerkMaterialTheme {
    Box(
      modifier =
        Modifier.fillMaxWidth().background(ClerkMaterialTheme.colors.background).padding(dp12)
    ) {
      ClerkLinearProgressIndicator(progress = 2)
    }
  }
}
