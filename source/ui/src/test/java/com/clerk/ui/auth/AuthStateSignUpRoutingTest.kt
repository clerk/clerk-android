package com.clerk.ui.auth

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.clerk.api.Constants
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.signup.SignUp
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class AuthStateSignUpRoutingTest {

  private val backStack = mockk<NavBackStack<NavKey>>(relaxed = true)
  private val authState = AuthState(mode = AuthMode.SignInOrUp, backStack = backStack)

  @Test
  fun setToStepForStatusRoutesToSignUpEmailLinkWhenEmailVerificationStrategyIsEmailLink() {
    val signUp =
      signUp(
        verifications =
          mapOf(
            "email_address" to
              Verification(
                status = Verification.Status.UNVERIFIED,
                strategy = Constants.Strategy.EMAIL_LINK,
              )
          )
      )

    authState.setToStepForStatus(signUp) {}

    verify(exactly = 1) {
      backStack.add(AuthDestination.SignUpEmailLink(emailAddress = "sam@clerk.dev"))
    }
  }

  @Test
  fun setToStepForStatusRoutesToSignUpCodeWhenEmailVerificationStrategyIsEmailCode() {
    val signUp =
      signUp(
        verifications =
          mapOf(
            "email_address" to
              Verification(
                status = Verification.Status.UNVERIFIED,
                strategy = Constants.Strategy.EMAIL_CODE,
              )
          )
      )

    authState.setToStepForStatus(signUp) {}

    verify(exactly = 1) {
      backStack.add(
        AuthDestination.SignUpCode(
          field = com.clerk.ui.signup.code.SignUpCodeField.Email("sam@clerk.dev")
        )
      )
    }
  }

  private fun signUp(verifications: Map<String, Verification?>): SignUp {
    every { backStack.add(any()) } returns true
    return SignUp(
      id = "sign_up_123",
      status = SignUp.Status.MISSING_REQUIREMENTS,
      requiredFields = listOf("email_address"),
      optionalFields = emptyList(),
      missingFields = emptyList(),
      unverifiedFields = listOf("email_address"),
      verifications = verifications,
      emailAddress = "sam@clerk.dev",
      passwordEnabled = false,
    )
  }
}
