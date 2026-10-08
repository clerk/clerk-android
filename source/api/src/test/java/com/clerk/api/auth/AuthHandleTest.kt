package com.clerk.api.auth

import android.net.Uri
import com.clerk.api.hostedauth.HostedAuthService
import com.clerk.api.magiclink.NativeMagicLinkAuthResult
import com.clerk.api.magiclink.NativeMagicLinkService
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.redirect.PendingRedirect
import com.clerk.api.redirect.RedirectCoordinator
import com.clerk.api.session.Session
import com.clerk.api.signin.SignIn
import com.clerk.api.sso.OAuthResult
import com.clerk.api.sso.SSOService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthHandleTest {
  private lateinit var auth: Auth

  @Before
  fun setup() {
    auth = Auth()
    mockkObject(NativeMagicLinkService)
    mockkObject(HostedAuthService)
    mockkObject(SSOService)
    coEvery { SSOService.completeRedirect(any(), any()) } coAnswers
      {
        val pending = firstArg<PendingRedirect.Sso>()
        RedirectCoordinator.finish(pending, ClerkResult.success(OAuthResult()))
      }
  }

  @After
  fun tearDown() {
    RedirectCoordinator.resetForTests()
    unmockkAll()
  }

  @Test
  fun `handle completes native magic link callback without SSO fallback`() = runTest {
    val callbackUri = Uri.parse("clerk://app.callback?flow_id=flow_123&approval_token=token")
    coEvery { NativeMagicLinkService.handleMagicLinkDeepLink(callbackUri) } returns
      ClerkResult.success(NativeMagicLinkAuthResult.SignIn(mockk<SignIn>(relaxed = true)))

    assertTrue(auth.handle(callbackUri))

    coVerify(exactly = 1) { NativeMagicLinkService.handleMagicLinkDeepLink(callbackUri) }
    coVerify(exactly = 0) { SSOService.completeRedirect(any(), any()) }
  }

  @Test
  fun `handle completes the pending SSO flow when the callback carries its state`() = runTest {
    val pending = startSso(state = "state_123")

    assertTrue(auth.handle(Uri.parse("$CALLBACK?rotating_token_nonce=n&$STATE=state_123")))

    coVerify(exactly = 1) { SSOService.completeRedirect(pending, any()) }
    assertTrue(pending.result.await() is ClerkResult.Success)
  }

  @Test
  fun `handle completes an SSO callback on a custom redirect URL`() = runTest {
    val pending = startSso(state = "state_123")

    assertTrue(auth.handle(Uri.parse("myapp://oauth?rotating_token_nonce=n&$STATE=state_123")))

    coVerify(exactly = 1) { SSOService.completeRedirect(pending, any()) }
  }

  @Test
  fun `handle ignores an SSO callback with the wrong state`() = runTest {
    val pending = startSso(state = "state_123")

    assertTrue(auth.handle(Uri.parse("$CALLBACK?error=access_denied&$STATE=forged")))

    coVerify(exactly = 0) { SSOService.completeRedirect(any(), any()) }
    assertFalse(pending.result.isCompleted)
    assertTrue(RedirectCoordinator.isCurrent(pending))
  }

  @Test
  fun `handle completes hosted auth callback before SSO fallback`() = runTest {
    val session = mockk<Session>(relaxed = true)
    RedirectCoordinator.begin(
      PendingRedirect.HostedAuth(redirectUrl = "myapp://hosted", state = "s", codeVerifier = "v")
    )
    val callbackUri =
      Uri.parse("myapp://hosted?state=s&rotating_token_nonce=n&created_session_id=sess")
    coEvery { HostedAuthService.complete(any(), any()) } returns ClerkResult.success(session)

    assertTrue(auth.handle(callbackUri))

    coVerify(exactly = 1) { HostedAuthService.complete(any(), any()) }
    coVerify(exactly = 0) { SSOService.completeRedirect(any(), any()) }
  }

  @Test
  fun `handle does not fall back to SSO when hosted auth completes a Clerk scheme callback`() =
    runTest {
      val session = mockk<Session>(relaxed = true)
      RedirectCoordinator.begin(
        PendingRedirect.HostedAuth(
          redirectUrl = "clerk://com.example.app.callback",
          state = "s",
          codeVerifier = "v",
        )
      )
      val callbackUri =
        Uri.parse(
          "clerk://com.example.app.callback?state=s&rotating_token_nonce=n&created_session_id=sess"
        )
      coEvery { HostedAuthService.complete(any(), any()) } returns ClerkResult.success(session)

      assertTrue(auth.handle(callbackUri))

      coVerify(exactly = 1) { HostedAuthService.complete(any(), any()) }
      coVerify(exactly = 0) { SSOService.completeRedirect(any(), any()) }
    }

  @Test
  fun `handle returns false for non Clerk URIs`() = runTest {
    assertFalse(auth.handle(Uri.parse("https://example.com/page")))

    coVerify(exactly = 0) { NativeMagicLinkService.handleMagicLinkDeepLink(any()) }
    coVerify(exactly = 0) { SSOService.completeRedirect(any(), any()) }
  }

  @Test
  fun `handle reports a Clerk callback with nothing pending as handled`() = runTest {
    assertTrue(auth.handle(Uri.parse("$CALLBACK?rotating_token_nonce=n")))

    coVerify(exactly = 0) { SSOService.completeRedirect(any(), any()) }
  }

  @Test
  fun `handle returns false for null URI`() = runTest {
    assertFalse(auth.handle(null))
    coVerify(exactly = 0) { NativeMagicLinkService.handleMagicLinkDeepLink(any()) }
    coVerify(exactly = 0) { SSOService.completeRedirect(any(), any()) }
  }

  private fun startSso(state: String): PendingRedirect.Sso =
    PendingRedirect.Sso(
        expectedState = state,
        transferable = true,
        redirectFlow = PendingRedirect.RedirectFlow.SIGN_IN,
        signUp = null,
      )
      .also(RedirectCoordinator::begin)

  private companion object {
    const val CALLBACK = "clerk://com.example.app.callback"
    const val STATE = "clerk_redirect_state"
  }
}
