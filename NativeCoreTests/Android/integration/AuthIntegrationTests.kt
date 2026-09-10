package com.clerk.api.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clerk.api.Clerk
import com.clerk.api.SignInEmailCodeSendCodeParamsCase1
import com.clerk.api.SignInEmailCodeSendParams
import com.clerk.api.SignInEmailCodeVerifyParams
import com.clerk.api.SignInPasswordParams
import com.clerk.api.SignInPasswordParamsCase1
import com.clerk.api.SignUpCreateParams
import com.clerk.api.SignUpEmailCodeVerifyParams
import com.clerk.api.SignUpPhoneCodeVerifyParams
import com.clerk.api.close
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in live development-instance test; creates and deletes its own synthetic account. */
@RunWith(AndroidJUnit4::class)
class AuthIntegrationTests {
  @Test
  fun signUpAndSignInWithEmailCodes() = runBlocking {
    val key = requirePublishableKey()
    withTimeout(180_000) {
      withContext(Dispatchers.Main.immediate) {
        val clerk = connectForIntegrationTesting(key)
        val email = generateTestEmail()
        var didCreateSignUp = false
        try {
          clerk.signUp.create(
            SignUpCreateParams(
              emailAddress = email,
              phoneNumber = generateTestPhone(),
              password = TEST_PASSWORD,
              firstName = "Integration",
              lastName = "Test",
            )
          )
          didCreateSignUp = true
          check(clerk.signUp.emailAddress == email)
          clerk.signUp.verifications.sendEmailCode()
          clerk.signUp.verifications.verifyEmailCode(
            SignUpEmailCodeVerifyParams(TEST_VERIFICATION_CODE)
          )
          if (clerk.signUp.unverifiedFields.any { it.rawValue == "phone_number" }) {
            clerk.signUp.verifications.sendPhoneCode()
            clerk.signUp.verifications.verifyPhoneCode(
              SignUpPhoneCodeVerifyParams(TEST_VERIFICATION_CODE)
            )
          }
          check(clerk.signUp.status.rawValue == "complete")
          val createdUserId = checkNotNull(clerk.signUp.createdUserId)
          val createdSessionId = checkNotNull(clerk.signUp.createdSessionId)
          check(clerk.session == null && clerk.user == null)
          clerk.signUp.finalize()
          check(clerk.session?.id == createdSessionId && clerk.user?.id == createdUserId)

          clerk.signOut()
          check(clerk.session == null && clerk.user == null)
          clerk.signIn.emailCode.sendCode(
            SignInEmailCodeSendParams.Case1(SignInEmailCodeSendCodeParamsCase1(email))
          )
          clerk.signIn.emailCode.verifyCode(SignInEmailCodeVerifyParams(TEST_VERIFICATION_CODE))
          check(clerk.signIn.status.rawValue == "complete")
          val signInSessionId = checkNotNull(clerk.signIn.createdSessionId)
          check(clerk.session == null && clerk.user == null)
          clerk.signIn.finalize()
          check(clerk.session?.id == signInSessionId)
          val user = checkNotNull(clerk.user)
          check(user.id == createdUserId)
          // Successful cleanup is part of the passing path.
          user.delete()
        } catch (error: Throwable) {
          // Preserve the original failure, but give owned-account cleanup a bounded chance
          // even if the caller's timeout canceled the authentication coroutine.
          withContext(NonCancellable) {
            withTimeoutOrNull(30_000) { deleteTestAccountIfExists(clerk, email, didCreateSignUp) }
          }
          throw error
        } finally {
          clerk.close()
        }
      }
    }
  }

  private suspend fun deleteTestAccountIfExists(
    clerk: Clerk,
    email: String,
    allowPasswordCleanup: Boolean,
  ) {
    try {
      if (clerk.user == null && allowPasswordCleanup) {
        clerk.signIn.password(
          SignInPasswordParams.Case1(SignInPasswordParamsCase1(TEST_PASSWORD, email))
        )
        if (clerk.signIn.status.rawValue != "complete") return
        clerk.signIn.finalize()
      }
      val user = clerk.user ?: return
      check(user.emailAddresses.any { it.emailAddress == email })
      user.delete()
    } catch (error: CancellationException) {
      throw error
    } catch (_: Exception) {
      // An incomplete sign-up may not have an account; preserve the original failure.
      // A completed account whose cleanup fails requires instance-side cleanup.
    }
  }
}
