package com.clerk.ui.auth

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.clerk.api.Constants
import com.clerk.api.network.model.factor.Factor
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthViewRestoredNavigationTest {

  @Test
  fun `discardRestoredAuthNavigation clears restored sign in stack`() {
    val factor = Factor(strategy = Constants.Strategy.TOTP)
    val restoredBackStack =
      NavBackStack<NavKey>(AuthDestination.AuthStart, AuthDestination.SignInFactorTwo(factor))

    discardRestoredAuthNavigation(restoredBackStack)

    assertEquals(listOf(AuthDestination.AuthStart), restoredBackStack.toList())
  }

  @Test
  fun `discardRestoredAuthNavigation clears restored sign up stack`() {
    val restoredBackStack =
      NavBackStack<NavKey>(
        AuthDestination.AuthStart,
        AuthDestination.SignUpCompleteProfile(progress = 1),
      )

    discardRestoredAuthNavigation(restoredBackStack)

    assertEquals(listOf(AuthDestination.AuthStart), restoredBackStack.toList())
  }

  @Test
  fun `discardRestoredAuthNavigation preserves auth start`() {
    val restoredBackStack = NavBackStack<NavKey>(AuthDestination.AuthStart)

    discardRestoredAuthNavigation(restoredBackStack)

    assertEquals(listOf(AuthDestination.AuthStart), restoredBackStack.toList())
  }
}
