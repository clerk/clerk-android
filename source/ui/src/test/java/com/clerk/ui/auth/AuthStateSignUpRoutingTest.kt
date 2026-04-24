package com.clerk.ui.auth

import android.content.Context
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.test.core.app.ApplicationProvider
import com.clerk.api.Constants
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.signup.SignUp
import com.clerk.ui.signup.collectfield.CollectField
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthStateSignUpRoutingTest {

  private lateinit var context: Context
  private val backStack = mockk<NavBackStack<NavKey>>(relaxed = true)
  private lateinit var authState: AuthState

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    preferences().edit().clear().commit()
    authState =
      AuthState(
        mode = AuthMode.SignInOrUp,
        backStack = backStack,
        sharedPreferences = preferences(),
      )
  }

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

  @Test
  fun setToStepForStatusCollectsRequiredFieldsBeforeStartingEmailLinkVerification() {
    val signUp =
      signUp(
        verifications =
          mapOf(
            "email_address" to
              Verification(
                status = Verification.Status.UNVERIFIED,
                strategy = Constants.Strategy.EMAIL_LINK,
              )
          ),
        missingFields = listOf("password"),
      )

    authState.setToStepForStatus(signUp) {}

    verify(exactly = 1) {
      backStack.add(AuthDestination.SignUpCollectField(CollectField.Password))
    }
    verify(exactly = 0) {
      backStack.add(AuthDestination.SignUpEmailLink(emailAddress = "sam@clerk.dev"))
    }
  }

  private fun signUp(
    verifications: Map<String, Verification?>,
    missingFields: List<String> = emptyList(),
  ): SignUp {
    every { backStack.add(any()) } returns true
    return SignUp(
      id = "sign_up_123",
      status = SignUp.Status.MISSING_REQUIREMENTS,
      requiredFields = listOf("email_address", "password"),
      optionalFields = emptyList(),
      missingFields = missingFields,
      unverifiedFields = listOf("email_address"),
      verifications = verifications,
      emailAddress = "sam@clerk.dev",
      passwordEnabled = false,
    )
  }

  private fun preferences() =
    context.getSharedPreferences(
      Constants.Storage.CLERK_PREFERENCES_FILE_NAME,
      Context.MODE_PRIVATE,
    )
}
