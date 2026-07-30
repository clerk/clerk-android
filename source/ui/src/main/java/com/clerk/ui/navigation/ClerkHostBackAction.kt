package com.clerk.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.clerk.api.FrameworkIntegrationApi

/**
 * Supplies the back affordance for the root screen of a Clerk component embedded in a host's own
 * navigation (for example Clerk's Expo SDK, or an app pushing the component onto its own back
 * stack).
 *
 * The component keeps its own top app bar, so screen titles, back buttons, and transitions inside
 * the component stay native. Only the root back button belongs to the host: at the component's root
 * screen it is shown in place of nothing and invokes [onHostBack], which should pop the host's
 * navigation.
 *
 * Wrap the component to enable it:
 * ```kotlin
 * ClerkHostBackActionProvider(onHostBack = { navController.popBackStack() }) {
 *   UserProfileView()
 * }
 * ```
 *
 * This surface exists for Clerk's own framework integrations and may change without notice in minor
 * releases.
 */
@FrameworkIntegrationApi
@Composable
fun ClerkHostBackActionProvider(onHostBack: () -> Unit, content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalClerkHostBackAction provides onHostBack, content = content)
}

/**
 * The host's back action for an embedded component's root screen, or `null` when the component is
 * not embedded. [ClerkTopAppBar][com.clerk.ui.core.appbar.ClerkTopAppBar] shows a back button at
 * the root while this is non-null.
 */
@SuppressLint("ComposeCompositionLocalUsage")
internal val LocalClerkHostBackAction = staticCompositionLocalOf<(() -> Unit)?> { null }
