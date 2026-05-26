package com.clerk.ui.core.footer

import androidx.compose.foundation.Image
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
  content: @Composable BoxScope.() -> Unit,
) {
  val shouldShowWarning = shouldShowDevelopmentModeWarning()
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
      modifier =
        modifier
          .fillMaxWidth()
          .height(metrics.height)
          .paint(
            painter = painterResource(background.drawableResId),
            contentScale = ContentScale.FillBounds,
          )
          .padding(bottom = metrics.labelBottomPadding),
      contentAlignment = Alignment.BottomCenter,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showBranding) {
          DevelopmentModeBranding()
          Spacer(modifier = Modifier.height(DEVELOPMENT_MODE_BRANDING_LABEL_GAP))
        }
        Text(text = stringResource(R.string.development_mode), style = DEVELOPMENT_MODE_TEXT_STYLE)
      }
    }
  }
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
    Text(text = stringResource(R.string.secured_by), style = DEVELOPMENT_MODE_BRANDING_TEXT_STYLE)
    Image(painter = painterResource(R.drawable.ic_clerk_logo), contentDescription = "Clerk")
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

private val DEVELOPMENT_MODE_BRANDED_BACKGROUND_HEIGHT = 100.dp
private val DEVELOPMENT_MODE_LABEL_ONLY_BACKGROUND_HEIGHT = 125.dp
private val DEVELOPMENT_MODE_HOME_INDICATOR_HEIGHT = 26.dp
private val DEVELOPMENT_MODE_LABEL_BOTTOM_GAP = 13.dp
private val DEVELOPMENT_MODE_BRANDING_LABEL_GAP = 9.dp
private val DEVELOPMENT_MODE_BRANDING_LOGO_GAP = 8.dp
private val DEVELOPMENT_MODE_WARNING_COLOR = Color(0xFFF36B16)
private val DEVELOPMENT_MODE_TEXT_STYLE =
  TextStyle(
    fontSize = 12.sp,
    lineHeight = 13.sp,
    fontWeight = FontWeight(590),
    color = DEVELOPMENT_MODE_WARNING_COLOR,
    letterSpacing = 0.sp,
  )
private val DEVELOPMENT_MODE_BRANDING_TEXT_STYLE =
  TextStyle(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.Normal,
    color = Color(0xFF747686),
    letterSpacing = 0.sp,
  )
