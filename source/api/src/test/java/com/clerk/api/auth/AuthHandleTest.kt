package com.clerk.api.auth

import android.net.Uri
import com.clerk.api.magiclink.NativeMagicLinkAuthResult
import com.clerk.api.magiclink.NativeMagicLinkService
import com.clerk.api.magiclink.canHandleNativeMagicLink
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.sso.SSOService
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthHandleTest {
  private lateinit var auth: Auth

  @Before
  fun setup() {
    auth = Auth()
    mockkObject(NativeMagicLinkService)
    mockkStatic(::canHandleNativeMagicLink)
    mockkObject(SSOService)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `handle completes native magic link callback without SSO fallback`() = runTest {
    val callbackUri = mockk<Uri>(relaxed = true)

    every { canHandleNativeMagicLink(callbackUri) } returns true
    coEvery { NativeMagicLinkService.handleMagicLinkDeepLink(callbackUri) } returns
      ClerkResult.success(NativeMagicLinkAuthResult.SignIn(mockk<SignIn>(relaxed = true)))

    val handled = auth.handle(callbackUri)

    assertTrue(handled)
    coVerify(exactly = 1) { NativeMagicLinkService.handleMagicLinkDeepLink(callbackUri) }
    coVerify(exactly = 0) { SSOService.completeAuthenticateWithRedirect(any()) }
  }

  @Test
  fun `handle completes SSO callback when URI is Clerk scheme and not magic link`() = runTest {
    val callbackUri = mockk<Uri>(relaxed = true)

    every { canHandleNativeMagicLink(callbackUri) } returns false
    every { callbackUri.scheme } returns "clerk"
    coJustRun { SSOService.completeAuthenticateWithRedirect(callbackUri) }

    val handled = auth.handle(callbackUri)

    assertTrue(handled)
    coVerify(exactly = 0) { NativeMagicLinkService.handleMagicLinkDeepLink(any()) }
    coVerify(exactly = 1) { SSOService.completeAuthenticateWithRedirect(callbackUri) }
  }

  @Test
  fun `handle returns false for non Clerk URIs`() = runTest {
    val callbackUri = mockk<Uri>(relaxed = true)

    every { canHandleNativeMagicLink(callbackUri) } returns false
    every { callbackUri.scheme } returns "https"

    val handled = auth.handle(callbackUri)

    assertFalse(handled)
    coVerify(exactly = 0) { NativeMagicLinkService.handleMagicLinkDeepLink(any()) }
    coVerify(exactly = 0) { SSOService.completeAuthenticateWithRedirect(any()) }
  }

  @Test
  fun `handle returns false for null URI`() = runTest {
    assertFalse(auth.handle(null))
    coVerify(exactly = 0) { NativeMagicLinkService.handleMagicLinkDeepLink(any()) }
    coVerify(exactly = 0) { SSOService.completeAuthenticateWithRedirect(any()) }
  }
}
