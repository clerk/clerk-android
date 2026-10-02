package com.clerk.api.signin

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.serialization.ClerkResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SignInSendCodeTest {

  private val signInApi = mockk<SignInApi>()

  @Before
  fun setup() {
    mockkObject(ClerkApi)
    every { ClerkApi.signIn } returns signInApi
    coEvery { signInApi.prepareSignInFirstFactor(any(), any()) } returns
      ClerkResult.success(SignIn(id = SIGN_IN_ID))
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `sendCode with email sends to the factor for that email`() = runTest {
    val signIn =
      signInWith(
        Factor(strategy = "email_code", emailAddressId = "email_first", safeIdentifier = FIRST),
        Factor(strategy = "email_code", emailAddressId = "email_second", safeIdentifier = SECOND),
      )

    val result = signIn.sendCode { email = SECOND }

    assertTrue(result is ClerkResult.Success)
    coVerify(exactly = 1) {
      signInApi.prepareSignInFirstFactor(
        SIGN_IN_ID,
        mapOf("strategy" to "email_code", "email_address_id" to "email_second"),
      )
    }
  }

  @Test
  fun `sendCode with email matches addresses case-insensitively`() = runTest {
    val signIn =
      signInWith(
        Factor(strategy = "email_code", emailAddressId = "email_first", safeIdentifier = FIRST),
        Factor(strategy = "email_code", emailAddressId = "email_second", safeIdentifier = SECOND),
      )

    signIn.sendCode { email = SECOND.uppercase() }

    coVerify(exactly = 1) {
      signInApi.prepareSignInFirstFactor(
        SIGN_IN_ID,
        mapOf("strategy" to "email_code", "email_address_id" to "email_second"),
      )
    }
  }

  @Test
  fun `sendCode with phone sends to the factor for that phone number`() = runTest {
    val signIn =
      signInWith(
        Factor(
          strategy = "phone_code",
          phoneNumberId = "phone_first",
          safeIdentifier = "+15550001",
        ),
        Factor(
          strategy = "phone_code",
          phoneNumberId = "phone_second",
          safeIdentifier = "+15550002",
        ),
      )

    signIn.sendCode { phone = "+1 555-0002" }

    coVerify(exactly = 1) {
      signInApi.prepareSignInFirstFactor(
        SIGN_IN_ID,
        mapOf("strategy" to "phone_code", "phone_number_id" to "phone_second"),
      )
    }
  }

  @Test
  fun `sendCode uses the only email factor when its identifier is masked`() = runTest {
    val signIn =
      signInWith(
        Factor(strategy = "email_code", emailAddressId = "email_123", safeIdentifier = "u***@x.com")
      )

    signIn.sendCode { email = "user@x.com" }

    coVerify(exactly = 1) {
      signInApi.prepareSignInFirstFactor(
        SIGN_IN_ID,
        mapOf("strategy" to "email_code", "email_address_id" to "email_123"),
      )
    }
  }

  @Test
  fun `sendCode fails without calling the API when no factor matches the email`() = runTest {
    val signIn =
      signInWith(
        Factor(strategy = "email_code", emailAddressId = "email_first", safeIdentifier = FIRST)
      )

    val result = signIn.sendCode { email = SECOND }

    assertTrue(result is ClerkResult.Failure)
    assertEquals(
      "first_factor_strategy_not_supported",
      (result as ClerkResult.Failure).error?.errors?.single()?.code,
    )
    coVerify(exactly = 0) { signInApi.prepareSignInFirstFactor(any(), any()) }
  }

  @Test
  fun `sendResetPasswordCode with email sends to the reset factor for that email`() = runTest {
    val signIn =
      signInWith(
        Factor(
          strategy = "reset_password_email_code",
          emailAddressId = "email_first",
          safeIdentifier = FIRST,
        ),
        Factor(
          strategy = "reset_password_email_code",
          emailAddressId = "email_second",
          safeIdentifier = SECOND,
        ),
      )

    signIn.sendResetPasswordCode { email = SECOND }

    coVerify(exactly = 1) {
      signInApi.prepareSignInFirstFactor(
        SIGN_IN_ID,
        mapOf("strategy" to "reset_password_email_code", "email_address_id" to "email_second"),
      )
    }
  }

  private fun signInWith(vararg factors: Factor): SignIn =
    SignIn(
      id = SIGN_IN_ID,
      status = SignIn.Status.NEEDS_FIRST_FACTOR,
      supportedFirstFactors = factors.toList(),
    )

  private companion object {
    const val SIGN_IN_ID = "sign_in_123"
    const val FIRST = "first@example.com"
    const val SECOND = "second@example.com"
  }
}
