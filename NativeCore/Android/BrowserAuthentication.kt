package com.clerk.api

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import java.lang.ref.WeakReference
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

public class BrowserAuthentication(private val activity: () -> Activity?) {
  public suspend fun open(url: String, callbackUrl: String): JsonElement = withContext(Dispatchers.Main.immediate) {
    val presenter = activity()?.takeUnless { it.isFinishing || it.isDestroyed } ?: throw CoreException("presentation_unavailable")
    val target = Uri.parse(url)
    val callback = Uri.parse(callbackUrl)
    if (target.scheme != "https" || target.host.isNullOrEmpty() || target.userInfo != null) throw CoreException("invalid_browser_url")
    if (callback.scheme in setOf(null, "http", "javascript", "data", "file", "about") || callback.host.isNullOrEmpty() || callback.fragment != null || callback.userInfo != null) throw CoreException("invalid_callback_url")
    val route = Intent(Intent.ACTION_VIEW, callback).addCategory(Intent.CATEGORY_BROWSABLE).setPackage(presenter.packageName)
    if (route.resolveActivity(presenter.packageManager) == null) throw CoreException("callback_not_registered")
    suspendCancellableCoroutine { continuation ->
      if (BrowserRequests.pending != null) { continuation.resumeWithException(CoreException("presentation_in_progress")); return@suspendCancellableCoroutine }
      val id = UUID.randomUUID().toString()
      BrowserRequests.pending = BrowserRequests.Pending(id, callback, continuation)
      continuation.invokeOnCancellation { presenter.runOnUiThread { BrowserRequests.cancel(id, notify = false) } }
      try {
        presenter.startActivity(Intent(presenter, CoreBrowserActivity::class.java).putExtra("request", id).putExtra("url", url))
      } catch (_: ActivityNotFoundException) { BrowserRequests.cancel(id, code = "browser_unavailable") }
    }
  }
}

internal object BrowserRequests {
  data class Pending(val id: String, val callback: Uri, val continuation: CancellableContinuation<JsonElement>, var activity: WeakReference<Activity>? = null)
  var pending: Pending? = null
  fun matches(uri: Uri): Boolean {
    val expected = pending?.callback ?: return false
    return uri.scheme == expected.scheme && uri.host == expected.host && uri.port == expected.port && uri.path == expected.path && uri.userInfo == null
  }
  fun finish(id: String, callback: Uri) {
    val request = pending?.takeIf { it.id == id && matches(callback) } ?: return
    pending = null
    if (request.continuation.isActive) request.continuation.resume(buildJsonObject { put("callbackUrl", callback.toString()) })
  }
  fun cancel(id: String, code: String = "user_cancelled", notify: Boolean = true) {
    val request = pending?.takeIf { it.id == id } ?: return
    pending = null
    request.activity?.get()?.finish()
    if (notify && request.continuation.isActive) request.continuation.resumeWithException(CoreException(code))
  }
}

/** Only presents and returns a browser callback. Clerk interprets that callback in TypeScript. */
public class CoreBrowserActivity : Activity() {
  private var started = false
  private var request: String? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    request = savedInstanceState?.getString("request") ?: intent.getStringExtra("request")
    started = savedInstanceState?.getBoolean("started") ?: false
    val pending = BrowserRequests.pending?.takeIf { it.id == request } ?: run { finish(); return }
    pending.activity = WeakReference(this)
  }
  override fun onResume() {
    super.onResume()
    val id = request ?: return
    if (BrowserRequests.pending?.id != id) { finish(); return }
    val callback = intent.data
    if (callback != null && BrowserRequests.matches(callback)) {
      BrowserRequests.finish(id, callback); finish(); return
    }
    if (started) { BrowserRequests.cancel(id); finish(); return }
    val url = intent.getStringExtra("url") ?: run { BrowserRequests.cancel(id, "invalid_browser_url"); return }
    try {
      started = true
      CustomTabsIntent.Builder().build().launchUrl(this, Uri.parse(url))
    } catch (_: ActivityNotFoundException) { BrowserRequests.cancel(id, "browser_unavailable"); finish() }
  }
  override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent) }
  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState); outState.putBoolean("started", started); outState.putString("request", request)
  }
}

public class CoreBrowserCallbackActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val callback = intent?.data
    if (callback != null && BrowserRequests.matches(callback)) {
      startActivity(Intent(this, CoreBrowserActivity::class.java).setData(callback)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
    }
    finish()
  }
}
