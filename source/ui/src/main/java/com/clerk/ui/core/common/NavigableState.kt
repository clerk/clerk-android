package com.clerk.ui.core.common

import androidx.navigation3.runtime.NavKey

internal interface NavigableState<T> {
  fun navigateTo(destination: NavKey)

  fun navigateBack()

  fun clearBackStack()

  fun pop(numberOfScreens: Int)

  /**
   * Pops the navigation back stack up to a specific destination.
   *
   * This function removes all destinations from the top of the back stack until the specified
   * [destination] is reached. The specified [destination] itself is not popped.
   *
   * @param destination The [NavKey] of the destination to pop up to.
   */
  fun popTo(destination: T)
}
