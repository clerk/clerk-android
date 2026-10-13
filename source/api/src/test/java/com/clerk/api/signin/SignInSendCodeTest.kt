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
  fun `sendCode rejects the only email factor when its mask contradicts the email`() = runTest {
    val signIn =
      signInWith(
        Factor(strategy = "email_code", emailAddressId = "email_123", safeIdentifier = "u***@x.com")
      )

    val result = signIn.sendCode { email = "other@y.com" }

    assertTrue(result is ClerkResult.Failure)
    coVerify(exactly = 0) { signInApi.prepareSignInFirstFactor(any(), any()) }
  }

  @Test
  fun `sendCode uses the only phone factor when its mask agrees with the phone`() = runTest {
    val signIn =
      signInWith(
        Factor(
          strategy = "phone_code",
          phoneNumberId = "phone_123",
          safeIdentifier = "+1******0002",
        )
      )

    signIn.sendCode { phone = "+1 (555) 000-0002" }

    coVerify(exactly = 1) {
      signInApi.prepareSignInFirstFactor(
        SIGN_IN_ID,
        mapOf("strategy" to "phone_code", "phone_number_id" to "phone_123"),
      )
    }
  }

  @Test
  fun `sendCode rejects the only phone factor when its mask contradicts the phone`() = runTest {
    val signIn =
      signInWith(
        Factor(
          strategy = "phone_code",
          phoneNumberId = "phone_123",
          safeIdentifier = "+1******0002",
        )
      )

    val result = signIn.sendCode { phone = "+1 (555) 000-0009" }

    assertTrue(result is ClerkResult.Failure)
    coVerify(exactly = 0) { signInApi.prepareSignInFirstFactor(any(), any()) }
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

  @Test
  fun `sendCode picks the masked email factor whose visible characters agree`() = runTest {
    val signIn =
      signInWith(
        Factor(strategy = "email_code", emailAddressId = "email_a", safeIdentifier = "a***@x.com"),
        Factor(strategy = "email_code", emailAddressId = "email_b", safeIdentifier = "b***@y.com"),
      )

    signIn.sendCode { email = "bob@y.com" }

    coVerify(exactly = 1) {
      signInApi.prepareSignInFirstFactor(
        SIGN_IN_ID,
        mapOf("strategy" to "email_code", "email_address_id" to "email_b"),
      )
    }
  }

  @Test
  fun `sendCode refuses to guess between masked email factors that both agree`() = runTest {
    val signIn =
      signInWith(
        Factor(strategy = "email_code", emailAddressId = "email_a", safeIdentifier = "b***@y.com"),
        Factor(strategy = "email_code", emailAddressId = "email_b", safeIdentifier = "b***@y.com"),
      )

    val result = signIn.sendCode { email = "bob@y.com" }

    assertTrue(result is ClerkResult.Failure)
    coVerify(exactly = 0) { signInApi.prepareSignInFirstFactor(any(), any()) }
  }

  @Test
  fun `sendCode fails without calling the API when the sign-in is not awaiting a first factor`() =
    runTest {
      val signIn =
        signInWith(
            Factor(strategy = "email_code", emailAddressId = "email_first", safeIdentifier = FIRST)
          )
          .copy(status = SignIn.Status.NEEDS_SECOND_FACTOR)

      val result = signIn.sendCode { email = FIRST }

      assertEquals(
        "sign_in_status_invalid",
        (result as ClerkResult.Failure).error?.errors?.single()?.code,
      )
      coVerify(exactly = 0) { signInApi.prepareSignInFirstFactor(any(), any()) }
    }

  @Test
  fun `sendResetPasswordCode falls back to the email code factor when no reset factor exists`() =
    runTest {
      val signIn =
        signInWith(
          Factor(strategy = "email_code", emailAddressId = "email_first", safeIdentifier = FIRST)
        )

      signIn.sendResetPasswordCode { email = FIRST }

      coVerify(exactly = 1) {
        signInApi.prepareSignInFirstFactor(
          SIGN_IN_ID,
          mapOf("strategy" to "reset_password_email_code", "email_address_id" to "email_first"),
        )
      }
    }

  @Test
  fun `sendResetPasswordCode with phone sends to the reset factor for that phone`() = runTest {
    val signIn =
      signInWith(
        Factor(
          strategy = "reset_password_phone_code",
          phoneNumberId = "phone_first",
          safeIdentifier = "+15550001",
        ),
        Factor(
          strategy = "reset_password_phone_code",
          phoneNumberId = "phone_second",
          safeIdentifier = "+15550002",
        ),
      )

    signIn.sendResetPasswordCode { phone = "+1 555-0002" }

    coVerify(exactly = 1) {
      signInApi.prepareSignInFirstFactor(
        SIGN_IN_ID,
        mapOf("strategy" to "reset_password_phone_code", "phone_number_id" to "phone_second"),
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
