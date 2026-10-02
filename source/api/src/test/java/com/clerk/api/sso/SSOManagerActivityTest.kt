package com.clerk.api.sso

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.clerk.api.Clerk
import com.clerk.api.Constants.Storage.KEY_AUTHORIZATION_STARTED
import com.clerk.api.auth.Auth
import com.clerk.api.auth.createSignUp
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.api.externalaccount.ExternalAccountService
import com.clerk.api.hostedauth.HostedAuthService
import com.clerk.api.magiclink.NativeMagicLinkError
import com.clerk.api.magiclink.NativeMagicLinkService
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.UserApi
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.redirect.CallbackOutcome
import com.clerk.api.redirect.PendingRedirect
import com.clerk.api.redirect.RedirectCoordinator
import com.clerk.api.session.Session
import com.clerk.api.signup.SignUp
import com.clerk.api.user.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.lang.ref.WeakReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class SSOManagerActivityTest {

  // Captured once so verifications count calls on Auth, not reads of the mocked Clerk.auth.
  private val auth: Auth = Clerk.auth

  private val app: Application
    get() = ApplicationProvider.getApplicationContext()

  @Before
  fun setup() {
    // Ensure AppCompat theme for AppCompatActivity
    app.setTheme(androidx.appcompat.R.style.Theme_AppCompat)
  }

  @After
  fun tearDown() {
    RedirectCoordinator.resetForTests()
    unmockkAll()
  }

  @Test
  fun callbackIsDispatchedThroughCoordinator_andSetsResultOk() {
    mockkObject(RedirectCoordinator)
    coEvery { RedirectCoordinator.dispatch(any()) } returns CallbackOutcome.Completed(true)
    val responseUri = Uri.parse("clerk://callback?rotating_token_nonce=abc")

    val activity = resumeWithCallback(responseUri, authorizationStarted = true)

    coVerify(exactly = 1) { RedirectCoordinator.dispatch(responseUri) }
    assertEquals(Activity.RESULT_OK, Shadows.shadowOf(activity).resultCode)
    assertTrue(activity.isFinishing)
  }

  @Test
  fun callbackIntent_completesWithoutAuthorizationStarted() {
    mockkObject(RedirectCoordinator)
    coEvery { RedirectCoordinator.dispatch(any()) } returns CallbackOutcome.Completed(true)
    val responseUri = Uri.parse("clerk://callback?rotating_token_nonce=abc")

    val activity = resumeWithCallback(responseUri)

    coVerify(exactly = 1) { RedirectCoordinator.dispatch(responseUri) }
    assertEquals(Activity.RESULT_OK, Shadows.shadowOf(activity).resultCode)
  }

  @Test
  fun failedOrIgnoredCallback_setsResultCanceled() {
    mockkObject(RedirectCoordinator)
    coEvery { RedirectCoordinator.dispatch(any()) } returns CallbackOutcome.Ignored
    val responseUri =
      Uri.parse("https://example.com/callback?__clerk_error_code=authentication_cancelled")

    val activity = resumeWithCallback(responseUri)

    coVerify(exactly = 1) { RedirectCoordinator.dispatch(responseUri) }
    assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
  }

  @Test
  fun newAuthorizationIntent_restartsManagerWithNewUrl() {
    val firstUri = Uri.parse("https://accounts.example.com/first")
    val secondUri = Uri.parse("https://accounts.example.com/second")
    val controller =
      Robolectric.buildActivity(
        SSOManagerActivity::class.java,
        SSOManagerActivity.createAuthorizationIntent(app, firstUri),
      )
    val activity = controller.create().resume().get()
    val shadow = Shadows.shadowOf(activity)

    assertEquals(firstUri, shadow.nextStartedActivity.data)

    controller.pause()
    controller.newIntent(SSOManagerActivity.createAuthorizationIntent(app, secondUri))
    controller.resume()

    assertEquals(secondUri, shadow.nextStartedActivity.data)
  }

  @Test
  fun authorizationCanceled_cancelsPendingFlow_whenNoData() {
    val pending = startPendingSso()
    val intent =
      SSOManagerActivity.createBaseIntent(app).apply {
        putExtra(KEY_AUTHORIZATION_STARTED, true)
      }

    val activity =
      Robolectric.buildActivity(SSOManagerActivity::class.java, intent).create().resume().get()

    assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
    val failure = runBlocking { pending.result.await() } as ClerkResult.Failure
    assertTrue(failure.throwable is SSOCancellationException)
    assertFalse(RedirectCoordinator.hasPendingRedirect.value)
  }

  @Test
  fun authorizationCanceled_keepsFlowWhoseCallbackIsAlreadyCompleting() {
    val pending = startPendingSso()
    pending.completionStarted.set(true)
    val intent =
      SSOManagerActivity.createBaseIntent(app).apply {
        putExtra(KEY_AUTHORIZATION_STARTED, true)
      }

    Robolectric.buildActivity(SSOManagerActivity::class.java, intent).create().resume()

    assertFalse(pending.result.isCompleted)
    assertTrue(RedirectCoordinator.isCurrent(pending))
  }

  @Test
  fun authorizationCanceled_failsPendingExternalAccountConnection() = runTest {
    mockkObject(ClerkApi)
    mockkObject(Clerk)
    val userApi = mockk<UserApi>()
    val verification = mockk<Verification>()
    val externalAccount = mockk<ExternalAccount>()
    every { verification.externalVerificationRedirectUrl } returns "https://oauth.example.com/auth"
    every { externalAccount.verification } returns verification
    every { externalAccount.id } returns "ext_account_123"
    every { ClerkApi.user } returns userApi
    coEvery { userApi.createExternalAccount(any(), any()) } returns
      ClerkResult.success(externalAccount)
    every { Clerk.applicationContext } returns WeakReference(mockk(relaxed = true))
    every { Clerk.debugMode } returns false

    val pendingResult = async {
      ExternalAccountService.connectExternalAccount(
        User.CreateExternalAccountParams(provider = OAuthProvider.GOOGLE)
      )
    }
    runCurrent()
    assertTrue(ExternalAccountService.hasPendingExternalAccountConnection())

    // The user dismisses the browser: the manager resumes without a callback URI.
    val intent =
      SSOManagerActivity.createBaseIntent(app).apply {
        putExtra(KEY_AUTHORIZATION_STARTED, true)
      }
    val activity =
      Robolectric.buildActivity(SSOManagerActivity::class.java, intent).create().resume().get()

    assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
    val failure = withTimeout(5_000L) { pendingResult.await() } as ClerkResult.Failure
    assertTrue(failure.throwable is SSOCancellationException)
    assertFalse(ExternalAccountService.hasPendingExternalAccountConnection())
  }

  @Test
  fun authorizationUrl_isNotTreatedAsCallback_whenBrowserIsDismissed() {
    mockkObject(RedirectCoordinator)
    val authorizationUri = Uri.parse("https://accounts.example.com/oauth/authorize")

    val activity = resumeWithCallback(authorizationUri, authorizationStarted = true)

    assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
    coVerify(exactly = 0) { RedirectCoordinator.dispatch(any()) }
    verify(exactly = 1) { RedirectCoordinator.cancelPendingUnlessCompleting() }
  }

  @Test
  fun hostedAuthCallback_callsOnlyHostedAuthService() {
    mockkObject(HostedAuthService)
    mockkObject(SSOService)
    val redirectUrl = "clerk://com.example.app.callback"
    RedirectCoordinator.begin(
      PendingRedirect.HostedAuth(redirectUrl = redirectUrl, state = "state_123", codeVerifier = "v")
    )
    val responseUri =
      Uri.parse(
        "$redirectUrl?state=state_123&rotating_token_nonce=nonce_123&created_session_id=sess_123"
      )
    coEvery { HostedAuthService.complete(responseUri) } returns
      ClerkResult.success(mockk<Session>(relaxed = true))

    val activity = resumeWithCallback(responseUri)

    coVerify(exactly = 1) { HostedAuthService.complete(responseUri) }
    coVerify(exactly = 0) { SSOService.completeRedirect(any(), any()) }
    assertEquals(Activity.RESULT_OK, Shadows.shadowOf(activity).resultCode)
  }

  @Test
  fun callback_reattachesAfterRecreation() {
    mockkObject(RedirectCoordinator)
    val gate = CompletableDeferred<Unit>()
    coEvery { RedirectCoordinator.dispatch(any()) } coAnswers
      {
        gate.await()
        CallbackOutcome.Completed(true)
      }
    val responseUri = Uri.parse("clerk://callback?rotating_token_nonce=abc")
    val intent = SSOManagerActivity.createResponseHandlingIntent(app, responseUri)
    val controller = Robolectric.buildActivity(SSOManagerActivity::class.java, intent)

    controller.create().resume()
    coVerify(exactly = 1) { RedirectCoordinator.dispatch(responseUri) }

    val savedState = Bundle()
    controller.saveInstanceState(savedState).pause().stop().destroy()
    val recreated =
      Robolectric.buildActivity(SSOManagerActivity::class.java, intent).create(savedState).resume()
    coVerify(exactly = 2) { RedirectCoordinator.dispatch(responseUri) }

    gate.complete(Unit)
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    assertTrue(recreated.get().isFinishing)
  }

  @Test
  fun callback_isDispatchedOnlyOnce_whenResumedTwice() {
    mockkObject(RedirectCoordinator)
    val gate = CompletableDeferred<Unit>()
    coEvery { RedirectCoordinator.dispatch(any()) } coAnswers
      {
        gate.await()
        CallbackOutcome.Completed(true)
      }
    val responseUri = Uri.parse("clerk://callback?rotating_token_nonce=abc")
    val intent =
      SSOManagerActivity.createResponseHandlingIntent(app, responseUri).apply {
        putExtra(KEY_AUTHORIZATION_STARTED, true)
      }

    val controller = Robolectric.buildActivity(SSOManagerActivity::class.java, intent)
    controller.create().resume()
    controller.pause().resume()

    coVerify(exactly = 1) { RedirectCoordinator.dispatch(any()) }
    gate.complete(Unit)
  }

  @Test
  fun dispatchFailure_setsResultCanceled() {
    mockkObject(RedirectCoordinator)
    coEvery { RedirectCoordinator.dispatch(any()) } throws RuntimeException("boom")
    val responseUri = Uri.parse("clerk://callback?rotating_token_nonce=abc")

    val activity = resumeWithCallback(responseUri, authorizationStarted = true)

    assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
    assertTrue(activity.isFinishing)
  }

  @Test
  fun ssoCompletion_survivesRecreation_andRecreatedActivityFinishes() = runTest {
    mockkObject(Clerk)
    mockkStatic("com.clerk.api.auth.AuthFlowsKt")
    every { Clerk.applicationContext } returns WeakReference(mockk(relaxed = true))
    val signUp = mockk<SignUp>(relaxed = true)
    val gate = CompletableDeferred<Unit>()
    coEvery { auth.createSignUp(SignUp.CreateParams.Transfer) } coAnswers
      {
        gate.await()
        ClerkResult.success(signUp)
      }
    val pendingResult =
      async(start = CoroutineStart.UNDISPATCHED) {
        SSOService.authenticateWithPreparedRedirect("https://accounts.example.com/oauth/authorize")
      }

    val responseUri =
      Uri.parse(
        "clerk://com.example.app.callback" +
          "?__clerk_status=failed&__clerk_error_code=external_account_not_found"
      )
    val intent = SSOManagerActivity.createResponseHandlingIntent(app, responseUri)
    val controller = Robolectric.buildActivity(SSOManagerActivity::class.java, intent)
    controller.create().resume()
    assertFalse(controller.get().isFinishing)

    val nightMode =
      Configuration(controller.get().resources.configuration).apply {
        uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
      }
    controller.configurationChange(nightMode)
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    assertFalse(pendingResult.isCompleted)

    gate.complete(Unit)
    val result = withTimeout(5_000L) { pendingResult.await() } as ClerkResult.Success
    assertEquals(signUp, result.value.signUp)
    waitForMainLooper { controller.get().isFinishing }
    assertEquals(Activity.RESULT_OK, Shadows.shadowOf(controller.get()).resultCode)
    coVerify(exactly = 1) { auth.createSignUp(SignUp.CreateParams.Transfer) }
  }

  @Test
  fun nativeMagicLinkFailure_setsResultCanceled() {
    mockkObject(NativeMagicLinkService)
    coEvery { NativeMagicLinkService.handleMagicLinkDeepLink(any()) } returns
      ClerkResult.apiFailure(NativeMagicLinkError(reasonCode = "native_magic_link_complete_failed"))
    val responseUri =
      Uri.parse("clerk://com.clerk.test.oauth?flow_id=flow_123&approval_token=approval_123")

    val activity = resumeWithCallback(responseUri, authorizationStarted = true)
    waitForMainLooper { activity.isFinishing }

    assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
  }

  private fun resumeWithCallback(
    responseUri: Uri,
    authorizationStarted: Boolean = false,
  ): SSOManagerActivity {
    val intent =
      SSOManagerActivity.createResponseHandlingIntent(app, responseUri).apply {
        if (authorizationStarted) putExtra(KEY_AUTHORIZATION_STARTED, true)
      }
    return Robolectric.buildActivity(SSOManagerActivity::class.java, intent).create().resume().get()
  }

  private fun startPendingSso(): PendingRedirect.Sso =
    PendingRedirect.Sso(
        expectedState = "state",
        transferable = true,
        redirectFlow = PendingRedirect.RedirectFlow.SIGN_IN,
        signUp = null,
      )
      .also(RedirectCoordinator::begin)

  /** Completion runs off the main thread; pump the main looper until [condition] holds. */
  private fun waitForMainLooper(condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + 5_000L
    while (!condition() && System.currentTimeMillis() < deadline) {
      Shadows.shadowOf(Looper.getMainLooper()).idle()
      Thread.sleep(5)
    }
    assertTrue(condition())
  }
}
