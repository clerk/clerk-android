package com.clerk.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.PathEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset

internal const val CLERK_NAVIGATION_TRANSITION_DURATION_MILLIS = 450
private const val CLERK_NAVIGATION_TRANSITION_DISTANCE_FRACTION = 0.1f
private const val FOREGROUND_ALPHA_DURATION_MILLIS = 83
private const val FORWARD_ALPHA_DELAY_MILLIS = 50
private const val POP_ALPHA_DELAY_MILLIS = 35

private val FastOutExtraSlowInEasing =
  PathEasing(
    Path().apply {
      moveTo(x = 0f, y = 0f)
      cubicTo(x1 = 0.05f, y1 = 0f, x2 = 0.133333f, y2 = 0.06f, x3 = 0.166666f, y3 = 0.4f)
      cubicTo(x1 = 0.208333f, y1 = 0.82f, x2 = 0.25f, y2 = 1f, x3 = 1f, y3 = 1f)
    }
  )

/** Matches React Navigation's default Android native-stack forward transition. */
internal fun clerkNavigationForwardTransition(): ContentTransform {
  val slideSpec = clerkNavigationSlideSpec<IntOffset>()
  val enter =
    slideInHorizontally(animationSpec = slideSpec, initialOffsetX = { it / 10 }) +
      fadeIn(animationSpec = clerkNavigationForwardAlphaSpec(), initialAlpha = 0f)
  val exit = slideOutHorizontally(animationSpec = slideSpec, targetOffsetX = { -(it / 10) })
  return enter togetherWith exit
}

/** Matches React Navigation's default Android native-stack pop transition. */
internal fun clerkNavigationPopTransition(): ContentTransform {
  val slideSpec = clerkNavigationSlideSpec<IntOffset>()
  val enter = slideInHorizontally(animationSpec = slideSpec, initialOffsetX = { -(it / 10) })
  val exit =
    slideOutHorizontally(animationSpec = slideSpec, targetOffsetX = { it / 10 }) +
      fadeOut(animationSpec = clerkNavigationPopAlphaSpec(), targetAlpha = 0f)
  return enter togetherWith exit
}

internal fun clerkNavigationSlideProgressSpec(): FiniteAnimationSpec<Float> =
  clerkNavigationSlideSpec()

internal fun clerkNavigationForwardAlphaSpec(): FiniteAnimationSpec<Float> =
  tween(
    durationMillis = FOREGROUND_ALPHA_DURATION_MILLIS,
    delayMillis = FORWARD_ALPHA_DELAY_MILLIS,
    easing = LinearEasing,
  )

internal fun clerkNavigationPopAlphaSpec(): FiniteAnimationSpec<Float> =
  tween(
    durationMillis = FOREGROUND_ALPHA_DURATION_MILLIS,
    delayMillis = POP_ALPHA_DELAY_MILLIS,
    easing = LinearEasing,
  )

private fun <T> clerkNavigationSlideSpec(): FiniteAnimationSpec<T> =
  tween(
    durationMillis = CLERK_NAVIGATION_TRANSITION_DURATION_MILLIS,
    easing = FastOutExtraSlowInEasing,
  )

/** Applies the forward transition's incoming-screen transform at [progress]. */
internal fun Modifier.clerkNavigationForwardEnterTransform(
  progress: Float,
  alpha: Float,
): Modifier = graphicsLayer {
  translationX = size.width * CLERK_NAVIGATION_TRANSITION_DISTANCE_FRACTION * (1f - progress)
  this.alpha = alpha
}

/** Applies the forward transition's previous-screen transform at [progress]. */
internal fun Modifier.clerkNavigationForwardExitTransform(progress: Float): Modifier =
  graphicsLayer {
    translationX = -size.width * CLERK_NAVIGATION_TRANSITION_DISTANCE_FRACTION * progress
  }
