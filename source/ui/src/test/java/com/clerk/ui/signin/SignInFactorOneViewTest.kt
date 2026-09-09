package com.clerk.ui.signin

import com.clerk.api.Clerk
import com.clerk.api.SignIn
import com.clerk.api.VerificationStatus
import com.clerk.testing.*
import com.clerk.ui.auth.FactorSelection
import com.clerk.ui.core.common.StrategyKeys
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SignInFactorOneViewTest {
  private val clerk = mockk<Clerk>(relaxed = true)

  @Before fun setUp() {}

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun resolveFirstFactorShouldKeepSelectedEmailCodeWhenEmailLinkIsSupported() {
    every { clerk.signIn } returns
      signIn(
        id = "sign_in_123",
        identifier = null,
        supportedFirstFactors =
          listOf(
            FactorSelection(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123"),
            FactorSelection(strategy = StrategyKeys.EMAIL_LINK, emailAddressId = "email_123"),
          ),
      )

    val resolved =
      resolveFirstFactor(
        clerk,
        FactorSelection(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123"),
      )

    assertEquals(StrategyKeys.EMAIL_CODE, resolved.strategy)
  }

  @Test
  fun resolveFirstFactorShouldKeepFallbackWhenEmailLinkIsNotSupported() {
    every { clerk.signIn } returns
      signIn(
        id = "sign_in_123",
        identifier = null,
        supportedFirstFactors =
          listOf(FactorSelection(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123")),
      )

    val fallback = FactorSelection(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123")
    val resolved = resolveFirstFactor(clerk, fallback)

    assertEquals(fallback, resolved)
  }

  @Test
  fun resolveFirstFactorShouldKeepPreparedEmailCodeSelectionForEmailIdentifier() {
    every { clerk.signIn } returns
      signIn(
        id = "sign_in_123",
        identifier = "sam@clerk.dev",
        supportedFirstFactors =
          listOf(
            FactorSelection(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123"),
            FactorSelection(strategy = StrategyKeys.EMAIL_LINK, emailAddressId = "email_123"),
          ),
        firstFactorVerification =
          mockVerification(
            status = VerificationStatus.Unverified,
            strategy = StrategyKeys.EMAIL_CODE,
          ),
      )

    val resolved =
      resolveFirstFactor(
        clerk,
        FactorSelection(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123"),
      )

    assertEquals(StrategyKeys.EMAIL_CODE, resolved.strategy)
  }

  @Test
  fun resolveFirstFactorShouldKeepSupportedFallbackWhenPasswordIsPrepared() {
    val emailCodeFactor =
      FactorSelection(
        strategy = StrategyKeys.EMAIL_CODE,
        emailAddressId = "email_123",
        safeIdentifier = "sam@clerk.dev",
      )
    every { clerk.signIn } returns
      signIn(
        id = "sign_in_123",
        identifier = "sam@clerk.dev",
        supportedFirstFactors =
          listOf(FactorSelection(strategy = StrategyKeys.PASSWORD), emailCodeFactor),
        firstFactorVerification =
          mockVerification(
            status = VerificationStatus.Unverified,
            strategy = StrategyKeys.PASSWORD,
          ),
      )

    val resolved = resolveFirstFactor(clerk, emailCodeFactor)

    assertEquals(emailCodeFactor, resolved)
  }

  @Test
  fun resolveFirstFactorShouldKeepExplicitPasswordWhenEmailLinkIsSupported() {
    val passwordFactor = FactorSelection(strategy = StrategyKeys.PASSWORD)
    every { clerk.signIn } returns
      signIn(
        id = "sign_in_123",
        identifier = "sam@clerk.dev",
        supportedFirstFactors =
          listOf(
            passwordFactor,
            FactorSelection(
              strategy = StrategyKeys.EMAIL_CODE,
              emailAddressId = "email_123",
              safeIdentifier = "sam@clerk.dev",
            ),
            FactorSelection(
              strategy = StrategyKeys.EMAIL_LINK,
              emailAddressId = "email_123",
              safeIdentifier = "sam@clerk.dev",
            ),
          ),
      )

    val resolved = resolveFirstFactor(clerk, passwordFactor)

    assertEquals(passwordFactor, resolved)
  }

  @Test
  fun resolveFirstFactorShouldKeepResetPasswordEmailCodeWhenEmailLinkIsSupported() {
    val resetFactor =
      FactorSelection(
        strategy = StrategyKeys.RESET_PASSWORD_EMAIL_CODE,
        emailAddressId = "email_123",
        safeIdentifier = "sam@clerk.dev",
      )
    every { clerk.signIn } returns
      signIn(
        id = "sign_in_123",
        identifier = "sam@clerk.dev",
        supportedFirstFactors =
          listOf(
            resetFactor,
            FactorSelection(strategy = StrategyKeys.EMAIL_LINK, emailAddressId = "email_123"),
            FactorSelection(strategy = StrategyKeys.PASSWORD),
          ),
      )

    val resolved = resolveFirstFactor(clerk, resetFactor)

    assertEquals(resetFactor, resolved)
  }

  private fun signIn(
    id: String,
    identifier: String?,
    supportedFirstFactors: List<FactorSelection>,
    firstFactorVerification: com.clerk.api.Verification = mockVerification(),
  ): SignIn {
    val signIn = mockSignIn()
    every { signIn.id } returns id
    every { signIn.identifier } returns identifier
    every { signIn.supportedFirstFactors } returns supportedFirstFactors.map(::firstFactor)
    every { signIn.firstFactorVerification } returns firstFactorVerification
    return signIn
  }
}
