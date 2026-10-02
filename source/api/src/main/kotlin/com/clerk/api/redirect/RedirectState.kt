package com.clerk.api.redirect

import android.net.Uri
import com.clerk.api.hostedauth.generateHostedAuthState

/**
 * Per-flow state for OAuth, enterprise SSO and external-account redirects.
 *
 * Clerk's OAuth state lives between FAPI and the provider and never reaches the app, so the SDK
 * adds its own: a random value appended to the `redirect_url` it sends. FAPI keeps the query of the
 * redirect URL when it redirects back (it appends `rotating_token_nonce` and friends with
 * `url.Values.Set`), and matches the redirect-URL allowlist without the query, so the value comes
 * back on the callback and nowhere else. A callback without it did not come from this flow.
 */
internal object RedirectState {
  const val QUERY_PARAMETER = "clerk_redirect_state"
  private const val MAX_PREPARED = 4

  private val lock = Any()

  /** States of redirects prepared but not yet launched, keyed by external verification URL. */
  private val prepared = LinkedHashMap<String, String>()

  fun generate(): String = generateHostedAuthState()

  /** Appends [state] to [redirectUrl], keeping any query and fragment it already has. */
  fun withState(redirectUrl: String, state: String): String {
    val fragmentStart = redirectUrl.indexOf('#').takeIf { it >= 0 } ?: redirectUrl.length
    val base = redirectUrl.substring(0, fragmentStart)
    val separator =
      when {
        !base.contains('?') -> "?"
        base.endsWith('?') || base.endsWith('&') -> ""
        else -> "&"
      }
    return "$base$separator$QUERY_PARAMETER=$state${redirectUrl.substring(fragmentStart)}"
  }

  /** Remembers the state of a prepared redirect until the flow for [externalUrl] launches. */
  fun remember(externalUrl: String, state: String) {
    synchronized(lock) {
      prepared.remove(externalUrl)
      prepared[externalUrl] = state
      while (prepared.size > MAX_PREPARED) {
        prepared.remove(prepared.keys.first())
      }
    }
  }

  /** Returns and forgets the state of the redirect prepared for [externalUrl], if any. */
  fun take(externalUrl: String): String? = synchronized(lock) { prepared.remove(externalUrl) }

  /** The state a callback carries, or `null` when it carries none or more than one. */
  fun of(uri: Uri): String? = runCatching {
    uri.getQueryParameters(QUERY_PARAMETER).singleOrNull()
  }
    .getOrNull()
    ?.takeIf { it.isNotBlank() }

  internal fun resetForTests() {
    synchronized(lock) { prepared.clear() }
  }
}
