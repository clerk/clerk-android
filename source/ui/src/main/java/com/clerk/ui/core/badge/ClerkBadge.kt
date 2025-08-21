package com.clerk.ui.core.badge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.sp
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.LocalClerkColors
import com.clerk.ui.theme.LocalClerkDesign
import com.clerk.ui.theme.LocalComputedColors

@Composable
fun Badge(
  text: String,
  modifier: Modifier = Modifier,
  badgeType: ClerkBadgeType = ClerkBadgeType.Primary,
) {
  val design = LocalClerkDesign.current
  val colors = LocalClerkColors.current
  val computedColors = LocalComputedColors.current
  val (backgroundColor, contentColor) =
    when (badgeType) {
      ClerkBadgeType.Primary ->
        MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
      ClerkBadgeType.Secondary -> MaterialTheme.colorScheme.secondary to colors.mutedForeground!!
      ClerkBadgeType.Positive -> computedColors.backgroundSuccess to colors.success!!
      ClerkBadgeType.Negative -> computedColors.backgroundDanger to MaterialTheme.colorScheme.error
      ClerkBadgeType.Warning -> computedColors.backgroundWarning to colors.warning!!
    }

  val borderColor =
    when (badgeType) {
      ClerkBadgeType.Primary -> Color.Transparent
      ClerkBadgeType.Secondary -> computedColors.buttonBorder
      ClerkBadgeType.Positive -> colors.success!!
      ClerkBadgeType.Negative -> colors.danger!!
      ClerkBadgeType.Warning -> colors.warning!!
    }

  ClerkMaterialTheme {
    Surface(
      modifier = Modifier.then(modifier),
      shape = RoundedCornerShape(design.borderRadius),
      color = backgroundColor,
      contentColor = contentColor,
      border = BorderStroke(dp1, borderColor),
    ) {
      Text(
        modifier = Modifier.padding(horizontal = dp8),
        text = text,
        style =
          MaterialTheme.typography.labelLarge.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
          ),
      )
    }
  }
}

enum class ClerkBadgeType {
  Primary,
  Secondary,
  Positive,
  Negative,
  Warning,
}

@PreviewLightDark
@Composable
private fun PreviewBadge() {
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.padding(dp8).background(color = MaterialTheme.colorScheme.background).padding(dp8),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(dp12, alignment = Alignment.CenterVertically),
    ) {
      Badge(text = "Badge Label")
      Badge(text = "Badge Label", badgeType = ClerkBadgeType.Secondary)
      Badge(text = "Badge Label", badgeType = ClerkBadgeType.Positive)
      Badge(text = "Badge Label", badgeType = ClerkBadgeType.Negative)
      Badge(text = "Badge Label", badgeType = ClerkBadgeType.Warning)
    }
  }
}
