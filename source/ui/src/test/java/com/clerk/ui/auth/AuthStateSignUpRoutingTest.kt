package com.clerk.ui.auth

import android.content.Context
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.test.core.app.ApplicationProvider
import com.clerk.api.*
import com.clerk.api.SignUp
import com.clerk.testing.mockClerk
import com.clerk.ui.signup.collectfield.CollectField
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
        clerk = mockClerk(),
        mode = AuthMode.SignInOrUp,
        backStack = backStack,
        sharedPreferences = preferences(),
      )
  }

  @Test
  fun authStateDefaultsToSignInOrUpMode() {
    val defaultAuthState =
      AuthState(clerk = mockClerk(), backStack = backStack, sharedPreferences = preferences())

    assertEquals(AuthMode.SignInOrUp, defaultAuthState.mode)
  }

  @Test
  fun authStateKeepsExplicitMode() {
    val signUpAuthState =
      AuthState(
        clerk = mockClerk(),
        mode = AuthMode.SignUp,
        backStack = backStack,
        sharedPreferences = preferences(),
      )

    assertEquals(AuthMode.SignUp, signUpAuthState.mode)
  }

  @Test
  fun setToStepForStatusRoutesToSignUpEmailLinkWhenEmailVerificationStrategyIsEmailLink() =
    runTest {
      val signUp =
        signUp(
          verifications =
            mapOf(
              "email_address" to
                verification(
                  status = VerificationStatus.Unverified,
                  strategy = "email_link",
                )
            )
        )

      authState.setToStepForStatus(signUp) {}

      verify(exactly = 1) {
        backStack.add(AuthDestination.SignUpEmailLink(emailAddress = "sam@clerk.dev"))
      }
    }

  @Test
  fun setToStepForStatusRoutesToSignUpCodeWhenEmailVerificationStrategyIsEmailCode() = runTest {
    val signUp =
      signUp(
        verifications =
          mapOf(
            "email_address" to
              verification(
                status = VerificationStatus.Unverified,
                strategy = "email_code",
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
  fun setToStepForStatusCollectsRequiredFieldsBeforeStartingEmailLinkVerification() = runTest {
    val signUp =
      signUp(
        verifications =
          mapOf(
            "email_address" to
              verification(
                status = VerificationStatus.Unverified,
                strategy = "email_link",
              )
          ),
        missingFields = listOf("password"),
      )

    authState.setToStepForStatus(signUp) {}

    verify(exactly = 1) { backStack.add(AuthDestination.SignUpCollectField(CollectField.Password)) }
    verify(exactly = 0) {
      backStack.add(AuthDestination.SignUpEmailLink(emailAddress = "sam@clerk.dev"))
    }
  }

  @Test
  fun setToStepForStatusDoesNotCollectOptionalMissingFields() = runTest {
    val signUp =
      signUp(
        verifications = emptyMap(),
        requiredFields = listOf("email_address"),
        optionalFields = listOf("first_name", "last_name"),
        missingFields = listOf("first_name", "last_name"),
        unverifiedFields = emptyList(),
      )

    authState.setToStepForStatus(signUp) {}

    verify(exactly = 0) { backStack.add(any()) }
  }

  private fun signUp(
    verifications: Map<String, SignUpVerification?>,
    missingFields: List<String> = emptyList(),
    requiredFields: List<String> = listOf("email_address", "password"),
    optionalFields: List<String> = emptyList(),
    unverifiedFields: List<String> = listOf("email_address"),
  ): SignUp {
    every { backStack.add(any()) } returns true
    val signUp = mockk<SignUp>(relaxed = true)
    every { signUp.status } returns SignUpStatus.MissingRequirements
    every { signUp.createdSessionId } returns null
    fun field(value: String) =
      SignUpField.fromJson(kotlinx.serialization.json.JsonPrimitive(value), mockk(relaxed = true))
    every { signUp.requiredFields } returns requiredFields.map(::field)
    every { signUp.optionalFields } returns optionalFields.map(::field)
    every { signUp.missingFields } returns missingFields.map(::field)
    every { signUp.unverifiedFields } returns
      unverifiedFields.map {
        SignUpIdentificationField.fromJson(
          kotlinx.serialization.json.JsonPrimitive(it),
          mockk(relaxed = true),
        )
      }
    every { signUp.verifications.emailAddress } returns
      (verifications["email_address"] ?: verification(VerificationStatus.Unverified, null))
    every { signUp.emailAddress } returns "sam@clerk.dev"
    return signUp
  }

  private fun verification(status: VerificationStatus, strategy: String?): SignUpVerification =
    mockk(relaxed = true) {
      every { this@mockk.status } returns status
      every { this@mockk.strategy } returns strategy
    }

  private fun preferences() =
    context.getSharedPreferences(
      "clerk_preferences",
      Context.MODE_PRIVATE,
    )
}
