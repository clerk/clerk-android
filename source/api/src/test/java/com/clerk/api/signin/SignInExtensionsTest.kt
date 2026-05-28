package com.clerk.api.signin

import com.clerk.api.Clerk
import com.clerk.api.network.model.environment.DisplayConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.PreferredSignInStrategy
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.verification.Verification
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SignInExtensionsTest {
  private lateinit var environment: Environment
  private lateinit var displayConfig: DisplayConfig

  @Before
  fun setup() {
    environment = mockk(relaxed = true)
    displayConfig = mockk(relaxed = true)
    every { environment.displayConfig } returns displayConfig
    Clerk.environment = environment
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `startingFirstFactor prefers email_link for email identifier when password is preferred`() {
    every { displayConfig.preferredSignInStrategy } returns PreferredSignInStrategy.PASSWORD
    val signIn =
      SignIn(
        id = "sign_in_123",
        identifier = "user@example.com",
        supportedFirstFactors =
          listOf(
            Factor(strategy = "password", safeIdentifier = "user@example.com"),
            Factor(strategy = "email_code", emailAddressId = "email_123"),
            Factor(strategy = "email_link", emailAddressId = "email_123"),
          ),
      )

    val factor = signIn.startingFirstFactor

    assertEquals("email_link", factor?.strategy)
  }

  @Test
  fun `startingFirstFactor prefers email_link for email identifier when otp is preferred`() {
    every { displayConfig.preferredSignInStrategy } returns PreferredSignInStrategy.OTP
    val signIn =
      SignIn(
        id = "sign_in_123",
        identifier = "user@example.com",
        supportedFirstFactors =
          listOf(
            Factor(strategy = "email_code", emailAddressId = "email_123"),
            Factor(strategy = "email_link", emailAddressId = "email_123"),
          ),
      )

    val factor = signIn.startingFirstFactor

    assertEquals("email_link", factor?.strategy)
  }

  @Test
  fun `startingFirstFactor does not force email_link for non-email identifiers`() {
    every { displayConfig.preferredSignInStrategy } returns PreferredSignInStrategy.OTP
    val signIn =
      SignIn(
        id = "sign_in_123",
        identifier = "username_123",
        supportedFirstFactors =
          listOf(
            Factor(strategy = "passkey"),
            Factor(strategy = "email_link", emailAddressId = "email_123"),
          ),
      )

    val factor = signIn.startingFirstFactor

    assertEquals("passkey", factor?.strategy)
  }

  @Test
  fun `startingFirstFactor chooses email_link matching the email factor identity`() {
    every { displayConfig.preferredSignInStrategy } returns PreferredSignInStrategy.PASSWORD
    val signIn =
      SignIn(
        id = "sign_in_123",
        identifier = "user@example.com",
        supportedFirstFactors =
          listOf(
            Factor(strategy = "email_link", emailAddressId = "email_other"),
            Factor(
              strategy = "email_code",
              emailAddressId = "email_123",
              safeIdentifier = "user@example.com",
            ),
            Factor(strategy = "email_link", emailAddressId = "email_123"),
            Factor(strategy = "password", safeIdentifier = "user@example.com"),
          ),
      )

    val factor = signIn.startingFirstFactor

    assertEquals("email_link", factor?.strategy)
    assertEquals("email_123", factor?.emailAddressId)
  }

  @Test
  fun `startingFirstFactor does not use unrelated email_link for username sign-ins`() {
    every { displayConfig.preferredSignInStrategy } returns PreferredSignInStrategy.PASSWORD
    val signIn =
      SignIn(
        id = "sign_in_123",
        identifier = "username_123",
        supportedFirstFactors =
          listOf(
            Factor(strategy = "email_link", emailAddressId = "email_123"),
            Factor(strategy = "password", safeIdentifier = "username_123"),
          ),
      )

    val factor = signIn.startingFirstFactor

    assertEquals("password", factor?.strategy)
  }

  @Test
  fun `startingFirstFactor matches prepared factor by identifier when strategies repeat`() {
    every { displayConfig.preferredSignInStrategy } returns PreferredSignInStrategy.OTP
    val signIn =
      SignIn(
        id = "sign_in_123",
        identifier = "second@example.com",
        supportedFirstFactors =
          listOf(
            Factor(
              strategy = "email_code",
              emailAddressId = "email_first",
              safeIdentifier = "first@example.com",
            ),
            Factor(
              strategy = "email_code",
              emailAddressId = "email_second",
              safeIdentifier = "second@example.com",
            ),
          ),
        firstFactorVerification =
          Verification(status = Verification.Status.UNVERIFIED, strategy = "email_code"),
      )

    val factor = signIn.startingFirstFactor

    assertEquals("email_second", factor?.emailAddressId)
  }

  @Test
  fun `startingFirstFactor returns prepared email_code when verification is already prepared`() {
    every { displayConfig.preferredSignInStrategy } returns PreferredSignInStrategy.OTP
    val signIn =
      SignIn(
        id = "sign_in_123",
        identifier = "user@example.com",
        supportedFirstFactors =
          listOf(
            Factor(strategy = "email_code", emailAddressId = "email_123"),
            Factor(strategy = "email_link", emailAddressId = "email_123"),
          ),
        firstFactorVerification =
          Verification(status = Verification.Status.UNVERIFIED, strategy = "email_code"),
      )

    val factor = signIn.startingFirstFactor

    assertEquals("email_code", factor?.strategy)
  }
}
