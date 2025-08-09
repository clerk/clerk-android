package com.clerk.ui.design

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.clerk.api.ui.ClerkDesign

@SuppressLint("ComposeCompositionLocalUsage")
internal val LocalClerkDesign =
  staticCompositionLocalOf<ClerkDesign> { error("No ClerkDesign provided") }

internal val clerkDesign: ClerkDesign
  @Composable @ReadOnlyComposable get() = LocalClerkDesign.current
