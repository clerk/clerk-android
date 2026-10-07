package com.clerk.ui.userprofile.custom

import android.annotation.SuppressLint
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Navigation API available to custom destination views inside
 * [com.clerk.ui.userprofile.UserProfileView].
 *
 * Access via [LocalUserProfileCustomNavigator].
 */
public class UserProfileCustomNavigator
internal constructor(
  private val pushAction: (String) -> Unit,
  private val popToRootAction: () -> Unit,
  private val navigateBackAction: () -> Unit,
) {
  /** Push another custom route key onto the navigation stack. */
  public fun push(routeKey: String) {
    pushAction(routeKey)
  }

  /** Pop back to the root user profile account screen. */
  public fun popToRoot() {
    popToRootAction()
  }

  /** Navigate back one screen. */
  public fun navigateBack() {
    navigateBackAction()
  }
}

@SuppressLint("ComposeCompositionLocalUsage")
public val LocalUserProfileCustomNavigator: ProvidableCompositionLocal<UserProfileCustomNavigator> =
  staticCompositionLocalOf<UserProfileCustomNavigator> {
    error(
      "No UserProfileCustomNavigator provided. " +
        "This is only available inside UserProfileView custom destinations."
    )
  }
