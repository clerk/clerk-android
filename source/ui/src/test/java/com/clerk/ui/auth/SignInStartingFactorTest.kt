package com.clerk.ui.auth

import com.clerk.api.*
import com.clerk.testing.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class SignInStartingFactorTest {
  private val clerk = mockClerk()
  private val displayConfig = mockk<DisplayConfig>(relaxed = true)

  init {
    every { clerk.environment.displayConfig } returns displayConfig
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `startingFirstFactor prefers password for email identifier when password is preferred`() {
    every { displayConfig.preferredSignInStrategy } returns PreferredSignInStrategy.Password
    val signIn =
      signIn(
        id = "sign_in_123",
        identifier = "user@example.com",
        supportedFirstFactors =
          listOf(
            FactorSelection(strategy = "password", safeIdentifier = "user@example.com"),
            FactorSelection(strategy = "email_code", emailAddressId = "email_123"),
            FactorSelection(strategy = "email_link", emailAddressId = "email_123"),
          ),
      )

    val factor = signIn.startingFirstFactor(clerk)

    assertEquals("password", factor?.strategy)
  }

  @Test
  fun `startingFirstFactor prefers email_link for email identifier when otp is preferred`() {
    every { displayConfig.preferredSignInStrategy } returns PreferredSignInStrategy.Otp
    val signIn =
      signIn(
        id = "sign_in_123",
        identifier = "user@example.com",
        supportedFirstFactors =
          listOf(
            FactorSelection(strategy = "email_code", emailAddressId = "email_123"),
            FactorSelection(strategy = "email_link", emailAddressId = "email_123"),
          ),
      )

    val factor = signIn.startingFirstFactor(clerk)

    assertEquals("email_link", factor?.strategy)
  }

  @Test
  fun `startingFirstFactor does not force email_link for non-email identifiers`() {
    every { displayConfig.preferredSignInStrategy } returns PreferredSignInStrategy.Otp
    val signIn =
      signIn(
        id = "sign_in_123",
        identifier = "username_123",
        supportedFirstFactors =
          listOf(
            FactorSelection(strategy = "passkey"),
            FactorSelection(strategy = "email_link", emailAddressId = "email_123"),
          ),
      )

    val factor = signIn.startingFirstFactor(clerk)

    assertEquals("passkey", factor?.strategy)
  }

  @Test
  fun `startingFirstFactor chooses matching email_link when preferred password is unavailable`() {
    every { displayConfig.preferredSignInStrategy } returns PreferredSignInStrategy.Password
    val signIn =
      signIn(
        id = "sign_in_123",
        identifier = "user@example.com",
        supportedFirstFactors =
          listOf(
            FactorSelection(strategy = "email_link", emailAddressId = "email_other"),
            FactorSelection(
              strategy = "email_code",
              emailAddressId = "email_123",
              safeIdentifier = "user@example.com",
            ),
            FactorSelection(strategy = "email_link", emailAddressId = "email_123"),
          ),
      )

    val factor = signIn.startingFirstFactor(clerk)

    assertEquals("email_link", factor?.strategy)
    assertEquals("email_123", factor?.emailAddressId)
  }

  @Test
  fun `startingFirstFactor does not use unrelated email_link for username sign-ins`() {
    every { displayConfig.preferredSignInStrategy } returns PreferredSignInStrategy.Password
    val signIn =
      signIn(
        id = "sign_in_123",
        identifier = "username_123",
        supportedFirstFactors =
          listOf(
            FactorSelection(strategy = "email_link", emailAddressId = "email_123"),
            FactorSelection(strategy = "password", safeIdentifier = "username_123"),
          ),
      )

    val factor = signIn.startingFirstFactor(clerk)

    assertEquals("password", factor?.strategy)
  }

  @Test
  fun `startingFirstFactor matches prepared factor by identifier when strategies repeat`() {
    every { displayConfig.preferredSignInStrategy } returns PreferredSignInStrategy.Otp
    val signIn =
      signIn(
        id = "sign_in_123",
        identifier = "second@example.com",
        supportedFirstFactors =
          listOf(
            FactorSelection(
              strategy = "email_code",
              emailAddressId = "email_first",
              safeIdentifier = "first@example.com",
            ),
            FactorSelection(
              strategy = "email_code",
              emailAddressId = "email_second",
              safeIdentifier = "second@example.com",
            ),
          ),
        firstFactorVerification =
          mockVerification(status = VerificationStatus.Unverified, strategy = "email_code"),
      )

    val factor = signIn.startingFirstFactor(clerk)

    assertEquals("email_second", factor?.emailAddressId)
  }

  @Test
  fun `startingFirstFactor uses preferred password over prepared email_code`() {
    every { displayConfig.preferredSignInStrategy } returns PreferredSignInStrategy.Password
    val signIn =
      signIn(
        id = "sign_in_123",
        identifier = "user@example.com",
        supportedFirstFactors =
          listOf(
            FactorSelection(strategy = "password", safeIdentifier = "user@example.com"),
            FactorSelection(strategy = "email_code", emailAddressId = "email_123"),
            FactorSelection(strategy = "email_link", emailAddressId = "email_123"),
          ),
        firstFactorVerification =
          mockVerification(status = VerificationStatus.Unverified, strategy = "email_code"),
      )

    val factor = signIn.startingFirstFactor(clerk)

    assertEquals("password", factor?.strategy)
  }

  private fun signIn(
    id: String,
    identifier: String?,
    supportedFirstFactors: List<FactorSelection>,
    firstFactorVerification: Verification = mockVerification(),
  ): SignIn =
    mockSignIn().also { signIn ->
      every { signIn.id } returns id
      every { signIn.identifier } returns identifier
      every { signIn.supportedFirstFactors } returns supportedFirstFactors.map(::firstFactor)
      every { signIn.firstFactorVerification } returns firstFactorVerification
    }
}
