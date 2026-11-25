package com.clerk.e2e

import android.net.Uri
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import java.io.IOException
import java.net.URL
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val SIGNED_IN_USER_ID =
  "window.Clerk?.loaded ? (window.Clerk.user?.id ?? null) : false"
private const val CLERK_NOT_LOADED = "false"
private const val ENVIRONMENT_TIMEOUT_MS = 15_000
private val USER_READ_INTERVAL = 200.milliseconds

private sealed interface WebSession {
  data object Loading : WebSession

  data class SignedIn(val userId: String) : WebSession

  data object SignedOut : WebSession

  data class Failed(val code: String) : WebSession
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NativeToWeb(frontendApiUrl: String?, onDone: () -> Unit) {
  val scope = rememberCoroutineScope()
  val page = remember { SessionTransferPage(scope, frontendApiUrl) }

  MaterialTheme {
    Surface(modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
      Column(modifier = Modifier.safeDrawingPadding()) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Status(page.webSession, Modifier.weight(1f))
          TextAction("Done", E2ETags.NativeToWeb.DONE, onDone)
        }
        AndroidView(
          factory = { context -> WebView(context).also(page::start) },
          modifier = Modifier.fillMaxWidth().weight(1f),
          onRelease = WebView::destroy,
        )
      }
    }
  }
}

@Composable
private fun Status(webSession: WebSession, modifier: Modifier) {
  ProvideTextStyle(MaterialTheme.typography.bodySmall) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      when (webSession) {
        WebSession.Loading -> Text("Loading the Account Portal")
        is WebSession.SignedIn ->
          LabelledId("Web user ID", webSession.userId, E2ETags.NativeToWeb.WEB_USER_ID)
        WebSession.SignedOut -> Text("Signed out on the web")
        is WebSession.Failed -> {
          Text("Transfer failed")
          Text(
            webSession.code,
            modifier = Modifier.testTag(E2ETags.NativeToWeb.ERROR),
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    }
  }
}

private class SessionTransferPage(
  private val scope: CoroutineScope,
  private val frontendApiUrl: String?,
) : WebViewClient() {
  var webSession: WebSession by mutableStateOf(WebSession.Loading)
    private set

  private var userRead: Job? = null

  fun start(webView: WebView) {
    webView.settings.javaScriptEnabled = true
    webView.webViewClient = this
    // On the emulator's software GPU, a hardware-accelerated WebView with no page leaves the whole
    // window blank, which hides the transfer's error.
    webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    scope.launch {
      clearWebData()
      val destination =
        frontendApiUrl?.let { accountPortalUserPage(it) } ?: return@launch fail("no_destination")
      val result = Clerk.auth.loadInWebView(webView, destination)
      if (result is ClerkResult.Failure) {
        fail(result.error?.errors?.firstOrNull()?.code ?: "load_failed")
      }
    }
  }

  override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
    sessionTransferFailureCode(request.url)?.let(::fail)
    return false
  }

  override fun onPageFinished(view: WebView, url: String?) {
    if (webSession is WebSession.Failed) return
    userRead?.cancel()
    userRead = scope.launch { webSession = signedInUser(view) }
  }

  private fun fail(code: String) {
    userRead?.cancel()
    webSession = WebSession.Failed(code)
  }
}

private suspend fun clearWebData() {
  WebStorage.getInstance().deleteAllData()
  suspendCancellableCoroutine { continuation ->
    CookieManager.getInstance().removeAllCookies { continuation.resume(Unit) }
  }
}

private suspend fun accountPortalUserPage(frontendApiUrl: String): String? =
  withContext(Dispatchers.IO) {
    val environment =
      try {
        val connection = URL("$frontendApiUrl/v1/environment").openConnection()
        connection.connectTimeout = ENVIRONMENT_TIMEOUT_MS
        connection.readTimeout = ENVIRONMENT_TIMEOUT_MS
        connection.getInputStream().bufferedReader().use { it.readText() }
      } catch (_: IOException) {
        return@withContext null
      }
    val homeUrl =
      try {
        Json.parseToJsonElement(environment)
          .jsonObject["display_config"]
          ?.jsonObject
          ?.get("home_url")
          ?.jsonPrimitive
          ?.contentOrNull
      } catch (_: IllegalArgumentException) {
        null
      } ?: return@withContext null
    Uri.parse(homeUrl).buildUpon().path("/user").clearQuery().fragment(null).build().toString()
  }

private suspend fun signedInUser(webView: WebView): WebSession {
  var result = webView.evaluate(SIGNED_IN_USER_ID)
  while (result == CLERK_NOT_LOADED) {
    delay(USER_READ_INTERVAL)
    result = webView.evaluate(SIGNED_IN_USER_ID)
  }
  val userId = Json.parseToJsonElement(result).jsonPrimitive.takeIf { it.isString }?.content
  return userId?.let(WebSession::SignedIn) ?: WebSession.SignedOut
}

private suspend fun WebView.evaluate(script: String): String =
  suspendCancellableCoroutine { continuation ->
    evaluateJavascript(script) { continuation.resume(it) }
  }

private fun sessionTransferFailureCode(url: Uri): String? =
  url
    .takeIf {
      it.isHierarchical && it.getQueryParameter("__clerk_status") == "session_transfer_failed"
    }
    ?.let { it.getQueryParameter("__clerk_error_code") ?: "unknown" }
