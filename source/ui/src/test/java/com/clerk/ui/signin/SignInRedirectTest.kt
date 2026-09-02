package com.clerk.ui.signin

import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.authenticateWithPreparedRedirect
import com.clerk.api.signin.prepareFirstFactor
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SignInRedirectTest {

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `redirect reuses the current sign in attempt`() = runTest {
    mockkObject(SignIn.Companion)
    mockkStatic("com.clerk.api.signin.SignInKt")
    mockkStatic("com.clerk.api.signin.SignInExtensionsKt")
    val currentSignIn =
      SignIn(
        id = "sign_in_existing",
        status = SignIn.Status.NEEDS_FIRST_FACTOR,
        identifier = "user@example.com",
        supportedFirstFactors = listOf(Factor(strategy = "password")),
      )
    val externalRedirectUrl = "https://oauth.example.com/start"
    val preparedSignIn =
      currentSignIn.copy(
        firstFactorVerification =
          Verification(
            strategy = OAuthProvider.GITHUB.strategy,
            externalVerificationRedirectUrl = externalRedirectUrl,
          )
      )
    val oauthResult = OAuthResult(signIn = preparedSignIn)
    val prepareParams = slot<SignIn.PrepareFirstFactorParams>()

    coEvery { currentSignIn.prepareFirstFactor(capture(prepareParams)) } returns
      ClerkResult.success(preparedSignIn)
    coEvery { preparedSignIn.authenticateWithPreparedRedirect(transferable = true) } returns
      ClerkResult.success(oauthResult)

    val result =
      authenticateWithRedirect(
        signIn = currentSignIn,
        provider = OAuthProvider.GITHUB,
        transferable = true,
      )

    assertSame(oauthResult, (result as ClerkResult.Success).value)
    assertEquals(OAuthProvider.GITHUB.strategy, prepareParams.captured.strategy)
    coVerify(exactly = 1) { currentSignIn.prepareFirstFactor(any()) }
    coVerify(exactly = 1) { preparedSignIn.authenticateWithPreparedRedirect(transferable = true) }
    coVerify(exactly = 0) { SignIn.authenticateWithRedirect(any(), any()) }
  }
}
