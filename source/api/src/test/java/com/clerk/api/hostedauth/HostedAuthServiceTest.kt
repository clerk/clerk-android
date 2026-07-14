package com.clerk.api.hostedauth

import android.content.Context
import android.net.Uri
import com.clerk.api.Clerk
import com.clerk.api.externalaccount.ExternalAccountService
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.ClientApi
import com.clerk.api.network.middleware.ManualClientSyncRequest
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.hostedauth.HostedAuthResource
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.sso.SSOService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HostedAuthServiceTest {
  private val clientApi = mockk<ClientApi>()

  @Before
  fun setup() {
    mockkObject(Clerk)
    every { Clerk.applicationContext } returns WeakReference(mockk<Context>(relaxed = true))
    every { Clerk.isClientResponseCurrent(any(), any()) } returns true
    justRun { Clerk.updateClient(any()) }
    mockkObject(ClerkApi)
    every { ClerkApi.client } returns clientApi
    mockkObject(SSOService)
    justRun { SSOService.cancelPendingAuthentication() }
    mockkObject(ExternalAccountService)
    justRun { ExternalAccountService.cancelPendingExternalAccountConnection() }
  }

  @After
  fun tearDown() {
    HostedAuthService.cancelPendingAuthentication()
    unmockkAll()
  }

  @Test
  fun startRejectsSecondConcurrentFlow() = runBlocking {
    val capturedState = stubCreateHostedAuth()
    val first = startInBackground()
    withTimeout(TIMEOUT_MS) { capturedState.await() }

    val second = HostedAuthService.start(mode = null, redirectUrl = REDIRECT_URL)

    assertTrue(second is ClerkResult.Failure)
    val message = (second as ClerkResult.Failure).throwable?.message.orEmpty()
    assertTrue(message.contains("already in progress"))

    HostedAuthService.cancelPendingAuthentication()
    assertTrue(withTimeout(TIMEOUT_MS) { first.await() } is ClerkResult.Failure)
  }

  @Test
  fun cancelWhileBrowserOpenFailsTheFlow() = runBlocking {
    val capturedState = stubCreateHostedAuth()
    val start = startInBackground()
    withTimeout(TIMEOUT_MS) { capturedState.await() }

    HostedAuthService.cancelPendingAuthentication()

    val result = withTimeout(TIMEOUT_MS) { start.await() }
    assertTrue(result is ClerkResult.Failure)
    assertFalse(HostedAuthService.hasPendingAuthentication())
  }

  @Test
  fun concurrentCompleteCallsRedeemExactlyOnce() = runBlocking {
    val capturedState = stubCreateHostedAuth()
    val redeemCalls = AtomicInteger(0)
    val redeemGate = CompletableDeferred<Unit>()
    coEvery { clientApi.redeemHostedAuth(any(), any(), any(), any(), any()) } coAnswers
      {
        redeemCalls.incrementAndGet()
        redeemGate.await()
        arg<ManualClientSyncRequest>(3).recordResponse(null, null)
        ClerkResult.success(redeemedClient())
      }
    val start = startInBackground()
    val callbackUri = legitimateCallback(withTimeout(TIMEOUT_MS) { capturedState.await() })

    val firstComplete = async(Dispatchers.Default) { HostedAuthService.complete(callbackUri) }
    waitUntil { redeemCalls.get() == 1 }
    // UNDISPATCHED runs the duplicate delivery up to its first suspension point while the
    // original completion still holds the redemption gate, guaranteeing true concurrency.
    val secondComplete =
      async(start = CoroutineStart.UNDISPATCHED) { HostedAuthService.complete(callbackUri) }
    redeemGate.complete(Unit)

    val results =
      withTimeout(TIMEOUT_MS) {
        listOf(firstComplete.await(), secondComplete.await(), start.await())
      }
    results.forEach { result ->
      assertTrue(result is ClerkResult.Success)
      assertEquals(SESSION_ID, (result as ClerkResult.Success).value.id)
    }
    assertEquals(1, redeemCalls.get())
  }

  @Test
  fun forgedCallbackDoesNotCompletePendingFlow() = runBlocking {
    val capturedState = stubCreateHostedAuth()
    stubRedeemHostedAuth(redeemedClient())
    val start = startInBackground()
    val state = withTimeout(TIMEOUT_MS) { capturedState.await() }

    val forgedUri =
      Uri.parse(
        "$REDIRECT_URL?state=forged&rotating_token_nonce=nonce_forged&created_session_id=sess_forged"
      )
    assertFalse(HostedAuthService.isValidCallback(forgedUri))
    assertTrue(HostedAuthService.isValidCallback(legitimateCallback(state)))
    val forgedResult = HostedAuthService.complete(forgedUri)

    assertTrue(forgedResult is ClerkResult.Failure)
    assertTrue(HostedAuthService.hasPendingAuthentication())
    assertFalse(start.isCompleted)

    val legitimateResult =
      withTimeout(TIMEOUT_MS) { HostedAuthService.complete(legitimateCallback(state)) }

    assertTrue(legitimateResult is ClerkResult.Success)
    assertEquals(SESSION_ID, (legitimateResult as ClerkResult.Success).value.id)
    val startResult = withTimeout(TIMEOUT_MS) { start.await() }
    assertEquals(SESSION_ID, (startResult as ClerkResult.Success).value.id)
  }

  @Test
  fun completeAppliesRedeemedClientLocally() = runBlocking {
    val capturedState = stubCreateHostedAuth()
    val client = redeemedClient()
    stubRedeemHostedAuth(client)
    val start = startInBackground()
    val callbackUri = legitimateCallback(withTimeout(TIMEOUT_MS) { capturedState.await() })

    val result = withTimeout(TIMEOUT_MS) { HostedAuthService.complete(callbackUri) }

    assertTrue(result is ClerkResult.Success)
    verify(exactly = 1) { Clerk.updateClient(client) }
    withTimeout(TIMEOUT_MS) { start.await() }
    Unit
  }

  @Test
  fun completeAppliesClientButFailsWhenCreatedSessionIsMissing() = runBlocking {
    val capturedState = stubCreateHostedAuth()
    val clientWithoutSession = Client(id = "client_123", sessions = emptyList())
    stubRedeemHostedAuth(clientWithoutSession)
    val start = startInBackground()
    val callbackUri = legitimateCallback(withTimeout(TIMEOUT_MS) { capturedState.await() })

    val result = withTimeout(TIMEOUT_MS) { HostedAuthService.complete(callbackUri) }

    assertTrue(result is ClerkResult.Failure)
    val message = (result as ClerkResult.Failure).throwable?.message.orEmpty()
    assertTrue(message.contains("did not include the created session"))
    verify(exactly = 1) { Clerk.updateClient(clientWithoutSession) }
    assertTrue(withTimeout(TIMEOUT_MS) { start.await() } is ClerkResult.Failure)
  }

  @Test
  fun completeRejectsStaleRedeemedClient() = runBlocking {
    val capturedState = stubCreateHostedAuth()
    val client = redeemedClient()
    every { Clerk.isClientResponseCurrent(any(), any()) } returns false
    stubRedeemHostedAuth(client)
    val start = startInBackground()
    val callbackUri = legitimateCallback(withTimeout(TIMEOUT_MS) { capturedState.await() })

    val result = withTimeout(TIMEOUT_MS) { HostedAuthService.complete(callbackUri) }

    assertTrue(result is ClerkResult.Failure)
    val message = (result as ClerkResult.Failure).throwable?.message.orEmpty()
    assertTrue(message.contains("no longer current"))
    verify(exactly = 0) { Clerk.updateClient(client) }
    assertTrue(withTimeout(TIMEOUT_MS) { start.await() } is ClerkResult.Failure)
  }

  private fun stubCreateHostedAuth(): CompletableDeferred<String> {
    val capturedState = CompletableDeferred<String>()
    coEvery { clientApi.createHostedAuth(any(), any(), any(), any(), any(), any()) } answers
      {
        capturedState.complete(thirdArg())
        ClerkResult.success(
          HostedAuthResource(objectType = "hosted_auth", url = "https://portal.dev/start")
        )
      }
    return capturedState
  }

  private fun stubRedeemHostedAuth(client: Client) {
    coEvery { clientApi.redeemHostedAuth(any(), any(), any(), any(), any()) } coAnswers
      {
        arg<ManualClientSyncRequest>(3).recordResponse(null, null)
        ClerkResult.success(client)
      }
  }

  private fun CoroutineScope.startInBackground():
    Deferred<ClerkResult<Session, ClerkErrorResponse>> =
    async(Dispatchers.Default) { HostedAuthService.start(mode = null, redirectUrl = REDIRECT_URL) }

  private fun legitimateCallback(state: String): Uri =
    Uri.parse(
      "$REDIRECT_URL?state=$state&rotating_token_nonce=nonce_123&created_session_id=$SESSION_ID"
    )

  private fun redeemedClient(): Client =
    Client(
      id = "client_123",
      sessions = listOf(session(SESSION_ID)),
      lastActiveSessionId = SESSION_ID,
    )

  private fun session(id: String): Session =
    Session(
      id = id,
      expireAt = 10_000L,
      lastActiveAt = 1_000L,
      createdAt = 1_000L,
      updatedAt = 1_000L,
    )

  private suspend fun waitUntil(condition: () -> Boolean) {
    withTimeout(TIMEOUT_MS) {
      while (!condition()) {
        delay(POLL_INTERVAL_MS)
      }
    }
  }

  private companion object {
    const val REDIRECT_URL = "clerk://com.example.app.callback"
    const val SESSION_ID = "sess_123"
    const val TIMEOUT_MS = 5_000L
    const val POLL_INTERVAL_MS = 10L
  }
}
