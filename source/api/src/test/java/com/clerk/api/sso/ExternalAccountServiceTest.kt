package com.clerk.api.sso

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.clerk.api.Clerk
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.api.externalaccount.ExternalAccountService
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.UserApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.redirect.CallbackOutcome
import com.clerk.api.redirect.PendingRedirect
import com.clerk.api.redirect.RedirectCoordinator
import com.clerk.api.redirect.RedirectState
import com.clerk.api.session.Session
import com.clerk.api.user.User
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import java.lang.ref.WeakReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExternalAccountServiceTest {
  private val testDispatcher = StandardTestDispatcher()

  private lateinit var mockContext: Context
  private lateinit var mockUserApi: UserApi
  private lateinit var mockClient: Client
  private lateinit var mockSession: Session
  private lateinit var mockUser: User
  private lateinit var mockExternalAccount: ExternalAccount
  private lateinit var mockVerification: Verification

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    RedirectCoordinator.cancelPending()

    mockContext = mockk(relaxed = true)
    mockUserApi = mockk(relaxed = true)
    mockClient = mockk(relaxed = true)
    mockSession = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)
    mockExternalAccount = mockk(relaxed = true)
    mockVerification = mockk(relaxed = true)

    mockkObject(ClerkApi)
    mockkObject(Clerk)
    mockkStatic(Client::class)

    every { ClerkApi.user } returns mockUserApi
    every { Clerk.applicationContext } returns WeakReference(mockContext)
    every { Clerk.debugMode } returns false
    every { Clerk.session } returns mockSession
    every { Clerk.session?.id } returns "session_123"

    every { mockVerification.status } returns Verification.Status.VERIFIED
    every { mockVerification.externalVerificationRedirectUrl } returns
      "https://oauth.example.com/auth"
    every { mockExternalAccount.verification } returns mockVerification
    every { mockExternalAccount.id } returns "ext_account_123"

    every { mockClient.lastActiveSessionId } returns "session_123"
    every { mockClient.sessions } returns listOf(mockSession)
    every { mockSession.id } returns "session_123"
    every { mockSession.user } returns mockUser
    every { mockUser.externalAccounts } returns listOf(mockExternalAccount)
    coEvery { mockUserApi.createExternalAccount(capture(createFields), any()) } returns
      ClerkResult.success(mockExternalAccount)
  }

  private val createFields = slot<Map<String, String>>()

  private suspend fun connect() =
    ExternalAccountService.connectExternalAccount(
      User.CreateExternalAccountParams(provider = OAuthProvider.GOOGLE)
    )

  private suspend fun awaitPendingState(): String {
    waitUntil { (RedirectCoordinator.current() is PendingRedirect.ExternalAccountConnection) }
    return requireNotNull(
      Uri.parse(createFields.captured.getValue("redirect_url"))
        .getQueryParameter(RedirectState.QUERY_PARAMETER)
    )
  }

  private fun callback(state: String): Uri =
    Uri.parse("clerk://com.example.app.callback?${RedirectState.QUERY_PARAMETER}=$state")

  private suspend fun waitUntil(condition: () -> Boolean) {
    withTimeout(TIMEOUT_MS) {
      while (!condition()) delay(POLL_MS)
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    RedirectCoordinator.resetForTests()
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `callback with nothing pending is ignored`() = runTest {
    val outcome = RedirectCoordinator.dispatch(callback("anything"))

    assertEquals(CallbackOutcome.Ignored, outcome)
    assertFalse((RedirectCoordinator.current() is PendingRedirect.ExternalAccountConnection))
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun `connectExternalAccount launches authorization through non-exported manager`() = runTest {
    coEvery { mockUserApi.createExternalAccount(any(), "session_123") } returns
      ClerkResult.success(mockExternalAccount)
    val startedIntent = slot<Intent>()

    val pendingResult = async {
      ExternalAccountService.connectExternalAccount(
        User.CreateExternalAccountParams(provider = OAuthProvider.GOOGLE)
      )
    }
    runCurrent()

    verify(exactly = 1) { mockContext.startActivity(capture(startedIntent)) }
    assertEquals(SSOManagerActivity::class.java.name, startedIntent.captured.component?.className)
    assertNull(startedIntent.captured.data)
    assertEquals(
      mockVerification.externalVerificationRedirectUrl,
      startedIntent.captured.getStringExtra(SSOManagerActivity.URI_KEY),
    )
    assertTrue(startedIntent.captured.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)

    RedirectCoordinator.cancelPending()
    pendingResult.await()
  }

  @Test
  fun `callback completes pending connection when client refresh fails`() = runBlocking {
    val clientFailure =
      ClerkResult.apiFailure(
        ClerkErrorResponse(
          errors = listOf(Error(message = "client refresh failed", code = "client_error"))
        )
      )
    coEvery { Client.get() } returns clientFailure

    val pendingResult = async(Dispatchers.Default) { connect() }
    val state = awaitPendingState()

    RedirectCoordinator.dispatch(callback(state))

    val result = withTimeout(TIMEOUT_MS) { pendingResult.await() }
    assertSame(clientFailure, result)
    assertFalse((RedirectCoordinator.current() is PendingRedirect.ExternalAccountConnection))
  }

  @Test
  fun `callback completes pending connection with the verified account`() = runBlocking {
    coEvery { Client.get() } returns ClerkResult.success(mockClient)

    val pendingResult = async(Dispatchers.Default) { connect() }
    val state = awaitPendingState()

    RedirectCoordinator.dispatch(callback(state))

    val result = withTimeout(TIMEOUT_MS) { pendingResult.await() } as ClerkResult.Success
    assertSame(mockExternalAccount, result.value)
  }

  @Test
  fun `callback without the connection state does not end it`() = runBlocking {
    val pendingResult = async(Dispatchers.Default) { connect() }
    val state = awaitPendingState()

    assertEquals(CallbackOutcome.Ignored, RedirectCoordinator.dispatch(callback("forged")))
    assertEquals(
      CallbackOutcome.Ignored,
      RedirectCoordinator.dispatch(Uri.parse("clerk://com.example.app.callback?error=x")),
    )
    assertFalse(pendingResult.isCompleted)
    assertTrue((RedirectCoordinator.current() is PendingRedirect.ExternalAccountConnection))

    RedirectCoordinator.cancelPending()
    assertTrue(withTimeout(TIMEOUT_MS) { pendingResult.await() } is ClerkResult.Failure)
    assertTrue(state.isNotBlank())
  }

  @Test
  fun `completion keeps running when the dispatching caller is cancelled`() = runBlocking {
    val clientGate = CompletableDeferred<Unit>()
    coEvery { Client.get() } coAnswers
      {
        clientGate.await()
        ClerkResult.success(mockClient)
      }
    val pendingResult = async(Dispatchers.Default) { connect() }
    val state = awaitPendingState()

    val dispatcher = launch(Dispatchers.Default) { RedirectCoordinator.dispatch(callback(state)) }
    waitUntil { RedirectCoordinator.current()?.completionStarted?.get() == true }
    dispatcher.cancel()
    dispatcher.join()
    assertFalse(pendingResult.isCompleted)

    clientGate.complete(Unit)

    val result = withTimeout(TIMEOUT_MS) { pendingResult.await() } as ClerkResult.Success
    assertSame(mockExternalAccount, result.value)
    assertFalse((RedirectCoordinator.current() is PendingRedirect.ExternalAccountConnection))
  }

  @Test
  fun `starting SSO cancels a pending connection`() = runBlocking {
    val pendingResult = async(Dispatchers.Default) { connect() }
    awaitPendingState()

    RedirectCoordinator.supersedePending()

    val failure = withTimeout(TIMEOUT_MS) { pendingResult.await() } as ClerkResult.Failure
    assertTrue(failure.throwable is SSOCancellationException)
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun `starting a new connection completes the superseded pending connection`() = runTest {
    coEvery { mockUserApi.createExternalAccount(any(), "session_123") } returns
      ClerkResult.success(mockExternalAccount)
    val firstResult = async {
      ExternalAccountService.connectExternalAccount(
        User.CreateExternalAccountParams(provider = OAuthProvider.GOOGLE)
      )
    }
    runCurrent()
    assertTrue((RedirectCoordinator.current() is PendingRedirect.ExternalAccountConnection))

    val secondResult = async {
      ExternalAccountService.connectExternalAccount(
        User.CreateExternalAccountParams(provider = OAuthProvider.GITHUB)
      )
    }
    runCurrent()

    val failure = withTimeout(TIMEOUT_MS) { firstResult.await() } as ClerkResult.Failure
    assertTrue(failure.throwable is SSOCancellationException)
    assertTrue((RedirectCoordinator.current() is PendingRedirect.ExternalAccountConnection))

    RedirectCoordinator.cancelPending()
    withTimeout(TIMEOUT_MS) { secondResult.await() }
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun `cancelling the pending connection fails waiter with SSOCancellationException`() = runTest {
    coEvery { mockUserApi.createExternalAccount(any(), "session_123") } returns
      ClerkResult.success(mockExternalAccount)
    val pendingResult = async {
      ExternalAccountService.connectExternalAccount(
        User.CreateExternalAccountParams(provider = OAuthProvider.GOOGLE)
      )
    }
    runCurrent()

    RedirectCoordinator.cancelPending()

    val failure = withTimeout(TIMEOUT_MS) { pendingResult.await() } as ClerkResult.Failure
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, failure.errorType)
    assertTrue(failure.throwable is SSOCancellationException)
    assertFalse((RedirectCoordinator.current() is PendingRedirect.ExternalAccountConnection))
  }

  @Test
  fun `connectExternalAccount returns an HTTP failure for a status code of 600 or above`() =
    runTest {
      coEvery { mockUserApi.createExternalAccount(any(), "session_123") } returns
        ClerkResult.Failure(
          error = null,
          code = 600,
          errorType = ClerkResult.Failure.ErrorType.HTTP,
        )

      val failure =
        ExternalAccountService.connectExternalAccount(
          User.CreateExternalAccountParams(provider = OAuthProvider.GOOGLE)
        ) as ClerkResult.Failure

      assertEquals(ClerkResult.Failure.ErrorType.HTTP, failure.errorType)
      assertEquals(600, failure.code)
      verify(exactly = 0) { mockContext.startActivity(any()) }
    }

  @Test
  fun `connectExternalAccount maps an in-range HTTP failure`() = runTest {
    coEvery { mockUserApi.createExternalAccount(any(), "session_123") } returns
      ClerkResult.httpFailure(code = 503)

    val failure =
      ExternalAccountService.connectExternalAccount(
        User.CreateExternalAccountParams(provider = OAuthProvider.GOOGLE)
      ) as ClerkResult.Failure

    assertEquals(ClerkResult.Failure.ErrorType.HTTP, failure.errorType)
    assertEquals(503, failure.code)
  }

  private companion object {
    const val TIMEOUT_MS = 5_000L
    const val POLL_MS = 5L
  }
}
