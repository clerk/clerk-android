package com.clerk.ui.auth

import androidx.navigation3.runtime.NavBackStack
import com.clerk.api.protect.ProtectCheckResource
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthStateProtectRoutingTest {

  @Test
  fun `protect-gated sign in routes to Protect check`() {
    val authState = createAuthState()
    val signIn = SignIn(id = "sia_123", status = SignIn.Status.NEEDS_PROTECT_CHECK)

    authState.setToStepForStatus(signIn) {}

    assertEquals(AuthDestination.SignInProtectCheck, authState.backStack.last())
  }

  @Test
  fun `protect check resource is authoritative even under another sign in status`() {
    val authState = createAuthState()
    val signIn =
      SignIn(
        id = "sia_123",
        status = SignIn.Status.NEEDS_FIRST_FACTOR,
        protectCheck = protectCheck(),
      )

    authState.setToStepForStatus(signIn) {}

    assertEquals(AuthDestination.SignInProtectCheck, authState.backStack.last())
  }

  @Test
  fun `protect-gated sign up routes before other missing fields`() {
    val authState = createAuthState()
    val signUp =
      SignUp(
        id = "sua_123",
        status = SignUp.Status.MISSING_REQUIREMENTS,
        requiredFields = listOf("password"),
        optionalFields = emptyList(),
        missingFields = listOf("password", "protect_check"),
        unverifiedFields = emptyList(),
        verifications = emptyMap(),
        passwordEnabled = false,
      )

    authState.setToStepForStatus(signUp) {}

    assertEquals(AuthDestination.SignUpProtectCheck, authState.backStack.last())
  }

  @Test
  fun `routing the same Protect check twice does not duplicate the destination`() {
    val authState = createAuthState()
    val signIn = SignIn(id = "sia_123", status = SignIn.Status.NEEDS_PROTECT_CHECK)

    authState.setToStepForStatus(signIn) {}
    authState.setToStepForStatus(signIn) {}

    assertEquals(
      listOf(AuthDestination.AuthStart, AuthDestination.SignInProtectCheck),
      authState.backStack.toList(),
    )
  }

  private fun createAuthState(): AuthState {
    return AuthState(
      backStack = NavBackStack(AuthDestination.AuthStart),
      sharedPreferences = InMemorySharedPreferences(),
    )
  }

  private fun protectCheck(): ProtectCheckResource =
    Json.decodeFromString(
      """
      {
        "status": "pending",
        "token": "challenge-token",
        "sdk_url": "https://specter.example.com/challenge"
      }
      """
        .trimIndent()
    )
}
