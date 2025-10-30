package com.clerk.ui.userprofile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.clerk.ui.core.common.NavigableState
import com.clerk.ui.core.navigation.pop

@Stable
internal class UserProfileState(val backStack: NavBackStack<NavKey>) : NavigableState {
  override fun navigateTo(destination: NavKey) {
    backStack.add(destination)
  }

  override fun navigateBack() {
    backStack.removeLastOrNull()
  }

  override fun clearBackStack() {
    backStack.clear()
  }

  override fun pop(numberOfScreens: Int) {
    backStack.pop(numberOfScreens)
  }
}

@Composable
internal fun PreviewUserProfileStateProvider(content: @Composable () -> Unit) {
  val backStack = rememberNavBackStack()
  UserProfileStateProvider(backStack) { content() }
}
