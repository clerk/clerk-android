package com.clerk.api.redirect

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.clerk.api.Clerk
import com.clerk.api.hostedauth.HostedAuthCancellationException
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.sso.OAuthResult
import com.clerk.api.sso.SSOCancellationException
import com.clerk.api.sso.SSOService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RedirectCoordinatorTest {
  private val completeCalls = AtomicInteger(0)
  private val completionGate = CompletableDeferred<Unit>()
  private val completionStarted = CompletableDeferred<Unit>()

  @Before
  fun setUp() {
    mockkObject(Clerk)
    every { Clerk.applicationContext } returns
      WeakReference(ApplicationProvider.getApplicationContext<Application>())
    mockkObject(SSOService)
    coEvery { SSOService.completeRedirect(any(), any()) } coAnswers
      {
        completeCalls.incrementAndGet()
        completionStarted.complete(Unit)
        completionGate.await()
        RedirectCoordinator.finish(firstArg(), ClerkResult.success(OAuthResult()))
      }
  }

  @After
  fun tearDown() {
    completionGate.complete(Unit)
    RedirectCoordinator.resetForTests()
    unmockkAll()
  }

  @Test
  fun `starting a flow completes the previous one with its cancellation`() = runBlocking {
    val first = startSso(EXTERNAL_URL_A)
    val second = startSso(EXTERNAL_URL_B)

    val failure = withTimeout(TIMEOUT_MS) { first.await() } as ClerkResult.Failure
    assertTrue(failure.throwable is SSOCancellationException)
    assertFalse(second.isCompleted)
    assertTrue(SSOService.hasPendingAuthentication())

    SSOService.cancelPendingAuthentication()
    assertTrue(withTimeout(TIMEOUT_MS) { second.await() } is ClerkResult.Failure)
  }

  @Test
  fun `flows of different kinds replace each other`() = runBlocking {
    val sso = startSso(EXTERNAL_URL_A)
    val hosted = PendingRedirect.HostedAuth("myapp://hosted", state = "s", codeVerifier = "v")

    RedirectCoordinator.begin(hosted)

    val ssoFailure = withTimeout(TIMEOUT_MS) { sso.await() } as ClerkResult.Failure
    assertTrue(ssoFailure.throwable is SSOCancellationException)

    RedirectCoordinator.begin(
      PendingRedirect.ExternalAccountConnection(expectedState = "e", externalAccountId = "eac")
    )
    val hostedFailure = hosted.result.await() as ClerkResult.Failure
    assertTrue(hostedFailure.throwable is HostedAuthCancellationException)
  }

  @Test
  fun `cancelling the caller frees the slot`() = runBlocking {
    RedirectState.remember(EXTERNAL_URL_A, STATE)
    val caller =
      launch(Dispatchers.Default) { SSOService.authenticateWithPreparedRedirect(EXTERNAL_URL_A) }
    waitUntil { RedirectCoordinator.hasPendingRedirect.value }

    caller.cancel()
    caller.join()

    assertFalse(RedirectCoordinator.hasPendingRedirect.value)
    assertEquals(null, RedirectCoordinator.current())
  }

  @Test
  fun `a callback after cancellation is ignored`() = runBlocking {
    val pending = startSso(EXTERNAL_URL_A, state = STATE)
    SSOService.cancelPendingAuthentication()
    assertTrue(withTimeout(TIMEOUT_MS) { pending.await() } is ClerkResult.Failure)

    val outcome = RedirectCoordinator.dispatch(callback(STATE))

    assertEquals(CallbackOutcome.Ignored, outcome)
    assertEquals(0, completeCalls.get())
  }

  @Test
  fun `a wrong state callback leaves the flow pending`() = runBlocking {
    val pending = startSso(EXTERNAL_URL_A, state = STATE)

    assertEquals(CallbackOutcome.Ignored, RedirectCoordinator.dispatch(callback("forged")))
    assertEquals(CallbackOutcome.Ignored, RedirectCoordinator.dispatch(callback(state = null)))
    assertTrue(RedirectCoordinator.isRejected(callback("forged")))

    assertFalse(pending.isCompleted)
    assertTrue(SSOService.hasPendingAuthentication())
    assertEquals(0, completeCalls.get())

    SSOService.cancelPendingAuthentication()
    assertTrue(withTimeout(TIMEOUT_MS) { pending.await() } is ClerkResult.Failure)
  }

  @Test
  fun `a duplicate callback joins the running completion`() = runBlocking {
    val pending = startSso(EXTERNAL_URL_A, state = STATE)
    val first = async(Dispatchers.Default) { RedirectCoordinator.dispatch(callback(STATE)) }
    withTimeout(TIMEOUT_MS) { completionStarted.await() }

    val second =
      async(start = CoroutineStart.UNDISPATCHED) { RedirectCoordinator.dispatch(callback(STATE)) }
    completionGate.complete(Unit)

    assertEquals(
      CallbackOutcome.Completed(success = true),
      withTimeout(TIMEOUT_MS) { first.await() },
    )
    assertEquals(
      CallbackOutcome.Completed(success = true),
      withTimeout(TIMEOUT_MS) { second.await() },
    )
    assertTrue(withTimeout(TIMEOUT_MS) { pending.await() } is ClerkResult.Success)
    assertEquals(1, completeCalls.get())
  }

  @Test
  fun `a duplicate callback after completion joins the finished result`() = runBlocking {
    val pending = startSso(EXTERNAL_URL_A, state = STATE)
    completionGate.complete(Unit)
    assertEquals(
      CallbackOutcome.Completed(success = true),
      RedirectCoordinator.dispatch(callback(STATE)),
    )
    assertTrue(withTimeout(TIMEOUT_MS) { pending.await() } is ClerkResult.Success)

    val repeat = RedirectCoordinator.dispatch(callback(STATE))

    assertEquals(CallbackOutcome.Completed(success = true), repeat)
    assertEquals(1, completeCalls.get())
    assertFalse(RedirectCoordinator.hasPendingRedirect.value)
  }

  @Test
  fun `completion survives the dispatching caller being cancelled`() = runBlocking {
    // The caller is an Activity's lifecycleScope; recreation cancels it mid-completion.
    val pending = startSso(EXTERNAL_URL_A, state = STATE)
    val destroyedActivity =
      launch(Dispatchers.Default) { RedirectCoordinator.dispatch(callback(STATE)) }
    withTimeout(TIMEOUT_MS) { completionStarted.await() }
    destroyedActivity.cancel()
    destroyedActivity.join()
    assertFalse(pending.isCompleted)

    val recreatedActivity =
      async(Dispatchers.Default) { RedirectCoordinator.dispatch(callback(STATE)) }
    completionGate.complete(Unit)

    assertEquals(
      CallbackOutcome.Completed(success = true),
      withTimeout(TIMEOUT_MS) { recreatedActivity.await() },
    )
    assertTrue(withTimeout(TIMEOUT_MS) { pending.await() } is ClerkResult.Success)
    assertEquals(1, completeCalls.get())
  }

  @Test
  fun `a completion that throws still completes the flow`() = runBlocking {
    coEvery { SSOService.completeRedirect(any(), any()) } throws CancellationException("boom")
    val pending = startSso(EXTERNAL_URL_A, state = STATE)

    val outcome = withTimeout(TIMEOUT_MS) { RedirectCoordinator.dispatch(callback(STATE)) }

    assertEquals(CallbackOutcome.Completed(success = false), outcome)
    val failure = withTimeout(TIMEOUT_MS) { pending.await() } as ClerkResult.Failure
    assertTrue(failure.throwable?.cause is CancellationException)
    assertFalse(RedirectCoordinator.hasPendingRedirect.value)
  }

  @Test
  fun `pending state clears as soon as the flow finishes`() = runBlocking {
    val pending = startSso(EXTERNAL_URL_A, state = STATE)
    assertTrue(RedirectCoordinator.hasPendingRedirect.value)
    val cleared =
      async(Dispatchers.Default) {
        RedirectCoordinator.hasPendingRedirect.first { !it }
      }

    completionGate.complete(Unit)
    RedirectCoordinator.dispatch(callback(STATE))

    assertFalse(withTimeout(TIMEOUT_MS) { cleared.await() })
    assertTrue(pending.await() is ClerkResult.Success)
  }

  @Test
  fun `a redirect prepared without state accepts any callback`() = runBlocking {
    val pending = startSso(EXTERNAL_URL_A, state = null)
    completionGate.complete(Unit)

    val outcome = RedirectCoordinator.dispatch(callback(state = null))

    assertEquals(CallbackOutcome.Completed(success = true), outcome)
    assertTrue(pending.await() is ClerkResult.Success)
    coVerify(exactly = 1) { SSOService.completeRedirect(any(), any()) }
  }

  @Test
  fun `while a flow is pending the receiver forwards only that flow's callback`() = runBlocking {
    val pending = startSso(EXTERNAL_URL_A, state = STATE)

    assertEquals(ReceiverDelivery.FORWARD, RedirectCoordinator.receiverDelivery(callback(STATE)))
    assertEquals(ReceiverDelivery.DROP, RedirectCoordinator.receiverDelivery(callback("forged")))
    assertEquals(ReceiverDelivery.DROP, RedirectCoordinator.receiverDelivery(callback(null)))
    assertEquals(
      ReceiverDelivery.COMPLETE_IN_BACKGROUND,
      RedirectCoordinator.receiverDelivery(MAGIC_LINK),
    )

    SSOService.cancelPendingAuthentication()
    assertTrue(withTimeout(TIMEOUT_MS) { pending.await() } is ClerkResult.Failure)
  }

  @Test
  fun `a callback for no flow is dropped while a hosted auth flow is pending`() {
    RedirectCoordinator.begin(
      PendingRedirect.HostedAuth("myapp://hosted", state = "s", codeVerifier = "v")
    )

    assertEquals(
      ReceiverDelivery.DROP,
      RedirectCoordinator.receiverDelivery(Uri.parse("clerk://com.example.app.callback")),
    )
    assertEquals(
      ReceiverDelivery.FORWARD,
      RedirectCoordinator.receiverDelivery(
        Uri.parse("myapp://hosted?state=s&rotating_token_nonce=n&created_session_id=sess_1")
      ),
    )

    RedirectCoordinator.cancelPending()
    assertEquals(
      ReceiverDelivery.FORWARD,
      RedirectCoordinator.receiverDelivery(Uri.parse("clerk://com.example.app.callback")),
    )
    assertEquals(ReceiverDelivery.FORWARD, RedirectCoordinator.receiverDelivery(MAGIC_LINK))
  }

  @Test
  fun `a finished flow without state does not claim the app's own deep links`() = runBlocking {
    val pending = startSso(EXTERNAL_URL_A, state = null)
    completionGate.complete(Unit)
    assertEquals(
      CallbackOutcome.Completed(success = true),
      RedirectCoordinator.dispatch(callback(state = null)),
    )
    assertTrue(withTimeout(TIMEOUT_MS) { pending.await() } is ClerkResult.Success)

    val appLink = RedirectCoordinator.dispatch(Uri.parse("myapp://orders?error=out_of_stock"))

    assertEquals(CallbackOutcome.NotHandled, appLink)
    assertEquals(1, completeCalls.get())
  }

  @Test
  fun `withState keeps the existing query and fragment`() {
    assertEquals(
      "clerk://a.callback?clerk_redirect_state=s",
      RedirectState.withState("clerk://a.callback", "s"),
    )
    assertEquals(
      "myapp://cb?x=1&clerk_redirect_state=s#frag",
      RedirectState.withState("myapp://cb?x=1#frag", "s"),
    )
    assertSame(null, RedirectState.take("never-prepared"))
  }

  /** Starts an SSO flow and returns once it holds the slot. */
  private suspend fun CoroutineScope.startSso(
    externalUrl: String,
    state: String? = STATE,
  ): Deferred<ClerkResult<OAuthResult, ClerkErrorResponse>> {
    val previous = RedirectCoordinator.current()
    state?.let { RedirectState.remember(externalUrl, it) }
    val started =
      async(Dispatchers.Default) { SSOService.authenticateWithPreparedRedirect(externalUrl) }
    waitUntil {
      RedirectCoordinator.current().let { it != null && it !== previous } || started.isCompleted
    }
    return started
  }

  private fun callback(state: String?): Uri =
    Uri.parse(
      buildString {
        append("clerk://com.example.app.callback?rotating_token_nonce=nonce")
        state?.let { append("&clerk_redirect_state=$it") }
      }
    )

  private suspend fun waitUntil(condition: () -> Boolean) {
    withTimeout(TIMEOUT_MS) {
      while (!condition()) delay(POLL_MS)
    }
  }

  private companion object {
    const val EXTERNAL_URL_A = "https://accounts.example.com/oauth/a"
    const val EXTERNAL_URL_B = "https://accounts.example.com/oauth/b"
    const val STATE = "state_123"
    val MAGIC_LINK: Uri = Uri.parse("myapp://email-link?flow_id=flow_123&approval_token=tok")
    const val TIMEOUT_MS = 5_000L
    const val POLL_MS = 5L
  }
}
