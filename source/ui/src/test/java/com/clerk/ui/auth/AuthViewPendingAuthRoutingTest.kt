package com.clerk.ui.auth

import android.content.Context
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.test.core.app.ApplicationProvider
import com.clerk.api.Constants
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthViewPendingAuthRoutingTest {

  private lateinit var context: Context
  private val backStack = mockk<NavBackStack<NavKey>>(relaxed = true)
  private lateinit var authState: AuthState

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    preferences().edit().clear().commit()
    every { backStack.add(any()) } returns true
    authState =
      AuthState(
        mode = AuthMode.SignInOrUp,
        backStack = backStack,
        sharedPreferences = preferences(),
      )
  }

  @Test
  fun `resumeInProgressAuthAttempt routes sign in to second factor from auth start`() {
    val factor = Factor(strategy = Constants.Strategy.TOTP)
    val signIn =
      SignIn(
        id = "sign_in_123",
        status = SignIn.Status.NEEDS_SECOND_FACTOR,
        supportedSecondFactors = listOf(factor),
      )

    resumeInProgressAuthAttempt(
      authState = authState,
      top = AuthDestination.AuthStart,
      signIn = signIn,
      signUp = null,
      onAuthComplete = {},
    )

    verify(exactly = 1) { backStack.add(AuthDestination.SignInFactorTwo(factor = factor)) }
  }

  @Test
  fun `resumeInProgressAuthAttempt does not reroute when auth stack is already past root`() {
    val factor = Factor(strategy = Constants.Strategy.TOTP)
    val signIn =
      SignIn(
        id = "sign_in_123",
        status = SignIn.Status.NEEDS_SECOND_FACTOR,
        supportedSecondFactors = listOf(factor),
      )

    resumeInProgressAuthAttempt(
      authState = authState,
      top = AuthDestination.SignInFactorTwo(factor),
      signIn = signIn,
      signUp = null,
      onAuthComplete = {},
    )

    verify(exactly = 0) { backStack.add(any()) }
  }

  @Test
  fun `resumeInProgressAuthAttempt does not reroute after identifier edit returns to root`() {
    val factor = Factor(strategy = Constants.Strategy.TOTP)
    val signIn =
      SignIn(
        id = "sign_in_123",
        status = SignIn.Status.NEEDS_SECOND_FACTOR,
        supportedSecondFactors = listOf(factor),
      )

    authState.navigateToAuthStartForIdentifierEdit()

    resumeInProgressAuthAttempt(
      authState = authState,
      top = AuthDestination.AuthStart,
      signIn = signIn,
      signUp = null,
      onAuthComplete = {},
    )

    verify(exactly = 0) { backStack.add(any()) }
  }

  @Test
  fun `resumeInProgressAuthAttempt completes auth after identifier edit returns to root`() {
    val signIn = SignIn(id = "sign_in_123", status = SignIn.Status.COMPLETE)
    var completed = false

    authState.navigateToAuthStartForIdentifierEdit()

    resumeInProgressAuthAttempt(
      authState = authState,
      top = AuthDestination.AuthStart,
      signIn = signIn,
      signUp = null,
      onAuthComplete = { completed = true },
    )

    assertTrue(completed)
    verify(exactly = 0) { backStack.add(any()) }
  }

  @Test
  fun `navigateBack from sign up email link does not reroute to pending verification`() {
    val realBackStack =
      NavBackStack<NavKey>(
        AuthDestination.AuthStart,
        AuthDestination.SignUpEmailLink(emailAddress = "sam@clerk.dev"),
      )
    val realAuthState =
      AuthState(
        mode = AuthMode.SignInOrUp,
        backStack = realBackStack,
        sharedPreferences = preferences(),
      )
    val signUp =
      SignUp(
        id = "sign_up_123",
        status = SignUp.Status.MISSING_REQUIREMENTS,
        requiredFields = listOf("email_address", "password"),
        optionalFields = emptyList(),
        missingFields = emptyList(),
        unverifiedFields = listOf("email_address"),
        verifications =
          mapOf(
            "email_address" to
              Verification(
                status = Verification.Status.UNVERIFIED,
                strategy = Constants.Strategy.EMAIL_LINK,
              )
          ),
        emailAddress = "sam@clerk.dev",
        passwordEnabled = false,
      )

    realAuthState.navigateBack()
    resumeInProgressAuthAttempt(
      authState = realAuthState,
      top = realBackStack.lastOrNull(),
      signIn = null,
      signUp = signUp,
      onAuthComplete = {},
    )

    assertEquals(listOf(AuthDestination.AuthStart), realBackStack.toList())
  }

  private fun preferences() =
    context.getSharedPreferences(
      Constants.Storage.CLERK_PREFERENCES_FILE_NAME,
      Context.MODE_PRIVATE,
    )
}
