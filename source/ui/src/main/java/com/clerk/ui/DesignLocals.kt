package com.clerk.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.clerk.api.ui.ClerkDesign

val LocalClerkDesign = staticCompositionLocalOf<ClerkDesign> { error("No ClerkDesign provided") }

val clerkDesign: ClerkDesign
  @Composable @ReadOnlyComposable get() = LocalClerkDesign.current
