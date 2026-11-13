package com.clerk.api.sessiontransfer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.core.content.IntentCompat
import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.model.sessiontransfer.SessionTransferResource
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.LocalFailureCodes
import com.clerk.api.session.Session
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import java.lang.ref.WeakReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SessionTransferPresentersTest {
  private val sessionApi = mockk<SessionApi>()
  private val context =
    mockk<Context>(relaxed = true) { every { packageName } returns PACKAGE_NAME }

  @Before
  fun setup() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    mockkObject(Clerk)
    every { Clerk.activeSession } returns mockk<Session> { every { id } returns SESSION_ID }
    every { Clerk.applicationContext } returns WeakReference(context)
    mockkObject(ClerkApi)
    every { ClerkApi.session } returns sessionApi
    coEvery { sessionApi.createSessionTransfer(any(), any(), any(), any()) } returns
      ClerkResult.success(
        SessionTransferResource(
          objectType = "session_transfer",
          url = TRANSFER_URL,
          expiresAt = 1_700_000_060_000L,
        )
      )
  }

  @After
  fun tearDown() {
    unmockkAll()
    Dispatchers.resetMain()
  }

  @Test
  fun browserIntentOpensTheUrlInABrowserWithTheAppAsReferrer() {
    val url = Uri.parse(TRANSFER_URL)

    val intent = sessionTransferBrowserIntent(url, PACKAGE_NAME)

    assertEquals(Intent.ACTION_VIEW, intent.action)
    assertEquals(url, intent.data)
    assertTrue(intent.hasCategory(Intent.CATEGORY_BROWSABLE))
    assertEquals(
      Uri.parse("android-app://$PACKAGE_NAME"),
      IntentCompat.getParcelableExtra(intent, Intent.EXTRA_REFERRER, Uri::class.java),
    )
    assertFalse(intent.hasExtra(Intent.EXTRA_REFERRER_NAME))
    assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
  }

  @Test
  fun browserIntentResolvesOnlyAgainstBrowsers() {
    val selector = sessionTransferBrowserIntent(Uri.parse(TRANSFER_URL), PACKAGE_NAME).selector

    assertNotNull(selector)
    assertEquals(Intent.ACTION_VIEW, selector?.action)
    assertEquals(Uri.parse("https://"), selector?.data)
    assertEquals(setOf(Intent.CATEGORY_BROWSABLE), selector?.categories)
    assertNull(selector?.`package`)
    assertNull(selector?.component)
  }

  @Test
  fun openInBrowserStartsTheBrowserIntentForTheMintedUrl() = runBlocking {
    val intent = slot<Intent>()
    every { context.startActivity(capture(intent)) } returns Unit

    val result = SessionTransferService.openInBrowser(DESTINATION)

    assertTrue(result is ClerkResult.Success)
    assertEquals(Uri.parse(TRANSFER_URL), intent.captured.data)
    assertEquals(
      Uri.parse("android-app://$PACKAGE_NAME"),
      IntentCompat.getParcelableExtra(intent.captured, Intent.EXTRA_REFERRER, Uri::class.java),
    )
    coVerify(exactly = 1) {
      sessionApi.createSessionTransfer(SESSION_ID, DESTINATION, "browser", any())
    }
  }

  @Test
  fun openInBrowserFailsWhenNoBrowserIsInstalled() = runBlocking {
    every { context.startActivity(any()) } throws ActivityNotFoundException()

    val result = SessionTransferService.openInBrowser(DESTINATION)

    val failure = result as ClerkResult.Failure
    assertEquals(LocalFailureCodes.BROWSER_UNAVAILABLE, failure.error?.errors?.single()?.code)
  }

  @Test
  fun openInBrowserReportsALaunchTheSystemRefusesAsAnUnknownFailure() = runBlocking {
    val refusal = SecurityException("Permission Denial")
    every { context.startActivity(any()) } throws refusal

    val result = SessionTransferService.openInBrowser(DESTINATION)

    val failure = result as ClerkResult.Failure
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, failure.errorType)
    assertSame(refusal, failure.throwable)
  }

  @Test
  fun openInBrowserFailsBeforeAnyRequestWhenClerkIsNotInitialized() = runBlocking {
    every { Clerk.applicationContext } returns null

    val result = SessionTransferService.openInBrowser(DESTINATION)

    val failure = result as ClerkResult.Failure
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, failure.errorType)
    coVerify(exactly = 0) { sessionApi.createSessionTransfer(any(), any(), any(), any()) }
  }

  @Test
  fun openInBrowserSkipsTheBrowserWhenPreparationFails() = runBlocking {
    every { Clerk.activeSession } returns null

    val result = SessionTransferService.openInBrowser(DESTINATION)

    val failure = result as ClerkResult.Failure
    assertEquals(LocalFailureCodes.SIGNED_OUT, failure.error?.errors?.single()?.code)
    verify(exactly = 0) { context.startActivity(any()) }
  }

  @Test
  fun loadInWebViewLoadsTheMintedUrl() = runBlocking {
    val webView = mockk<WebView>(relaxed = true)

    val result = SessionTransferService.loadInWebView(webView, DESTINATION)

    assertTrue(result is ClerkResult.Success)
    verify(exactly = 1) { webView.loadUrl(TRANSFER_URL) }
    coVerify(exactly = 1) {
      sessionApi.createSessionTransfer(SESSION_ID, DESTINATION, "webview", any())
    }
  }

  @Test
  fun loadInWebViewSkipsLoadingWhenPreparationFails() = runBlocking {
    every { Clerk.activeSession } returns null
    val webView = mockk<WebView>(relaxed = true)

    val result = SessionTransferService.loadInWebView(webView, DESTINATION)

    assertTrue(result is ClerkResult.Failure)
    verify(exactly = 0) { webView.loadUrl(any<String>()) }
  }

  private companion object {
    const val SESSION_ID = "sess_123"
    const val PACKAGE_NAME = "com.example.app"
    const val DESTINATION = "https://example.com/disputes/new?transaction=streamly"
    const val TRANSFER_URL = "https://clerk.example.com/v1/session_transfers/redeem?token=abc"
  }
}
