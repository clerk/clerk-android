package com.clerk.api.signup

import com.clerk.api.Clerk
import com.clerk.api.Constants
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SignUpApi
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.UserSettings
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SignUpEmailVerificationStrategyTest {
  private val mockSignUpApi = mockk<SignUpApi>(relaxed = true)
  private val environment = mockk<Environment>(relaxed = true)
  private val userSettings = mockk<UserSettings>(relaxed = true)

  @Before
  fun setUp() {
    mockkObject(ClerkApi)
    every { ClerkApi.signUp } returns mockSignUpApi
    every { environment.userSettings } returns userSettings
    Clerk.environment = environment
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun emailVerificationStrategyUsesActiveVerificationStrategyWhenPresent() {
    every { userSettings.attributes } returns emptyMap()
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

    assertEquals(Constants.Strategy.EMAIL_LINK, signUp.emailVerificationStrategy)
  }

  @Test
  fun emailVerificationStrategyFallsBackToEnvironmentStrategiesWhenNoActiveStrategyExists() {
    every { userSettings.attributes } returns
      mapOf(
        "email_address" to
          UserSettings.AttributesConfig(
            enabled = true,
            required = true,
            usedForFirstFactor = true,
            firstFactors = listOf(Constants.Strategy.EMAIL_CODE, Constants.Strategy.EMAIL_LINK),
            usedForSecondFactor = false,
            secondFactors = emptyList(),
            verifications = listOf(Constants.Strategy.EMAIL_LINK),
            verifyAtSignUp = true,
          )
      )
    val signUp = signUp()

    assertEquals(Constants.Strategy.EMAIL_LINK, signUp.emailVerificationStrategy)
    assertTrue(signUp.isEmailLinkVerificationSupported)
  }

  @Test
  fun prepareVerificationMapsNativeEmailLinkPkceFields() {
    val fieldsSlot = slot<Map<String, String>>()
    val signUp = signUp()

    coEvery { mockSignUpApi.prepareSignUpVerification(signUp.id, capture(fieldsSlot)) } returns
      ClerkResult.success(signUp)

    runBlocking {
      signUp.prepareVerification(
        SignUp.PrepareVerificationParams.Strategy.EmailLink(redirectUrl = "app://clerk-callback")
      )
    }

    coVerify(exactly = 1) { mockSignUpApi.prepareSignUpVerification(signUp.id, any()) }
    assertEquals(Constants.Strategy.EMAIL_LINK, fieldsSlot.captured["strategy"])
    assertEquals("app://clerk-callback", fieldsSlot.captured["redirect_uri"])
    assertEquals("S256", fieldsSlot.captured["code_challenge_method"])
    assertTrue(fieldsSlot.captured["code_challenge"]?.isNotBlank() == true)
  }

  private fun signUp(verifications: Map<String, Verification?> = emptyMap()): SignUp {
    return SignUp(
      id = "sign_up_123",
      status = SignUp.Status.MISSING_REQUIREMENTS,
      requiredFields = listOf("email_address"),
      optionalFields = emptyList(),
      missingFields = emptyList(),
      unverifiedFields = listOf("email_address"),
      verifications = verifications,
      emailAddress = "sam@clerk.dev",
      passwordEnabled = false,
    )
  }
}
