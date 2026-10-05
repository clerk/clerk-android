package com.clerk.api.sso

import android.content.Context
import android.content.Intent
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
import com.clerk.api.session.Session
import com.clerk.api.user.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import java.lang.ref.WeakReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
    ExternalAccountService.cancelPendingExternalAccountConnection()

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

    coEvery { mockUserApi.createExternalAccount(any(), "session_123") } returns
      ClerkResult.success(mockExternalAccount)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    ExternalAccountService.cancelPendingExternalAccountConnection()
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `completeExternalConnection without pending connection makes no requests`() = runTest {
    ExternalAccountService.completeExternalConnection()

    assertFalse(ExternalAccountService.hasPendingExternalAccountConnection())
    coVerify(exactly = 0) { Client.get() }
  }

  @Test
  fun `completeExternalConnection resolves pending connection with verified account`() = runTest {
    coEvery { Client.get() } returns ClerkResult.success(mockClient)
    val pendingResult = startPendingConnection()

    ExternalAccountService.completeExternalConnection()

    val result = pendingResult.await() as ClerkResult.Success
    assertSame(mockExternalAccount, result.value)
    assertFalse(ExternalAccountService.hasPendingExternalAccountConnection())
  }

  @Test
  fun `completeExternalConnection fails pending connection when account is missing`() = runTest {
    every { mockUser.externalAccounts } returns emptyList()
    coEvery { Client.get() } returns ClerkResult.success(mockClient)
    val pendingResult = startPendingConnection()

    ExternalAccountService.completeExternalConnection()

    val failure = pendingResult.await() as ClerkResult.Failure
    assertEquals("External account not found for ID: ext_account_123", failure.throwable?.message)
    assertFalse(ExternalAccountService.hasPendingExternalAccountConnection())
  }

  @Test
  fun `completeExternalConnection fails pending connection when account is not verified`() =
    runTest {
      coEvery { Client.get() } returns ClerkResult.success(mockClient)
      val pendingResult = startPendingConnection()
      every { mockVerification.status } returns Verification.Status.UNVERIFIED

      ExternalAccountService.completeExternalConnection()

      val failure = pendingResult.await() as ClerkResult.Failure
      assertTrue(
        failure.throwable?.message.orEmpty().startsWith("External account verification failed")
      )
      assertFalse(ExternalAccountService.hasPendingExternalAccountConnection())
    }

  @Test
  fun `cancelPendingExternalAccountConnection completes pending connection with cancellation error`() =
    runTest {
      val pendingResult = startPendingConnection()

      ExternalAccountService.cancelPendingExternalAccountConnection()

      val failure = pendingResult.await() as ClerkResult.Failure
      assertTrue(failure.throwable is SSOCancellationException)
      assertEquals("External account connection cancelled", failure.throwable?.message)
      assertFalse(ExternalAccountService.hasPendingExternalAccountConnection())
    }

  @Test
  fun `connectExternalAccount launches authorization through non-exported manager`() = runTest {
    val startedIntent = slot<Intent>()

    val pendingResult = startPendingConnection()

    verify(exactly = 1) { mockContext.startActivity(capture(startedIntent)) }
    assertEquals(SSOManagerActivity::class.java.name, startedIntent.captured.component?.className)
    assertNull(startedIntent.captured.data)
    assertEquals(
      mockVerification.externalVerificationRedirectUrl,
      startedIntent.captured.getStringExtra(SSOManagerActivity.URI_KEY),
    )
    assertTrue(startedIntent.captured.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)

    ExternalAccountService.cancelPendingExternalAccountConnection()
    pendingResult.await()
  }

  @Test
  fun `cancellation thrown while completing external connection is rethrown`() = runTest {
    coEvery { Client.get() } throws CancellationException("caller cancelled")
    val pendingResult = startPendingConnection()

    val thrown = runCatching {
      ExternalAccountService.completeExternalConnection()
    }
      .exceptionOrNull()

    assertTrue(thrown is CancellationException)
    assertFalse(ExternalAccountService.hasPendingExternalAccountConnection())
    assertInterrupted(pendingResult.await())
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun `completeExternalConnection completes pending connection when client refresh fails`() =
    runTest {
      coEvery { mockUserApi.createExternalAccount(any(), "session_123") } returns
        ClerkResult.success(mockExternalAccount)
      val clientFailure =
        ClerkResult.apiFailure(
          ClerkErrorResponse(
            errors = listOf(Error(message = "client refresh failed", code = "client_error"))
          )
        )
      coEvery { Client.get() } returns clientFailure

      val pendingResult = async {
        ExternalAccountService.connectExternalAccount(
          User.CreateExternalAccountParams(provider = OAuthProvider.GOOGLE)
        )
      }
      runCurrent()
      assertTrue(ExternalAccountService.hasPendingExternalAccountConnection())

      ExternalAccountService.completeExternalConnection()

      val result = withTimeout(TIMEOUT_MS) { pendingResult.await() }
      assertSame(clientFailure, result)
      assertFalse(ExternalAccountService.hasPendingExternalAccountConnection())
    }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun `cancelling external connection completion propagates and fails pending connection`() =
    runTest {
      val neverCompletes = CompletableDeferred<ClerkResult<Client, ClerkErrorResponse>>()
      coEvery { Client.get() } coAnswers { neverCompletes.await() }
      val pendingResult = startPendingConnection()

      val completionJob = launch { ExternalAccountService.completeExternalConnection() }
      runCurrent()
      assertTrue(completionJob.isActive)

      completionJob.cancel()
      completionJob.join()

      assertTrue(completionJob.isCancelled)
      assertFalse(ExternalAccountService.hasPendingExternalAccountConnection())
      assertInterrupted(pendingResult.await())
    }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun `starting a new connection cancels the superseded pending connection`() = runTest {
    val firstResult = startPendingConnection()

    val secondResult = startPendingConnection()

    val failure = firstResult.await() as ClerkResult.Failure
    assertTrue(failure.throwable is SSOCancellationException)
    assertFalse(secondResult.isCompleted)

    ExternalAccountService.cancelPendingExternalAccountConnection()
    secondResult.await()
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun `stale completion does not clear a newer external account connection`() = runTest {
    val clientResponse = CompletableDeferred<ClerkResult<Client, ClerkErrorResponse>>()
    coEvery { Client.get() } coAnswers { clientResponse.await() }
    val firstResult = startPendingConnection()
    val staleCompletion = launch { ExternalAccountService.completeExternalConnection() }
    runCurrent()

    val secondResult = startPendingConnection()
    clientResponse.complete(ClerkResult.success(mockClient))
    staleCompletion.join()

    val superseded = firstResult.await() as ClerkResult.Failure
    assertTrue(superseded.throwable is SSOCancellationException)
    assertTrue(ExternalAccountService.hasPendingExternalAccountConnection())
    assertFalse(secondResult.isCompleted)

    ExternalAccountService.cancelPendingExternalAccountConnection()
    assertTrue(secondResult.await() is ClerkResult.Failure)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun TestScope.startPendingConnection():
    Deferred<ClerkResult<ExternalAccount, ClerkErrorResponse>> {
    val pendingResult = async {
      ExternalAccountService.connectExternalAccount(
        User.CreateExternalAccountParams(provider = OAuthProvider.GOOGLE)
      )
    }
    runCurrent()
    assertTrue(ExternalAccountService.hasPendingExternalAccountConnection())
    return pendingResult
  }

  private fun assertInterrupted(result: ClerkResult<ExternalAccount, ClerkErrorResponse>) {
    val failure = result as ClerkResult.Failure
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, failure.errorType)
    assertFalse(failure.throwable is SSOCancellationException)
    assertFalse(failure.throwable is CancellationException)
    assertTrue(failure.throwable?.cause is CancellationException)
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
  }
}
