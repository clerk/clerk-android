package com.clerk.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.clerk.api.Clerk
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.core.common.dimens.dp1
import com.clerk.ui.core.common.dimens.dp12
import com.clerk.ui.core.common.dimens.dp24
import com.clerk.ui.core.common.dimens.dp6
import com.materialkolor.ktx.toHex
import org.junit.Test

class ClerkThemeSnapshotTest : BaseSnapshotTest() {

  @Test
  fun clerkDefaultTheme() {
    Clerk.customTheme = null
    paparazzi.snapshot {
      ClerkMaterialTheme {
        val colors = LocalComposeColors.current
        Column {
          ThemeRow(text = "Primary", color = colors.primary!!, hasBorder = true)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "Background", color = colors.background!!, hasBorder = true)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "Input", color = colors.input!!, hasBorder = true)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "Danger", color = colors.danger!!)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "Success", color = colors.success!!)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "Warning", color = colors.warning!!)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "Foreground", color = colors.foreground!!)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "MutedForeground", color = colors.mutedForeground!!)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "PrimaryForeground", color = colors.primaryForeground!!, hasBorder = true)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "InputForeground", color = colors.inputForeground!!)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "Neutral", color = colors.neutral!!)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "Border", color = colors.border!!)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "Shadow", color = colors.shadow!!)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "Ring", color = colors.ring!!)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "Muted", color = colors.muted!!, hasBorder = true)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
        }
      }
    }
  }

  @Test
  fun clerkDefaultThemeComputedColors() {
    Clerk.customTheme = null
    paparazzi.snapshot {
      ClerkMaterialTheme {
        val computedColors = LocalComputedColors.current
        Column {
          ThemeRow(text = "Primary:hover", color = computedColors.primaryPressed, hasBorder = true)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "Border", color = computedColors.border, hasBorder = true)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "ButtonBorder", color = computedColors.buttonBorder, hasBorder = true)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "InputBorder", color = computedColors.inputBorder)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "InputBorder:hover", color = computedColors.inputBorderFocused)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "Default:focus", color = Color.Red)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "DangerInputBorder", color = computedColors.dangerInputBorder)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(
            text = "DangerInputBorder:focus",
            color = computedColors.dangerInputBorderFocused,
          )
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(
            text = "BackgroundTransparent",
            color = computedColors.backgroundTransparent,
            hasBorder = true,
          )
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "BackgroundSuccess", color = computedColors.backgroundSuccess)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "borderSuccess", color = computedColors.borderSuccess)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "BackgroundDanger", color = computedColors.backgroundDanger)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "BorderDanger", color = computedColors.borderDanger)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "BackgroundWarning", color = computedColors.backgroundWarning)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
          ThemeRow(text = "BorderWarning", color = computedColors.borderWarning)
          HorizontalDivider(thickness = dp1, color = Color.LightGray)
        }
      }
    }
  }
}

@Composable
private fun ThemeRow(text: String, color: Color, hasBorder: Boolean = false) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(dp12),
    horizontalArrangement = Arrangement.spacedBy(dp12, alignment = Alignment.Start),
  ) {
    Text(
      modifier = Modifier.width(150.dp),
      text = text,
      style = MaterialTheme.typography.titleMedium,
    )
    val hasBorderModifier =
      if (hasBorder) Modifier.border(2.dp, color = Color.LightGray, shape = RoundedCornerShape(dp6))
      else Modifier
    Box(
      modifier =
        Modifier.size(dp24)
          .background(color = color, shape = RoundedCornerShape(dp6))
          .then(hasBorderModifier)
    ) {}
    Text(text = color.toHex())
  }
}
