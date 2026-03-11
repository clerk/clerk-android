package com.clerk.ui.signin

import com.clerk.api.Clerk
import com.clerk.api.auth.Auth
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.signin.SignIn
import com.clerk.ui.core.common.StrategyKeys
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
  fun resolveFirstFactorShouldPreferEmailLinkWhenFallbackEmailCodeHasEmailAddressId() {
    every { mockAuth.currentSignIn } returns
      SignIn(
        id = "sign_in_123",
        identifier = null,
        supportedFirstFactors =
          listOf(
            Factor(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123"),
            Factor(strategy = StrategyKeys.EMAIL_LINK, emailAddressId = "email_123"),
          ),
      )

    val resolved =
      resolveFirstFactor(Factor(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123"))

    assertEquals(StrategyKeys.EMAIL_LINK, resolved.strategy)
  }

  @Test
  fun resolveFirstFactorShouldKeepFallbackWhenEmailLinkIsNotSupported() {
    every { mockAuth.currentSignIn } returns
      SignIn(
        id = "sign_in_123",
        identifier = null,
        supportedFirstFactors =
          listOf(Factor(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123")),
      )

    val fallback = Factor(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123")
    val resolved = resolveFirstFactor(fallback)

    assertEquals(fallback, resolved)
  }
}
