package com.clerk.ui.userprofile.custom

import android.annotation.SuppressLint
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Navigation API available to custom destination views inside
 * [com.clerk.ui.userprofile.UserProfileView].
 *
 * Access via [LocalUserProfileCustomNavigator].
 */
class UserProfileCustomNavigator
internal constructor(
  private val pushAction: (String) -> Unit,
  private val popToRootAction: () -> Unit,
  private val navigateBackAction: () -> Unit,
) {
  /** Push another custom route key onto the navigation stack. */
  fun push(routeKey: String) {
    pushAction(routeKey)
  }

  /** Pop back to the root user profile account screen. */
  fun popToRoot() {
    popToRootAction()
  }

  /** Navigate back one screen. */
  fun navigateBack() {
    navigateBackAction()
  }
}

@SuppressLint("ComposeCompositionLocalUsage")
val LocalUserProfileCustomNavigator =
  staticCompositionLocalOf<UserProfileCustomNavigator> {
    error(
      "No UserProfileCustomNavigator provided. " +
        "This is only available inside UserProfileView custom destinations."
    )
  }
