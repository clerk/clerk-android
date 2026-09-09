package com.clerk.ui.signin

import com.clerk.api.*
import com.clerk.testing.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class SignInRedirectTest {
  @After fun tearDown() = unmockkAll()

  @Test
  fun redirectUsesTheCurrentCoreAndPreservesProviderAndTransfer() = runTest {
    val clerk = mockClerk()
    val signIn = mockSignIn(SignInStatus.NeedsFirstFactor)
    every { clerk.signIn } returns signIn
    val response = MobileAuthenticationResult.Case1(MobileAuthCallbackResultCase1(signIn))
    coEvery { clerk.authenticateWithSSO(any()) } returns response
    val result = authenticateWithRedirect(clerk, OAuthProvider.Github, true)
    assertSame(response, result)
    coVerify(exactly = 1) {
      clerk.authenticateWithSSO(
        match {
          it.strategy.rawValue == "oauth_github" &&
            it.start == MobileSSOParamsStart.SignIn &&
            it.transferable == true
        }
      )
    }
  }
}
