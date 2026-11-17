package com.clerk.api.sessiontransfer

import android.content.Context
import android.webkit.WebView
import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.sessiontransfer.SessionTransferResource
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import java.lang.ref.WeakReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SessionTransferAccountSwitchTest {
  private val sessionApi = mockk<SessionApi>()
  private val context =
    mockk<Context>(relaxed = true) {
      every { packageName } returns "com.example.app"
    }
  private val webView = mockk<WebView>(relaxed = true)
  private val entered = CompletableDeferred<Unit>()
  private val release = CompletableDeferred<Unit>()
  private val client =
    Client(
      id = "client_switch",
      sessions = listOf(session("sess_account_a"), session("sess_account_b")),
      lastActiveSessionId = "sess_account_a",
    )

  @Before
  fun setup() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    Clerk.stateStore.reset()
    Clerk.updateClient(client)
    Clerk.applicationContext = WeakReference(context)
    mockkObject(ClerkApi)
    every { ClerkApi.session } returns sessionApi
    coEvery { sessionApi.createSessionTransfer("sess_account_a", any(), any(), any()) } coAnswers
      {
        entered.complete(Unit)
        release.await()
        ClerkResult.success(
          SessionTransferResource(
            objectType = "session_transfer",
            url = "https://clerk.example.invalid/v1/session_transfers/redeem?token=synthetic",
            expiresAt = 4_000_000_000_000L,
          )
        )
      }
  }

  @After
  fun tearDown() {
    unmockkAll()
    Clerk.applicationContext = null
    Clerk.stateStore.reset()
    Dispatchers.resetMain()
  }

  @Test
  fun prepareRejectsTransferForAnAccountThatIsNoLongerSelected() = runBlocking {
    switchedResult {
      SessionTransferService.prepare(
        "https://example.invalid/",
        SessionTransferPresentation.BROWSER,
      )
    }
      .let {
        assertTrue(
          "An account switch must invalidate the returned transfer",
          it is ClerkResult.Failure,
        )
      }
  }

  @Test
  fun browserDoesNotOpenThePreviousAccountsTransfer() = runBlocking {
    val result = switchedResult { SessionTransferService.openInBrowser("https://example.invalid/") }
    verify(exactly = 0) { context.startActivity(any()) }
    assertTrue(result is ClerkResult.Failure)
  }

  @Test
  fun webViewDoesNotLoadThePreviousAccountsTransfer() = runBlocking {
    val result = switchedResult {
      SessionTransferService.loadInWebView(webView, "https://example.invalid/")
    }
    verify(exactly = 0) { webView.loadUrl(any<String>()) }
    assertTrue(result is ClerkResult.Failure)
  }

  @Test
  fun browserDoesNotOpenATransferQueuedBeforeTheAccountSwitch() = runBlocking {
    val result = switchedWhileQueuedResult {
      SessionTransferService.openInBrowser("https://example.invalid/")
    }
    verify(exactly = 0) { context.startActivity(any()) }
    assertEquals("session_changed", result.failureCode())
  }

  @Test
  fun webViewDoesNotLoadATransferQueuedBeforeTheAccountSwitch() = runBlocking {
    val result = switchedWhileQueuedResult {
      SessionTransferService.loadInWebView(webView, "https://example.invalid/")
    }
    verify(exactly = 0) { webView.loadUrl(any<String>()) }
    assertEquals("session_changed", result.failureCode())
  }

  @Test
  fun prepareReportsSignedOutWhenTheUserSignsOutDuringTheRequest() = runBlocking {
    val result = coroutineScope {
      val pending = async {
        SessionTransferService.prepare(
          "https://example.invalid/",
          SessionTransferPresentation.BROWSER,
        )
      }
      entered.await()
      Clerk.updateClient(Client())
      release.complete(Unit)
      pending.await()
    }
    assertEquals("signed_out", result.failureCode())
  }

  @Test
  fun browserReportsSignedOutWhenTheUserSignsOutBeforeItOpens() = runBlocking {
    val result = coroutineScope {
      val main = StandardTestDispatcher()
      Dispatchers.setMain(main)
      val pending = async { SessionTransferService.openInBrowser("https://example.invalid/") }
      entered.await()
      release.complete(Unit)
      yield()
      Clerk.updateClient(Client())
      main.scheduler.advanceUntilIdle()
      pending.await()
    }
    verify(exactly = 0) { context.startActivity(any()) }
    assertEquals("signed_out", result.failureCode())
  }

  private suspend fun <T> switchedWhileQueuedResult(action: suspend () -> T): T = coroutineScope {
    val main = StandardTestDispatcher()
    Dispatchers.setMain(main)
    val pending = async { action() }
    entered.await()
    release.complete(Unit)
    yield()
    assertEquals("sess_account_a", Clerk.activeSession?.id)
    Clerk.updateClient(client.copy(lastActiveSessionId = "sess_account_b"))
    main.scheduler.advanceUntilIdle()
    pending.await()
  }

  private fun ClerkResult<*, ClerkErrorResponse>.failureCode(): String? =
    (this as? ClerkResult.Failure)?.error?.errors?.firstOrNull()?.code

  private suspend fun <T> switchedResult(action: suspend () -> T): T = coroutineScope {
    assertEquals("sess_account_a", Clerk.activeSession?.id)
    val pending = async { action() }
    entered.await()
    Clerk.updateClient(client.copy(lastActiveSessionId = "sess_account_b"))
    assertEquals("sess_account_b", Clerk.activeSession?.id)
    release.complete(Unit)
    pending.await().also { assertEquals("sess_account_b", Clerk.activeSession?.id) }
  }

  private fun session(id: String) =
    Session(
      id = id,
      status = Session.SessionStatus.ACTIVE,
      expireAt = 4_000_000_000_000L,
      lastActiveAt = 1_000L,
      createdAt = 1_000L,
      updatedAt = 1_000L,
    )
}
