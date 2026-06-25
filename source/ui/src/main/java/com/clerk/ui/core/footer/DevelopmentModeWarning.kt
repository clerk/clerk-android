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
import androidx.compose.ui.geometry.Size
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
  showBranding: Boolean = true,
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
  showBranding: Boolean = true,
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
    Box(
      modifier = modifier.fillMaxWidth().height(metrics.height),
      contentAlignment = Alignment.BottomCenter,
    ) {
      DevelopmentModePattern(background = background, modifier = Modifier.matchParentSize())
      Column(
        modifier = Modifier.padding(bottom = metrics.labelBottomPadding),
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
    drawDevModeGrid(warningColor = warningColor)
    val lineHeight = DEV_MODE_GRID_LINE_HEIGHT.toPx()
    drawRect(
      brush =
        Brush.horizontalGradient(
          colorStops =
            arrayOf(
              DEV_MODE_GRID_LINE_START_STOP to warningColor.copy(alpha = 0f),
              DEV_MODE_GRID_LINE_CENTER_STOP to warningColor,
              DEV_MODE_GRID_LINE_END_STOP to warningColor.copy(alpha = 0f),
            )
        ),
      topLeft = Offset(0f, size.height - lineHeight),
      size = Size(size.width, lineHeight),
    )
  }
}

private fun DrawScope.drawDevModeGrid(warningColor: Color) {
  val squareSize = DEV_MODE_GRID_SQUARE_SIZE.toPx()
  val step = squareSize + DEV_MODE_GRID_GAP.toPx()
  val lineHeight = DEV_MODE_GRID_LINE_HEIGHT.toPx()
  val gridHeight = DEV_MODE_GRID_HEIGHT.toPx() - lineHeight
  val gridTop = size.height - lineHeight - gridHeight
  val columns = kotlin.math.ceil(size.width / step).toInt() + 1
  val rows = kotlin.math.ceil(gridHeight / step).toInt() + 1

  for (row in 0 until rows) {
    val y = gridTop + row * step
    for (column in 0 until columns) {
      val x = column * step
      val maskAlpha = devModeGridMaskAlpha(x = x, y = y, gridTop = gridTop, gridHeight = gridHeight)
      if (maskAlpha <= 0f) continue

      val randomOpacity =
        DEV_MODE_GRID_MIN_OPACITY +
          (DEV_MODE_GRID_MAX_OPACITY - DEV_MODE_GRID_MIN_OPACITY) *
            Math.pow(cellRandom(column, row), DEV_MODE_GRID_CONTRAST.toDouble())
      val alpha = randomOpacity.toFloat() * maskAlpha
      drawRect(
        color = warningColor.copy(alpha = alpha),
        topLeft = Offset(x, y),
        size = Size(squareSize, squareSize),
      )
    }
  }
}

private fun DrawScope.devModeGridMaskAlpha(
  x: Float,
  y: Float,
  gridTop: Float,
  gridHeight: Float,
): Float {
  val centerX = size.width / 2f
  val centerY = gridTop + gridHeight * DEV_MODE_GRID_FADE_CENTER_Y_RATIO
  val radiusX = size.width * DEV_MODE_GRID_FADE_WIDTH_RATIO
  val radiusY = DEV_MODE_GRID_FADE_HEIGHT.toPx()
  val normalizedX = (x - centerX) / radiusX
  val normalizedY = (y - centerY) / radiusY
  val distance = kotlin.math.sqrt(normalizedX * normalizedX + normalizedY * normalizedY)
  val edgeOpacity = 1f - DEV_MODE_GRID_FADE_STRENGTH
  return (DEV_MODE_GRID_FADE_CENTER - (DEV_MODE_GRID_FADE_CENTER - edgeOpacity) * distance)
    .coerceIn(edgeOpacity, DEV_MODE_GRID_FADE_CENTER)
}

private fun cellRandom(cellX: Int, cellY: Int): Double {
  var hash =
    cellX.toUInt() * DEV_MODE_GRID_HASH_X_MULTIPLIER +
      cellY.toUInt() * DEV_MODE_GRID_HASH_Y_MULTIPLIER +
      DEV_MODE_GRID_HASH_SEED
  hash = (hash xor (hash shr DEV_MODE_GRID_HASH_FIRST_SHIFT)) * DEV_MODE_GRID_HASH_MIX_MULTIPLIER
  hash = hash xor (hash shr DEV_MODE_GRID_HASH_FINAL_SHIFT)
  return hash.toDouble() / UINT_RANGE
}

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
  val bottomInset = maxOf(reportedBottomInset, DEVELOPMENT_MODE_HOME_INDICATOR_HEIGHT)
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
    labelBottomPadding = bottomInset + DEVELOPMENT_MODE_LABEL_BOTTOM_GAP,
  )
}

private data class DevelopmentModeWarningMetrics(val height: Dp, val labelBottomPadding: Dp)

private val DEVELOPMENT_MODE_BRANDED_BACKGROUND_HEIGHT = 118.dp
private val DEVELOPMENT_MODE_LABEL_ONLY_BACKGROUND_HEIGHT = 118.dp
private val DEVELOPMENT_MODE_HOME_INDICATOR_HEIGHT = 26.dp
private val DEVELOPMENT_MODE_LABEL_BOTTOM_GAP = 13.dp
private val DEVELOPMENT_MODE_BRANDING_LABEL_GAP = 9.dp
private val DEVELOPMENT_MODE_BRANDING_LOGO_GAP = 8.dp
private val DEV_MODE_GRID_HEIGHT = 60.dp
private val DEV_MODE_GRID_SQUARE_SIZE = 1.5.dp
private val DEV_MODE_GRID_GAP = 2.dp
private val DEV_MODE_GRID_FADE_HEIGHT = 20.dp
private val DEV_MODE_GRID_LINE_HEIGHT = 1.dp
private const val DEV_MODE_GRID_MIN_OPACITY = 0.1
private const val DEV_MODE_GRID_MAX_OPACITY = 0.7
private const val DEV_MODE_GRID_CONTRAST = 2
private const val DEV_MODE_GRID_FADE_WIDTH_RATIO = 0.45f
private const val DEV_MODE_GRID_FADE_CENTER_Y_RATIO = 0.85f
private const val DEV_MODE_GRID_FADE_STRENGTH = 0.98f
private const val DEV_MODE_GRID_FADE_CENTER = 0.82f
private const val DEV_MODE_GRID_LINE_START_STOP = 0.02f
private const val DEV_MODE_GRID_LINE_CENTER_STOP = 0.5f
private const val DEV_MODE_GRID_LINE_END_STOP = 0.98f
private const val DEV_MODE_GRID_HASH_X_MULTIPLIER = 374761393u
private const val DEV_MODE_GRID_HASH_Y_MULTIPLIER = 668265263u
private const val DEV_MODE_GRID_HASH_SEED = 2246822519u
private const val DEV_MODE_GRID_HASH_FIRST_SHIFT = 13
private const val DEV_MODE_GRID_HASH_MIX_MULTIPLIER = 1274126177u
private const val DEV_MODE_GRID_HASH_FINAL_SHIFT = 16
private const val UINT_RANGE = 4294967296.0
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
