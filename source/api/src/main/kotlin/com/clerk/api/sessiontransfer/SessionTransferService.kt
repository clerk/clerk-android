package com.clerk.api.sessiontransfer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.core.net.toUri
import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.sessiontransfer.SessionTransferResource
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.LocalFailureCodes
import com.clerk.api.network.serialization.localFailure
import com.clerk.api.network.serialization.map
import com.clerk.api.network.serialization.suspendingFlatMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object SessionTransferService {
  suspend fun prepare(
    destination: String,
    presentation: SessionTransferPresentation,
  ): ClerkResult<SessionTransfer, ClerkErrorResponse> {
    val sessionId = Clerk.activeSession?.id
    return when {
      !isWebUrl(destination) ->
        localFailure(
          code = LocalFailureCodes.INVALID_ARGUMENTS,
          longMessage = "A session transfer destination must be an absolute http or https URL.",
        )
      sessionId == null ->
        localFailure(
          code = LocalFailureCodes.SIGNED_OUT,
          longMessage = "A session transfer requires an active session.",
        )
      else -> create(sessionId, destination, presentation)
    }
  }

  private suspend fun create(
    sessionId: String,
    destination: String,
    presentation: SessionTransferPresentation,
  ): ClerkResult<SessionTransfer, ClerkErrorResponse> =
    when (
      val result =
        ClerkApi.session.createSessionTransfer(
          sessionId = sessionId,
          redirectUrl = destination,
          presentationMode = presentation.value,
        )
    ) {
      is ClerkResult.Failure -> result
      is ClerkResult.Success ->
        result.value.toSessionTransfer()?.let { ClerkResult.success(it) }
          ?: ClerkResult.unknownFailure(
            IllegalStateException("Session transfer creation returned an invalid response.")
          )
    }

  suspend fun openInBrowser(
    destination: String,
    context: Context,
  ): ClerkResult<Unit, ClerkErrorResponse> =
    prepare(destination, SessionTransferPresentation.BROWSER).suspendingFlatMap { transfer ->
      withContext(Dispatchers.Main) {
        try {
          context.startActivity(sessionTransferBrowserIntent(transfer.url, context.packageName))
          ClerkResult.success(Unit)
        } catch (_: ActivityNotFoundException) {
          localFailure(
            code = LocalFailureCodes.BROWSER_UNAVAILABLE,
            longMessage = "No browser is available to open the session transfer.",
          )
        }
      }
    }

  suspend fun loadInWebView(
    webView: WebView,
    destination: String,
  ): ClerkResult<Unit, ClerkErrorResponse> =
    prepare(destination, SessionTransferPresentation.WEB_VIEW).map { transfer ->
      withContext(Dispatchers.Main) { webView.loadUrl(transfer.url.toString()) }
    }
}

internal fun sessionTransferBrowserIntent(url: Uri, packageName: String): Intent =
  Intent(Intent.ACTION_VIEW, url).apply {
    addCategory(Intent.CATEGORY_BROWSABLE)
    // Chrome treats an app-opened navigation as cross-site and sends the
    // `android-app://<package>` Referer that FAPI requires only when the intent names the app.
    putExtra(Intent.EXTRA_REFERRER, "android-app://$packageName".toUri())
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    // Android resolves the intent against the selector, and only a browser handles a bare https
    // URL, so an app that claims the transfer URL's host through App Links is never offered it.
    selector = Intent(Intent.ACTION_VIEW, "https://".toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
  }

private fun isWebUrl(value: String): Boolean {
  val uri = value.toUri()
  val scheme = uri.scheme
  return (scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)) &&
    !uri.host.isNullOrBlank()
}

internal fun SessionTransferResource.toSessionTransfer(): SessionTransfer? {
  if (objectType != SESSION_TRANSFER_OBJECT) return null
  return runCatching { url.toUri() }
    .getOrNull()
    ?.takeIf { uri ->
      uri.isHierarchical &&
        !uri.host.isNullOrBlank() &&
        uri.userInfo == null &&
        uri.scheme.equals("https", ignoreCase = true)
    }
    ?.let { uri -> SessionTransfer(url = uri, expiresAt = expiresAt) }
}

private const val SESSION_TRANSFER_OBJECT = "session_transfer"
