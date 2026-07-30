package com.clerk.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.clerk.api.FrameworkIntegrationApi

/**
 * Lets a host that embeds Clerk components inside its own navigation chrome (for example Clerk's
 * Expo SDK, or an app with its own top app bar) hide Clerk's top app bars while observing and
 * driving the component's internal navigation stack.
 *
 * Pass an instance to [UserProfileView][com.clerk.ui.userprofile.UserProfileView] or
 * [AuthView][com.clerk.ui.auth.AuthView]. The component then hides its top app bars, keeps [depth]
 * up to date as its internal back stack changes, and executes [pop] / [popToRoot] against that
 * stack. The host owns all header chrome, including back affordances: render your own back button
 * and call [pop] while [canGoBack] is `true`.
 *
 * When no instance is passed (the default), component behavior is unchanged.
 *
 * Only one component drives an instance at a time.
 *
 * Obtaining an instance requires opting in to [FrameworkIntegrationApi]: this surface exists for
 * Clerk's own framework integrations and may change without notice in minor releases.
 */
@Stable
class ClerkEmbeddedNavigation @FrameworkIntegrationApi constructor() {
  /** The number of screens pushed above the embedded component's root screen. */
  var depth: Int by mutableIntStateOf(0)
    internal set

  /** Whether the embedded component's internal stack has screens to pop. */
  val canGoBack: Boolean
    get() = depth > 0

  internal var popHandler: ((toRoot: Boolean) -> Unit)? = null

  /** Pops one screen off the embedded component's internal stack. No-op at the root. */
  fun pop() {
    popHandler?.invoke(false)
  }

  /** Pops the embedded component's internal stack back to its root screen. */
  fun popToRoot() {
    popHandler?.invoke(true)
  }
}

/** Creates and remembers a [ClerkEmbeddedNavigation] for embedding Clerk components. */
@FrameworkIntegrationApi
@Composable
fun rememberClerkEmbeddedNavigation(): ClerkEmbeddedNavigation {
  return remember { ClerkEmbeddedNavigation() }
}

/**
 * Embedded navigation for the enclosing Clerk component, or `null` when the component renders its
 * own navigation chrome. [ClerkTopAppBar][com.clerk.ui.core.appbar.ClerkTopAppBar] renders nothing
 * while this is non-null.
 */
@SuppressLint("ComposeCompositionLocalUsage")
internal val LocalClerkEmbeddedNavigation =
  staticCompositionLocalOf<ClerkEmbeddedNavigation?> { null }

/**
 * Connects [embeddedNavigation] to a component's [backStack]: publishes depth changes and executes
 * pop commands. No-op when [embeddedNavigation] is `null`.
 */
@Composable
internal fun EmbeddedNavigationEffects(
  embeddedNavigation: ClerkEmbeddedNavigation?,
  backStack: NavBackStack<NavKey>,
) {
  if (embeddedNavigation == null) return

  LaunchedEffect(embeddedNavigation, backStack) {
    snapshotFlow { backStack.size }
      .collect { size -> embeddedNavigation.depth = (size - 1).coerceAtLeast(0) }
  }

  DisposableEffect(embeddedNavigation, backStack) {
    val handler: (toRoot: Boolean) -> Unit = { toRoot ->
      if (toRoot) {
        while (backStack.size > 1) {
          backStack.removeLastOrNull()
        }
      } else if (backStack.size > 1) {
        backStack.removeLastOrNull()
      }
    }
    embeddedNavigation.popHandler = handler
    onDispose {
      // Only tear down our own registration: a component leaving composition after
      // a successor has registered must not clear the successor's handler. Reset
      // depth so the handle doesn't advertise canGoBack with a no-op pop.
      if (embeddedNavigation.popHandler === handler) {
        embeddedNavigation.popHandler = null
        embeddedNavigation.depth = 0
      }
    }
  }
}
