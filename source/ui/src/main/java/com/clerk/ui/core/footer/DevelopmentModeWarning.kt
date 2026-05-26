package com.clerk.ui.core.footer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun DevelopmentModeWarningBox(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val shouldShowWarning = shouldShowDevelopmentModeWarning()
  val metrics = developmentModeWarningMetrics()
  Box(modifier = modifier) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .then(if (shouldShowWarning) Modifier.padding(bottom = metrics.height) else Modifier)
    ) {
      content()
    }
    if (shouldShowWarning) {
      DevelopmentModeWarningContent(
        modifier = Modifier.align(Alignment.BottomCenter),
        metrics = metrics,
      )
    }
  }
}

@Composable
internal fun DevelopmentModeWarning(modifier: Modifier = Modifier) {
  if (!shouldShowDevelopmentModeWarning()) return

  DevelopmentModeWarningContent(modifier = modifier, metrics = developmentModeWarningMetrics())
}

@Composable
private fun DevelopmentModeWarningContent(
  metrics: DevelopmentModeWarningMetrics,
  modifier: Modifier = Modifier,
) {
  ClerkMaterialTheme {
    Box(
      modifier =
        modifier
          .fillMaxWidth()
          .height(metrics.height)
          .drawBehind {
            drawRect(DEVELOPMENT_MODE_BACKGROUND_GRADIENT)
            drawDevelopmentModeStripes(color = DEVELOPMENT_MODE_WARNING_COLOR)
          }
          .padding(bottom = metrics.labelBottomPadding),
      contentAlignment = Alignment.BottomCenter,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SecuredByClerkView(hideWhenDevelopmentModeWarning = false)
        Spacer(modifier = Modifier.height(DEVELOPMENT_MODE_BRANDING_LABEL_GAP))
        Text(text = stringResource(R.string.development_mode), style = DEVELOPMENT_MODE_TEXT_STYLE)
      }
    }
  }
}

@Composable
private fun shouldShowDevelopmentModeWarning(): Boolean {
  val isInitialized by Clerk.isInitialized.collectAsStateWithLifecycle()
  return isInitialized && Clerk.shouldShowDevelopmentModeWarning
}

@Composable
private fun developmentModeWarningMetrics(): DevelopmentModeWarningMetrics {
  val density = LocalDensity.current
  val reportedBottomInset = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
  val bottomInset = maxOf(reportedBottomInset, DEVELOPMENT_MODE_HOME_INDICATOR_HEIGHT)
  val height =
    DEVELOPMENT_MODE_BACKGROUND_HEIGHT +
      maxOf(reportedBottomInset - DEVELOPMENT_MODE_HOME_INDICATOR_HEIGHT, 0.dp)
  return DevelopmentModeWarningMetrics(
    height = height,
    labelBottomPadding = bottomInset + DEVELOPMENT_MODE_LABEL_BOTTOM_GAP,
  )
}

private fun DrawScope.drawDevelopmentModeStripes(color: Color) {
  val stripeBrush =
    Brush.verticalGradient(
      colorStops =
        arrayOf(
          0.0f to Color.Transparent,
          DEVELOPMENT_MODE_STRIPE_FADE_START to Color.Transparent,
          1.0f to color.copy(alpha = DEVELOPMENT_MODE_STRIPE_ALPHA),
        ),
      startY = 0f,
      endY = size.height,
    )
  val spacing = dp12.toPx()
  val strokeWidth = dp1.toPx()
  var x = -size.height
  while (x < size.width + size.height) {
    drawLine(
      brush = stripeBrush,
      start = Offset(x, size.height),
      end = Offset(x + size.height, 0f),
      strokeWidth = strokeWidth,
    )
    x += spacing
  }
}

private data class DevelopmentModeWarningMetrics(val height: Dp, val labelBottomPadding: Dp)

private val DEVELOPMENT_MODE_BACKGROUND_HEIGHT = 100.dp
private val DEVELOPMENT_MODE_HOME_INDICATOR_HEIGHT = 26.dp
private val DEVELOPMENT_MODE_LABEL_BOTTOM_GAP = 13.dp
private val DEVELOPMENT_MODE_BRANDING_LABEL_GAP = 9.dp
private val DEVELOPMENT_MODE_WARNING_COLOR = Color(0xFFF36B16)
private val DEVELOPMENT_MODE_BACKGROUND_GRADIENT =
  Brush.verticalGradient(colors = listOf(Color(0xFFF9F9F9), Color(0x99FFF6ED)))
private val DEVELOPMENT_MODE_TEXT_STYLE =
  TextStyle(
    fontSize = 12.sp,
    lineHeight = 13.sp,
    fontWeight = FontWeight(590),
    color = DEVELOPMENT_MODE_WARNING_COLOR,
    letterSpacing = 0.sp,
  )
private const val DEVELOPMENT_MODE_STRIPE_ALPHA = 0.12f
private const val DEVELOPMENT_MODE_STRIPE_FADE_START = 0.45f
