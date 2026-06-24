package com.clerk.ui.core.footer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.ui.R
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
internal fun DevelopmentModeWarningBox(
  modifier: Modifier = Modifier,
  background: DevelopmentModeWarningBackground = DevelopmentModeWarningBackground.Grey,
  showBranding: Boolean = false,
  showWarning: Boolean = true,
  content: @Composable BoxScope.() -> Unit,
) {
  val shouldShowWarning = showWarning && shouldShowDevelopmentModeWarning()
  val metrics = developmentModeWarningMetrics(showBranding = showBranding)
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
        background = background,
        showBranding = showBranding,
      )
    }
  }
}

@Composable
internal fun DevelopmentModeWarning(
  modifier: Modifier = Modifier,
  background: DevelopmentModeWarningBackground = DevelopmentModeWarningBackground.Grey,
  showBranding: Boolean = false,
) {
  if (!shouldShowDevelopmentModeWarning()) return

  DevelopmentModeWarningContent(
    modifier = modifier,
    metrics = developmentModeWarningMetrics(showBranding = showBranding),
    background = background,
    showBranding = showBranding,
  )
}

@Composable
private fun DevelopmentModeWarningContent(
  metrics: DevelopmentModeWarningMetrics,
  background: DevelopmentModeWarningBackground,
  showBranding: Boolean,
  modifier: Modifier = Modifier,
) {
  ClerkMaterialTheme {
    Box(modifier = modifier.fillMaxWidth().height(metrics.height)) {
      DevelopmentModePattern(background = background, modifier = Modifier.matchParentSize())
      Column(
        modifier = Modifier.align(Alignment.TopCenter).padding(top = metrics.labelTopPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        if (showBranding) {
          DevelopmentModeBranding()
          Spacer(modifier = Modifier.height(DEVELOPMENT_MODE_BRANDING_LABEL_GAP))
        }
        Text(
          text = stringResource(R.string.development_mode),
          style = DEVELOPMENT_MODE_TEXT_STYLE,
          color = ClerkMaterialTheme.colors.warning,
        )
      }
    }
  }
}

@Composable
private fun DevelopmentModePattern(
  background: DevelopmentModeWarningBackground,
  modifier: Modifier = Modifier,
) {
  val baseColor =
    when (background) {
      DevelopmentModeWarningBackground.Grey -> ClerkMaterialTheme.colors.muted
      DevelopmentModeWarningBackground.White -> ClerkMaterialTheme.colors.background
    }
  val warningColor = ClerkMaterialTheme.colors.warning

  Canvas(modifier = modifier) {
    drawRect(color = baseColor)

    drawDevelopmentModeDots(warningColor = warningColor)

    drawRect(
      brush =
        Brush.verticalGradient(
          colorStops =
            arrayOf(
              0f to Color.Transparent,
              DEVELOPMENT_MODE_PATTERN_FADE_STOP to
                baseColor.copy(alpha = DEVELOPMENT_MODE_PATTERN_FADE_ALPHA),
              1f to baseColor,
            )
        )
    )
    val bottomStrokeWidth = DEVELOPMENT_MODE_PATTERN_BOTTOM_STROKE_WIDTH.toPx()
    drawLine(
      color = warningColor.copy(alpha = DEVELOPMENT_MODE_PATTERN_BOTTOM_STROKE_ALPHA),
      start = Offset(0f, size.height - bottomStrokeWidth / 2f),
      end = Offset(size.width, size.height - bottomStrokeWidth / 2f),
      strokeWidth = bottomStrokeWidth,
    )
  }
}

private fun DrawScope.drawDevelopmentModeDots(warningColor: Color) {
  val dotSpacing = DEVELOPMENT_MODE_PATTERN_DOT_SPACING.toPx()
  val dotRadius = DEVELOPMENT_MODE_PATTERN_DOT_RADIUS.toPx()
  val patternTop = size.height * DEVELOPMENT_MODE_PATTERN_TOP_RATIO
  val centerX = size.width / 2f
  var y = patternTop
  var row = 0
  while (y < size.height) {
    val verticalProgress = ((y - patternTop) / (size.height - patternTop)).coerceIn(0f, 1f)
    val rowWidth =
      size.width *
        (DEVELOPMENT_MODE_PATTERN_MIN_WIDTH_RATIO +
          verticalProgress * DEVELOPMENT_MODE_PATTERN_WIDTH_GROWTH_RATIO)
    val startX = centerX - rowWidth / 2f + if (row % 2 == 0) 0f else dotSpacing / 2f
    drawDevelopmentModeDotRow(
      DotRowParams(
        warningColor = warningColor,
        dotRadius = dotRadius,
        dotSpacing = dotSpacing,
        centerX = centerX,
        rowWidth = rowWidth,
        startX = startX,
        y = y,
        verticalProgress = verticalProgress,
      )
    )
    y += dotSpacing
    row += 1
  }
}

private fun DrawScope.drawDevelopmentModeDotRow(params: DotRowParams) {
  var x = params.startX
  while (x <= params.centerX + params.rowWidth / 2f) {
    val horizontalProgress =
      1f - (kotlin.math.abs(x - params.centerX) / (params.rowWidth / 2f)).coerceIn(0f, 1f)
    val verticalAlpha = 1f - params.verticalProgress * DEVELOPMENT_MODE_PATTERN_VERTICAL_FADE_FACTOR
    val alpha =
      verticalAlpha.coerceIn(0f, 1f) * horizontalProgress * DEVELOPMENT_MODE_PATTERN_MAX_ALPHA
    drawCircle(
      color = params.warningColor.copy(alpha = alpha),
      radius = params.dotRadius,
      center = Offset(x, params.y),
    )
    x += params.dotSpacing
  }
}

private data class DotRowParams(
  val warningColor: Color,
  val dotRadius: Float,
  val dotSpacing: Float,
  val centerX: Float,
  val rowWidth: Float,
  val startX: Float,
  val y: Float,
  val verticalProgress: Float,
)

@Composable
private fun DevelopmentModeBranding(modifier: Modifier = Modifier) {
  if (!Clerk.isBranded) return

  Row(
    modifier = modifier,
    horizontalArrangement =
      Arrangement.spacedBy(DEVELOPMENT_MODE_BRANDING_LOGO_GAP, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = stringResource(R.string.secured_by),
      style = DEVELOPMENT_MODE_BRANDING_TEXT_STYLE,
      color = ClerkMaterialTheme.colors.mutedForeground,
    )
    Icon(
      painter = painterResource(R.drawable.ic_clerk_logo),
      contentDescription = "Clerk",
      tint = ClerkMaterialTheme.colors.mutedForeground,
    )
  }
}

@Composable
private fun shouldShowDevelopmentModeWarning(): Boolean {
  val isInitialized by Clerk.isInitialized.collectAsStateWithLifecycle()
  return isInitialized && Clerk.shouldShowDevelopmentModeWarning
}

@Composable
private fun developmentModeWarningMetrics(showBranding: Boolean): DevelopmentModeWarningMetrics {
  val density = LocalDensity.current
  val reportedBottomInset = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
  val designHeight =
    if (showBranding) {
      DEVELOPMENT_MODE_BRANDED_BACKGROUND_HEIGHT
    } else {
      DEVELOPMENT_MODE_LABEL_ONLY_BACKGROUND_HEIGHT
    }
  val height =
    designHeight + maxOf(reportedBottomInset - DEVELOPMENT_MODE_HOME_INDICATOR_HEIGHT, 0.dp)
  return DevelopmentModeWarningMetrics(
    height = height,
    labelTopPadding = DEVELOPMENT_MODE_LABEL_TOP_GAP,
  )
}

private data class DevelopmentModeWarningMetrics(val height: Dp, val labelTopPadding: Dp)

private val DEVELOPMENT_MODE_BRANDED_BACKGROUND_HEIGHT = 118.dp
private val DEVELOPMENT_MODE_LABEL_ONLY_BACKGROUND_HEIGHT = 118.dp
private val DEVELOPMENT_MODE_HOME_INDICATOR_HEIGHT = 26.dp
private val DEVELOPMENT_MODE_LABEL_TOP_GAP = 17.dp
private val DEVELOPMENT_MODE_BRANDING_LABEL_GAP = 9.dp
private val DEVELOPMENT_MODE_BRANDING_LOGO_GAP = 8.dp
private val DEVELOPMENT_MODE_PATTERN_DOT_SPACING = 5.dp
private val DEVELOPMENT_MODE_PATTERN_DOT_RADIUS = 1.15.dp
private val DEVELOPMENT_MODE_PATTERN_BOTTOM_STROKE_WIDTH = 1.dp
private const val DEVELOPMENT_MODE_PATTERN_TOP_RATIO = 0f
private const val DEVELOPMENT_MODE_PATTERN_MIN_WIDTH_RATIO = 0.72f
private const val DEVELOPMENT_MODE_PATTERN_WIDTH_GROWTH_RATIO = 0.22f
private const val DEVELOPMENT_MODE_PATTERN_FADE_STOP = 0.62f
private const val DEVELOPMENT_MODE_PATTERN_FADE_ALPHA = 0.58f
private const val DEVELOPMENT_MODE_PATTERN_MAX_ALPHA = 0.24f
private const val DEVELOPMENT_MODE_PATTERN_VERTICAL_FADE_FACTOR = 0.86f
private const val DEVELOPMENT_MODE_PATTERN_BOTTOM_STROKE_ALPHA = 0.22f
private val DEVELOPMENT_MODE_TEXT_STYLE =
  TextStyle(
    fontSize = 17.sp,
    lineHeight = 24.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 0.sp,
  )
private val DEVELOPMENT_MODE_BRANDING_TEXT_STYLE =
  TextStyle(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp,
  )
