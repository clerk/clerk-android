package com.clerk.ui.auth

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.clerk.api.Constants
import com.clerk.api.network.model.factor.Factor
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthStateCredentialResetTest {

  @Test
  fun navigatingBackToAuthStartClearsSignInCredentials() {
    val factor = Factor(strategy = Constants.Strategy.PASSWORD)
    val backStack =
      NavBackStack<NavKey>(AuthDestination.AuthStart, AuthDestination.SignInFactorOne(factor))
    val authState =
      AuthState(backStack = backStack, sharedPreferences = InMemorySharedPreferences())
    authState.signInPassword = "Password123!"
    authState.signInNewPassword = "NewPassword123!"
    authState.signInConfirmNewPassword = "NewPassword123!"
    authState.signInBackupCode = "backup-code"

    authState.navigateBack()
    authState.navigateTo(AuthDestination.SignInFactorOne(factor))

    assertEquals("", authState.signInPassword)
    assertEquals("", authState.signInNewPassword)
    assertEquals("", authState.signInConfirmNewPassword)
    assertEquals("", authState.signInBackupCode)
  }
}
