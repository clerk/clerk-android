package com.clerk.ui.organizationprofile.custom

import android.annotation.SuppressLint
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Navigation API available to custom destination views inside
 * [com.clerk.ui.organizationprofile.OrganizationProfileView].
 *
 * Access via [LocalOrganizationProfileCustomNavigator].
 */
class OrganizationProfileCustomNavigator
internal constructor(
  private val pushAction: (String) -> Unit,
  private val popToRootAction: () -> Unit,
  private val navigateBackAction: () -> Unit,
) {
  /** Push another custom route key onto the organization profile navigation stack. */
  fun push(routeKey: String) {
    pushAction(routeKey)
  }

  /** Pop back to the root organization profile screen. */
  fun popToRoot() {
    popToRootAction()
  }

  /** Navigate back one screen. */
  fun navigateBack() {
    navigateBackAction()
  }
}

/**
 * Composition local that exposes organization profile custom-destination navigation.
 *
 * This value is provided only while rendering a custom destination from
 * [com.clerk.ui.organizationprofile.OrganizationProfileView] or
 * [com.clerk.ui.organizationswitcher.OrganizationSwitcher].
 */
@SuppressLint("ComposeCompositionLocalUsage")
val LocalOrganizationProfileCustomNavigator =
  staticCompositionLocalOf<OrganizationProfileCustomNavigator> {
    error(
      "No OrganizationProfileCustomNavigator provided. " +
        "This is only available inside OrganizationProfileView custom destinations."
    )
  }
