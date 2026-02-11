package com.clerk.api.integration

import com.clerk.api.Clerk
import com.clerk.api.auth.types.VerificationType
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.verifyCode
import com.clerk.api.signup.sendCode
import com.clerk.api.signup.verifyCode
import com.clerk.api.user.delete
import kotlinx.coroutines.runBlocking
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Integration tests for SignIn and SignUp flows.
 *
 * These tests make real API calls to a Clerk instance and verify that the SDK correctly integrates
 * with the Clerk API. Unlike unit tests which use mocked responses, these tests verify end-to-end
 * functionality including proper JSON serialization.
 *
 * Requirements:
 * - Network access
 * - Valid Clerk test instance (configured via `.keys.json`)
 * - Test emails use the `+clerk_test` subaddress so Clerk accepts code "424242"
 * - Test phones use fictional 555-01xx numbers so Clerk accepts code "424242"
 */
@RunWith(RobolectricTestRunner::class)
class AuthIntegrationTests {

  companion object {
    /** Fictional phone number in the 555-01xx range — accepted by Clerk test instances. */
    private const val TEST_PHONE = "+12015550100"
  }

  @Test
  fun `sign up and sign in with email codes`(): Unit = runBlocking {
    val pk = requirePublishableKey()
    initializeClerkAndWait(pk)

    val testEmail = generateTestEmail()
    var didCreateSignUp = false

    try {
      // ── Sign Up (provide all required fields upfront) ──
      val signUp =
        assertSuccess(
          "signUp",
          Clerk.auth.signUp {
            email = testEmail
            phone = TEST_PHONE
            password = TEST_PASSWORD
            firstName = "Integration"
            lastName = "Test"
          },
        )
      didCreateSignUp = true

      // ── Verify email ──
      val afterEmailSend = assertSuccess("sendEmailCode", signUp.sendCode { email = testEmail })
      val afterEmailVerify =
        assertSuccess(
          "verifyEmailCode",
          afterEmailSend.verifyCode(TEST_VERIFICATION_CODE, VerificationType.EMAIL),
        )

      // ── Verify phone (if still required) ──
      var latestSignUp = afterEmailVerify
      if ("phone_number" in latestSignUp.unverifiedFields) {
        val afterPhoneSend =
          assertSuccess("sendPhoneCode", latestSignUp.sendCode { phone = TEST_PHONE })
        latestSignUp =
          assertSuccess(
            "verifyPhoneCode",
            afterPhoneSend.verifyCode(TEST_VERIFICATION_CODE, VerificationType.PHONE),
          )
      }

      // ── Sign Out ──
      assertSuccess("signOut", Clerk.auth.signOut())

      // ── Sign In with OTP (creates sign-in and sends code in one call) ──
      val signIn = assertSuccess("signInWithOtp", Clerk.auth.signInWithOtp { email = testEmail })

      // ── Verify sign-in code ──
      assertSuccess("signIn verifyCode", signIn.verifyCode(TEST_VERIFICATION_CODE))
    } finally {
      deleteTestAccountIfExists(testEmail, didCreateSignUp)
    }
  }

  /** Asserts a [ClerkResult] is [ClerkResult.Success], printing the API error on failure. */
  private fun <T : Any> assertSuccess(step: String, result: ClerkResult<T, ClerkErrorResponse>): T {
    if (result is ClerkResult.Failure) {
      val errors = result.error?.errors?.joinToString { "${it.code}: ${it.longMessage}" }
      fail("$step failed — code=${result.code}, type=${result.errorType}, errors=[$errors]")
    }
    return (result as ClerkResult.Success).value
  }

  /**
   * Best-effort cleanup: delete the test user if one was created.
   *
   * If we have a current user session, delete directly. Otherwise fall back to signing in with
   * password first (handles the case where sign-up succeeded but subsequent steps failed).
   */
  private suspend fun deleteTestAccountIfExists(email: String, allowPasswordCleanup: Boolean) {
    try {
      if (Clerk.user != null) {
        Clerk.user?.delete()
        return
      }

      if (!allowPasswordCleanup) return

      val signIn =
        Clerk.auth.signInWithPassword {
          identifier = email
          password = TEST_PASSWORD
        }
      if (signIn is ClerkResult.Success) {
        Clerk.user?.delete()
      }
    } catch (_: Exception) {
      // Best-effort — don't mask the original test failure
    }
  }
}
