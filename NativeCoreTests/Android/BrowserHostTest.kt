package com.clerk.api

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserHostTest {
  @Test
  fun idleCallbacksAndCancellationDoNotCreatePendingWork() = fixture { _, callback ->
    check(!BrowserRequests.matches(Uri.parse(callback)))
    BrowserRequests.finish("absent", Uri.parse(callback))
    BrowserRequests.cancel("absent")
    check(BrowserRequests.pending == null)
  }

  @Test
  fun launchesExplicitNonExportedManagerWithOpaqueRequestId() = fixture { presenter, callback ->
    val pending =
      async(start = CoroutineStart.UNDISPATCHED) {
        BrowserAuthentication { presenter }.open(authorization, callback)
      }
    val request = checkNotNull(BrowserRequests.pending)
    val intent = presenter.started.single()
    check(intent.component?.className == CoreBrowserActivity::class.java.name)
    check(intent.component?.packageName == presenter.packageName)
    check(!presenter.packageManager.getActivityInfo(checkNotNull(intent.component), 0).exported)
    check(intent.data == null && intent.getStringExtra("url") == authorization)
    check(intent.getStringExtra("request") == request.id && request.id.isNotBlank())
    check(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK == 0)
    val returned = "$callback?rotating_token_nonce=native_browser_nonce"
    BrowserRequests.finish(request.id, Uri.parse(returned))
    check(pending.await().jsonObject["callbackUrl"] == JsonPrimitive(returned))
    check(BrowserRequests.pending == null)
  }

  @Test
  fun userCancellationIsTypedAndAllowsAnotherPresentation() = fixture { presenter, callback ->
    val host = BrowserAuthentication { presenter }
    val first =
      async(start = CoroutineStart.UNDISPATCHED) {
        runCatching { host.open(authorization, callback) }
      }
    val firstId = checkNotNull(BrowserRequests.pending).id
    BrowserRequests.cancel(firstId)
    check((first.await().exceptionOrNull() as? CoreException)?.code == "user_cancelled")
    check(BrowserRequests.pending == null)
    val second = async(start = CoroutineStart.UNDISPATCHED) { host.open(authorization, callback) }
    val secondId = checkNotNull(BrowserRequests.pending).id
    check(secondId != firstId)
    BrowserRequests.cancel(firstId)
    BrowserRequests.finish(firstId, Uri.parse(callback))
    check(BrowserRequests.pending?.id == secondId && !second.isCompleted)
    BrowserRequests.finish(secondId, Uri.parse(callback))
    second.await()
    check(presenter.started.size == 2 && BrowserRequests.pending == null)
  }

  @Test
  fun coroutineCancellationReleasesThePendingPresentation() = fixture { presenter, callback ->
    val pending =
      async(start = CoroutineStart.UNDISPATCHED) {
        BrowserAuthentication { presenter }.open(authorization, callback)
      }
    val id = checkNotNull(BrowserRequests.pending).id
    pending.cancelAndJoin()
    withTimeout(1000) { while (BrowserRequests.pending != null) delay(10) }
    BrowserRequests.finish(id, Uri.parse(callback))
    check(pending.isCancelled && BrowserRequests.pending == null)
  }

  @Test
  fun concurrentPresentationDoesNotReplaceTheFirstRequest() = fixture { presenter, callback ->
    val host = BrowserAuthentication { presenter }
    val first = async(start = CoroutineStart.UNDISPATCHED) { host.open(authorization, callback) }
    val id = checkNotNull(BrowserRequests.pending).id
    val failure = runCatching { host.open(authorization, callback) }.exceptionOrNull()
    check((failure as? CoreException)?.code == "presentation_in_progress")
    check(BrowserRequests.pending?.id == id && presenter.started.size == 1)
    BrowserRequests.finish(id, Uri.parse(callback))
    first.await()
  }

  @Test
  fun mismatchedAndDuplicateCallbacksCannotCompleteAnotherRequest() =
    fixture { presenter, callback ->
      val pending =
        async(start = CoroutineStart.UNDISPATCHED) {
          BrowserAuthentication { presenter }.open(authorization, callback)
        }
      val id = checkNotNull(BrowserRequests.pending).id
      for (url in
        listOf(
          "other://oauth/callback",
          callback.replace("oauth/", "other/"),
          "$callback/extra",
          callback.replace("://oauth", "://user@oauth"),
          callback.replace("://oauth", "://oauth:123"),
        )) {
        BrowserRequests.finish(id, Uri.parse(url))
        check(BrowserRequests.pending?.id == id && !pending.isCompleted)
      }
      val returned = "$callback?error=access_denied"
      BrowserRequests.finish(id, Uri.parse(returned))
      check(pending.await().jsonObject["callbackUrl"] == JsonPrimitive(returned))
      BrowserRequests.finish(id, Uri.parse(callback))
      BrowserRequests.cancel(id)
      check(BrowserRequests.pending == null)
    }

  @Test
  fun unavailableOrInvalidPresentationFailsBeforeLaunching() = fixture { presenter, callback ->
    check(
      (runCatching { BrowserAuthentication { null }.open(authorization, callback) }
          .exceptionOrNull() as? CoreException)
        ?.code == "presentation_unavailable"
    )
    val host = BrowserAuthentication { presenter }
    for (url in
      listOf(
        "http://provider.example/auth",
        "https://user@provider.example/auth",
        "javascript:alert(1)",
      )) check(
      (runCatching { host.open(url, callback) }.exceptionOrNull() as? CoreException)?.code ==
        "invalid_browser_url"
    )
    for (url in
      listOf(
        "http://oauth/callback",
        "$callback#fragment",
        callback.replace("://oauth", "://user@oauth"),
      )) check(
      (runCatching { host.open(authorization, url) }.exceptionOrNull() as? CoreException)?.code ==
        "invalid_callback_url"
    )
    check(
      (runCatching { host.open(authorization, "unregistered://oauth/callback") }.exceptionOrNull()
          as? CoreException)
        ?.code == "callback_not_registered"
    )
    check(presenter.started.isEmpty() && BrowserRequests.pending == null)
  }

  @Test
  fun failedManagerLaunchClearsPendingState() = fixture { presenter, callback ->
    presenter.failLaunch = true
    val failure =
      runCatching { BrowserAuthentication { presenter }.open(authorization, callback) }
        .exceptionOrNull()
    check((failure as? CoreException)?.code == "browser_unavailable")
    check(BrowserRequests.pending == null)
  }

  private val authorization = "https://provider.example/authorize"

  // Exercise framework Intent/PackageManager behavior on Android while intercepting presentation.
  // Real browser activity lifecycle and live authorization remain separate integration gates.
  private fun fixture(verify: suspend CoroutineScope.(CapturingActivity, String) -> Unit) =
    runBlocking {
      withTimeout(10000) {
        withContext(Dispatchers.Main.immediate) {
          check(BrowserRequests.pending == null)
          val context = InstrumentationRegistry.getInstrumentation().targetContext
          val presenter = CapturingActivity(context)
          try {
            verify(presenter, "${context.packageName}.clerk://oauth/callback")
          } finally {
            BrowserRequests.pending?.let { BrowserRequests.cancel(it.id) }
          }
        }
      }
    }
}

private class CapturingActivity(context: Context) : Activity() {
  init {
    attachBaseContext(context)
  }

  val started = mutableListOf<Intent>()
  var failLaunch = false

  override fun startActivity(intent: Intent) {
    if (failLaunch) throw ActivityNotFoundException("Fixture manager launch unavailable")
    started += intent
  }
}
