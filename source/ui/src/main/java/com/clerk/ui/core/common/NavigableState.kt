package com.clerk.ui.core.common

import androidx.navigation3.runtime.NavKey

/**
 * Represents the state of a component that can handle navigation actions. Provides a standardized
 * way to interact with a navigation controller.
 */
internal interface NavigableState {
  /**
   * Navigates to the specified destination.
   *
   * This function is used to move to a new screen or state within the navigation graph.
   *
   * @param destination The [NavKey] representing the target destination to navigate to.
   */
  fun navigateTo(destination: NavKey)

  /**
   * Navigates to the previous screen in the back stack. If the back stack is empty, this operation
   * may have no effect.
   */
  fun navigateBack()

  /**
   * Clears the entire back stack, removing all previous navigation destinations. After calling
   * this, navigating back will typically exit the current navigation graph or activity.
   */
  fun clearBackStack()

  /**
   * Pops the current destination from the back stack.
   *
   * This is equivalent to `navigateBack`, but the name "pop" is often used in navigation contexts
   * and can be more intuitive for developers familiar with stack-based navigation.
   */
  fun pop(numberOfScreens: Int)
}
