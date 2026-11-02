package com.clerk.ui.core.common

import androidx.navigation3.runtime.NavKey

internal interface NavigableState<T> {
  fun navigateTo(destination: NavKey)

  fun navigateBack()

  fun clearBackStack()

  fun pop(numberOfScreens: Int)

  fun popTo(destination: T)
}
