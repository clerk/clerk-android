package com.clerk.api.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Represents the design configuration for Clerk UI components.
 *
 * @property borderRadius The corner radius for the design elements.
 * @property logoMaxHeight The maximum height of the application logo shown on authentication
 *   screens. The logo maintains its aspect ratio within this height.
 */
data class ClerkDesign(val borderRadius: Dp = 8.dp, val logoMaxHeight: Dp = 44.dp)
