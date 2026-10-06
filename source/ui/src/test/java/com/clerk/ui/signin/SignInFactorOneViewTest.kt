package com.clerk.ui.signin

import com.clerk.api.Clerk
import com.clerk.api.auth.Auth
import com.clerk.api.auth.types.Strategy
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.signin.SignIn
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SignInFactorOneViewTest {

  private val mockAuth = mockk<Auth>(relaxed = true)

  @Before
  fun setUp() {
    mockkObject(Clerk)
    every { Clerk.auth } returns mockAuth
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun resolveFirstFactorShouldKeepSelectedEmailCodeWhenEmailLinkIsSupported() {
    every { mockAuth.currentSignIn } returns
      SignIn(
        id = "sign_in_123",
        identifier = null,
        supportedFirstFactors =
          listOf(
            Factor(strategy = Strategy.EmailCode.value, emailAddressId = "email_123"),
            Factor(strategy = Strategy.EmailLink.value, emailAddressId = "email_123"),
          ),
      )

    val resolved =
      resolveFirstFactor(Factor(strategy = Strategy.EmailCode.value, emailAddressId = "email_123"))

    assertEquals(Strategy.EmailCode, resolved.strategyType)
  }

  @Test
  fun resolveFirstFactorShouldKeepFallbackWhenEmailLinkIsNotSupported() {
    every { mockAuth.currentSignIn } returns
      SignIn(
        id = "sign_in_123",
        identifier = null,
        supportedFirstFactors =
          listOf(Factor(strategy = Strategy.EmailCode.value, emailAddressId = "email_123")),
      )

    val fallback = Factor(strategy = Strategy.EmailCode.value, emailAddressId = "email_123")
    val resolved = resolveFirstFactor(fallback)

    assertEquals(fallback, resolved)
  }

  @Test
  fun resolveFirstFactorShouldKeepPreparedEmailCodeSelectionForEmailIdentifier() {
    every { mockAuth.currentSignIn } returns
      SignIn(
        id = "sign_in_123",
        identifier = "sam@clerk.dev",
        supportedFirstFactors =
          listOf(
            Factor(strategy = Strategy.EmailCode.value, emailAddressId = "email_123"),
            Factor(strategy = Strategy.EmailLink.value, emailAddressId = "email_123"),
          ),
        firstFactorVerification =
          com.clerk.api.network.model.verification.Verification(
            status = com.clerk.api.network.model.verification.Verification.Status.UNVERIFIED,
            strategy = Strategy.EmailCode.value,
          ),
      )

    val resolved =
      resolveFirstFactor(Factor(strategy = Strategy.EmailCode.value, emailAddressId = "email_123"))

    assertEquals(Strategy.EmailCode, resolved.strategyType)
  }

  @Test
  fun resolveFirstFactorShouldKeepSupportedFallbackWhenPasswordIsPrepared() {
    val emailCodeFactor =
      Factor(
        strategy = Strategy.EmailCode.value,
        emailAddressId = "email_123",
        safeIdentifier = "sam@clerk.dev",
      )
    every { mockAuth.currentSignIn } returns
      SignIn(
        id = "sign_in_123",
        identifier = "sam@clerk.dev",
        supportedFirstFactors = listOf(Factor(strategy = Strategy.Password.value), emailCodeFactor),
        firstFactorVerification =
          com.clerk.api.network.model.verification.Verification(
            status = com.clerk.api.network.model.verification.Verification.Status.UNVERIFIED,
            strategy = Strategy.Password.value,
          ),
      )

    val resolved = resolveFirstFactor(emailCodeFactor)

    assertEquals(emailCodeFactor, resolved)
  }

  @Test
  fun resolveFirstFactorShouldKeepExplicitPasswordWhenEmailLinkIsSupported() {
    val passwordFactor = Factor(strategy = Strategy.Password.value)
    every { mockAuth.currentSignIn } returns
      SignIn(
        id = "sign_in_123",
        identifier = "sam@clerk.dev",
        supportedFirstFactors =
          listOf(
            passwordFactor,
            Factor(
              strategy = Strategy.EmailCode.value,
              emailAddressId = "email_123",
              safeIdentifier = "sam@clerk.dev",
            ),
            Factor(
              strategy = Strategy.EmailLink.value,
              emailAddressId = "email_123",
              safeIdentifier = "sam@clerk.dev",
            ),
          ),
      )

    val resolved = resolveFirstFactor(passwordFactor)

    assertEquals(passwordFactor, resolved)
  }

  @Test
  fun resolveFirstFactorShouldSwitchToPreparedFactorWhenFallbackIsNoLongerSupported() {
    val preparedEmailCode =
      Factor(
        strategy = Strategy.EmailCode.value,
        emailAddressId = "email_123",
        safeIdentifier = "sam@clerk.dev",
      )
    every { mockAuth.currentSignIn } returns
      SignIn(
        id = "sign_in_123",
        identifier = "sam@clerk.dev",
        supportedFirstFactors =
          listOf(Factor(strategy = Strategy.Password.value), preparedEmailCode),
        firstFactorVerification =
          com.clerk.api.network.model.verification.Verification(
            status = com.clerk.api.network.model.verification.Verification.Status.UNVERIFIED,
            strategy = Strategy.EmailCode.value,
          ),
      )

    val resolved =
      resolveFirstFactor(Factor(strategy = Strategy.PhoneCode.value, phoneNumberId = "phone_123"))

    assertEquals(preparedEmailCode, resolved)
  }

  @Test
  fun resolveFirstFactorShouldKeepResetPasswordEmailCodeWhenEmailLinkIsSupported() {
    val resetFactor =
      Factor(
        strategy = Strategy.ResetPasswordEmailCode.value,
        emailAddressId = "email_123",
        safeIdentifier = "sam@clerk.dev",
      )
    every { mockAuth.currentSignIn } returns
      SignIn(
        id = "sign_in_123",
        identifier = "sam@clerk.dev",
        supportedFirstFactors =
          listOf(
            resetFactor,
            Factor(strategy = Strategy.EmailLink.value, emailAddressId = "email_123"),
            Factor(strategy = Strategy.Password.value),
          ),
      )

    val resolved = resolveFirstFactor(resetFactor)

    assertEquals(resetFactor, resolved)
  }
}
