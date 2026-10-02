package com.clerk.api.sso

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.clerk.api.Clerk
import com.clerk.api.auth.Auth
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

/**
 * OAuth callbacks reach the app through the exported [SSOReceiverActivity] or the host app's own
 * deep-link handler, so any app on the device can fire one. A callback must carry the state the SDK
 * put in the redirect URL for the in-flight flow; anything else is ignored and must not end it.
 */
@RunWith(RobolectricTestRunner::class)
class RedirectCallbackStateTest {
  private val signInApi = mockk<SignInApi>()
  private val prepareParams = slot<Map<String, String>>()

  @Before
  fun setUp() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    mockkObject(Clerk)
    every { Clerk.applicationContext } returns WeakReference(application)
    mockkObject(ClerkApi)
    every { ClerkApi.signIn } returns signInApi
    val createdSignIn =
      SignIn(
        id = SIGN_IN_ID,
        status = SignIn.Status.NEEDS_FIRST_FACTOR,
        supportedFirstFactors = listOf(Factor(strategy = "oauth_google")),
      )
    coEvery { signInApi.createSignIn(any()) } returns ClerkResult.success(createdSignIn)
    coEvery { signInApi.prepareSignInFirstFactor(SIGN_IN_ID, capture(prepareParams)) } returns
      ClerkResult.success(
        SignIn(
          id = SIGN_IN_ID,
          firstFactorVerification =
            Verification(externalVerificationRedirectUrl = EXTERNAL_VERIFICATION_URL),
        )
      )
  }

  @After
  fun tearDown() {
    SSOService.cancelPendingAuthentication()
    unmockkAll()
  }

  @Test
  fun `callbacks without the flow state do not end the pending sign in`() = runTest {
    val pendingResult =
      async(start = CoroutineStart.UNDISPATCHED) {
        SSOService.authenticateWithRedirect(strategy = "oauth_google", redirectUrl = REDIRECT_URL)
      }

    val auth = Auth()
    auth.handle(Uri.parse("$REDIRECT_URL?error=access_denied"))
    auth.handle(Uri.parse("$REDIRECT_URL?error=access_denied&clerk_redirect_state=forged"))
    auth.handle(Uri.parse("$REDIRECT_URL?rotating_token_nonce=forged&clerk_redirect_state="))

    assertFalse(pendingResult.isCompleted)
    assertTrue(SSOService.hasPendingAuthentication())

    // The real callback for this flow still completes it.
    val state = Uri.parse(prepareParams.captured.getValue("redirect_url")).redirectState()
    assertNotNull(state)
    assertTrue(auth.handle(Uri.parse("$REDIRECT_URL?error=access_denied&$STATE_PARAM=$state")))

    val failure = withTimeout(TIMEOUT_MS) { pendingResult.await() } as ClerkResult.Failure
    assertTrue(failure.throwable is SSOCancellationException)
    assertFalse(SSOService.hasPendingAuthentication())
  }

  @Test
  fun `receiver drops a callback whose state does not match the pending flow`() = runTest {
    val pendingResult =
      async(start = CoroutineStart.UNDISPATCHED) {
        SSOService.authenticateWithRedirect(strategy = "oauth_google", redirectUrl = REDIRECT_URL)
      }

    // Drop the browser launch the flow itself started.
    Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>())
      .clearNextStartedActivities()
    val forged = createReceiver(Uri.parse("$REDIRECT_URL?error=access_denied&$STATE_PARAM=forged"))
    assertNull(Shadows.shadowOf(forged).nextStartedActivity)
    assertTrue(forged.isFinishing)

    val missing = createReceiver(Uri.parse("$REDIRECT_URL?error=access_denied"))
    assertNull(Shadows.shadowOf(missing).nextStartedActivity)

    val state = Uri.parse(prepareParams.captured.getValue("redirect_url")).redirectState()
    val legitimate = createReceiver(Uri.parse("$REDIRECT_URL?error=x&$STATE_PARAM=$state"))
    assertNotNull(Shadows.shadowOf(legitimate).nextStartedActivity)

    assertFalse(pendingResult.isCompleted)

    SSOService.cancelPendingAuthentication()
    assertTrue(withTimeout(TIMEOUT_MS) { pendingResult.await() } is ClerkResult.Failure)
  }

  private fun createReceiver(callbackUri: Uri): SSOReceiverActivity {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val intent = Intent(context, SSOReceiverActivity::class.java).apply { data = callbackUri }
    return Robolectric.buildActivity(SSOReceiverActivity::class.java, intent).create().get()
  }

  private fun Uri.redirectState(): String? = getQueryParameter(STATE_PARAM)

  private companion object {
    const val SIGN_IN_ID = "sia_123"
    const val REDIRECT_URL = "clerk://com.example.app.callback"
    const val EXTERNAL_VERIFICATION_URL = "https://accounts.example.com/oauth/authorize"
    const val STATE_PARAM = "clerk_redirect_state"
    const val TIMEOUT_MS = 5_000L
  }
}
