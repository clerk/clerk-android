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

class SignInPrepareSecondFactorTest {

  private val signInApi = mockk<SignInApi>()
  private val signIn =
    SignIn(
      id = SIGN_IN_ID,
      status = SignIn.Status.NEEDS_SECOND_FACTOR,
      supportedSecondFactors =
        listOf(
          Factor(strategy = "phone_code", phoneNumberId = "phone_123"),
          Factor(strategy = "email_code", emailAddressId = "email_123"),
        ),
    )

  @Before
  fun setup() {
    mockkObject(ClerkApi)
    every { ClerkApi.signIn } returns signInApi
    coEvery { signInApi.prepareSecondFactor(any(), any()) } returns ClerkResult.success(signIn)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `prepareSecondFactor with an email address id prepares email even when phone is supported`() =
    runTest {
      signIn.prepareSecondFactor(emailAddressId = "email_123")

      coVerify(exactly = 1) {
        signInApi.prepareSecondFactor(
          SIGN_IN_ID,
          mapOf("strategy" to "email_code", "email_address_id" to "email_123"),
        )
      }
    }

  @Test
  fun `prepareSecondFactor without ids prefers the phone factor`() = runTest {
    signIn.prepareSecondFactor()

    coVerify(exactly = 1) {
      signInApi.prepareSecondFactor(
        SIGN_IN_ID,
        mapOf("strategy" to "phone_code", "phone_number_id" to "phone_123"),
      )
    }
  }

  @Test
  fun `sendMfaPhoneCode checks the sign-in status like prepareSecondFactor`() = runTest {
    val firstFactorSignIn = signIn.copy(status = SignIn.Status.NEEDS_FIRST_FACTOR)

    val result = firstFactorSignIn.sendMfaPhoneCode()

    assertTrue(result is ClerkResult.Failure)
    assertEquals(
      "sign_in_status_invalid",
      (result as ClerkResult.Failure).error?.errors?.single()?.code,
    )
    coVerify(exactly = 0) { signInApi.prepareSecondFactor(any(), any()) }
  }

  @Test
  fun `sendMfaEmailCode prepares the email second factor`() = runTest {
    signIn.sendMfaEmailCode()

    coVerify(exactly = 1) {
      signInApi.prepareSecondFactor(
        SIGN_IN_ID,
        mapOf("strategy" to "email_code", "email_address_id" to "email_123"),
      )
    }
  }

  @Test
  fun `sendMfaPhoneCode and sendMfaEmailCode fail before the second factor stage`() = runTest {
    val firstFactorSignIn = SignIn(id = SIGN_IN_ID, status = SignIn.Status.NEEDS_FIRST_FACTOR)

    for (result in
      listOf(firstFactorSignIn.sendMfaPhoneCode(), firstFactorSignIn.sendMfaEmailCode())) {
      assertEquals(
        "sign_in_status_invalid",
        (result as ClerkResult.Failure).error?.errors?.single()?.code,
      )
    }
    coVerify(exactly = 0) { signInApi.prepareSecondFactor(any(), any()) }
  }

  @Test
  fun `sendMfaEmailCode fails instead of throwing when no email factor is available`() = runTest {
    val phoneOnly =
      SignIn(
        id = SIGN_IN_ID,
        status = SignIn.Status.NEEDS_SECOND_FACTOR,
        supportedSecondFactors =
          listOf(Factor(strategy = "phone_code", phoneNumberId = "phone_123")),
      )

    val result = phoneOnly.sendMfaEmailCode()

    assertEquals(
      "second_factor_strategy_not_supported",
      (result as ClerkResult.Failure).error?.errors?.single()?.code,
    )
    coVerify(exactly = 0) { signInApi.prepareSecondFactor(any(), any()) }
  }

  @Test
  fun `prepareSecondFactor with a phone number id prepares phone even when only email is supported`() =
    runTest {
      val emailOnly =
        SignIn(
          id = SIGN_IN_ID,
          status = SignIn.Status.NEEDS_SECOND_FACTOR,
          supportedSecondFactors =
            listOf(Factor(strategy = "email_code", emailAddressId = "email_123")),
        )

      emailOnly.prepareSecondFactor(phoneNumberId = "phone_456")

      coVerify(exactly = 1) {
        signInApi.prepareSecondFactor(
          SIGN_IN_ID,
          mapOf("strategy" to "phone_code", "phone_number_id" to "phone_456"),
        )
      }
    }

  @Test
  fun `prepareSecondFactor without ids fails instead of throwing when no code factor exists`() =
    runTest {
      val result =
        SignIn(id = SIGN_IN_ID, status = SignIn.Status.NEEDS_SECOND_FACTOR).prepareSecondFactor()

      assertEquals(
        "second_factor_strategy_not_supported",
        (result as ClerkResult.Failure).error?.errors?.single()?.code,
      )
    }

  private companion object {
    const val SIGN_IN_ID = "sign_in_123"
  }
}
