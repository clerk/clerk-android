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
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    ExternalAccountService.cancelPendingExternalAccountConnection()
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `hasPendingExternalAccountConnection returns false initially`() {
    assertFalse(ExternalAccountService.hasPendingExternalAccountConnection())
  }

  @Test
  fun `cancelPendingExternalAccountConnection clears state`() {
    ExternalAccountService.cancelPendingExternalAccountConnection()

    assertFalse(ExternalAccountService.hasPendingExternalAccountConnection())
  }

  @Test
  fun `completeExternalConnection handles no pending connection gracefully`() = runTest {
    ExternalAccountService.completeExternalConnection()

    assertFalse(ExternalAccountService.hasPendingExternalAccountConnection())
  }

  @Test
  fun `completeExternalConnection handles missing external account`() = runTest {
    every { mockUser.externalAccounts } returns emptyList()
    coEvery { Client.get() } returns ClerkResult.Success(mockClient, emptyMap())

    ExternalAccountService.completeExternalConnection()

    assertFalse(ExternalAccountService.hasPendingExternalAccountConnection())
  }

  @Test
  fun `cancelPendingExternalAccountConnection completes with cancellation error`() {
    ExternalAccountService.cancelPendingExternalAccountConnection()

    assertFalse(ExternalAccountService.hasPendingExternalAccountConnection())
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

    ExternalAccountService.cancelPendingExternalAccountConnection()
    pendingResult.await()
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun `cancelling external connection completion propagates and fails pending connection`() =
    runTest {
      coEvery { mockUserApi.createExternalAccount(any(), "session_123") } returns
        ClerkResult.success(mockExternalAccount)
      val neverCompletes = CompletableDeferred<ClerkResult<Client, ClerkErrorResponse>>()
      coEvery { Client.get() } coAnswers { neverCompletes.await() }
      val pendingResult = async {
        ExternalAccountService.connectExternalAccount(
          User.CreateExternalAccountParams(provider = OAuthProvider.GOOGLE)
        )
      }
      runCurrent()
      assertTrue(ExternalAccountService.hasPendingExternalAccountConnection())

      val completionJob = launch { ExternalAccountService.completeExternalConnection() }
      runCurrent()
      assertTrue(completionJob.isActive)

      completionJob.cancel()
      completionJob.join()

      assertTrue(completionJob.isCancelled)
      assertFalse(ExternalAccountService.hasPendingExternalAccountConnection())
      val failure = pendingResult.await() as ClerkResult.Failure
      assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, failure.errorType)
      assertFalse(failure.throwable is SSOCancellationException)
      assertFalse(failure.throwable is CancellationException)
      assertTrue(failure.throwable?.cause is CancellationException)
    }
}
