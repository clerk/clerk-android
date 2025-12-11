package com.clerk.ui.core.badge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.theme.ClerkMaterialTheme

/**
 * A badge component that displays text with various styling options.
 *
 * The badge supports different visual styles through [ClerkBadgeType], including primary,
 * secondary, positive, negative, and warning variants. Each type has its own color scheme and
 * border styling.
 *
 * @param text The text content to display inside the badge
 * @param modifier Optional [Modifier] to be applied to the badge
 * @param badgeType The visual style of the badge, defaults to [ClerkBadgeType.Primary]
 */
@Composable
fun Badge(
  text: String,
  modifier: Modifier = Modifier,
  badgeType: ClerkBadgeType = ClerkBadgeType.Primary,
  clerkTheme: ClerkTheme? = null,
) {
  ClerkMaterialTheme(clerkTheme = clerkTheme) {
    val (backgroundColor, contentColor) =
      when (badgeType) {
        ClerkBadgeType.Primary ->
          ClerkMaterialTheme.colors.primary to ClerkMaterialTheme.colors.primaryForeground
        ClerkBadgeType.Secondary ->
          ClerkMaterialTheme.colors.muted to ClerkMaterialTheme.colors.mutedForeground
        ClerkBadgeType.Positive ->
          ClerkMaterialTheme.computedColors.backgroundSuccess to ClerkMaterialTheme.colors.success
        ClerkBadgeType.Negative ->
          ClerkMaterialTheme.computedColors.backgroundDanger to ClerkMaterialTheme.colors.danger
        ClerkBadgeType.Warning ->
          ClerkMaterialTheme.computedColors.backgroundWarning to ClerkMaterialTheme.colors.warning
      }

    val borderColor =
      when (badgeType) {
        ClerkBadgeType.Primary -> Color.Transparent
        ClerkBadgeType.Secondary -> ClerkMaterialTheme.computedColors.buttonBorder
        ClerkBadgeType.Positive -> ClerkMaterialTheme.colors.success
        ClerkBadgeType.Negative -> ClerkMaterialTheme.colors.danger
        ClerkBadgeType.Warning -> ClerkMaterialTheme.colors.warning
      }

    Surface(
      modifier = Modifier.then(modifier),
      shape = ClerkMaterialTheme.shape,
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

/**
 * Defines the visual styling options for [Badge] components.
 *
 * Each type corresponds to a different color scheme and border treatment:
 * - [Primary]: Primary brand colors with no border
 * - [Secondary]: Muted colors with visible border
 * - [Positive]: Success/positive state colors with matching border
 * - [Negative]: Error/danger state colors with matching border
 * - [Warning]: Warning state colors with matching border
 */
enum class ClerkBadgeType {
  /** Primary brand styling with no border */
  Primary,

  /** Secondary styling with muted colors and border */
  Secondary,

  /** Positive/success state styling with green color scheme */
  Positive,

  /** Negative/error state styling with red color scheme */
  Negative,

  /** Warning state styling with yellow/orange color scheme */
  Warning,
}

@PreviewLightDark
@Composable
private fun PreviewBadge() {
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.padding(dp8).background(color = ClerkMaterialTheme.colors.background).padding(dp8),
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
