package com.clerk.api.sso

import com.clerk.api.Clerk
import java.net.URL

/**
 * Internal configuration object for OAuth redirect URLs.
 *
 * This object contains default configuration values used for OAuth authentication flows,
 * particularly the redirect URLs that OAuth providers use to return users to the application after
 * authentication.
 *
 * This is an internal utility and should not be used outside the Clerk SDK.
 */
internal object RedirectConfiguration {
  /**
   * The default redirect URL used for OAuth authentication flows.
   *
   * This URL is used as the redirect target when users complete OAuth authentication with external
   * providers. The custom scheme "clerk://" allows the application to handle the OAuth callback and
   * complete the authentication process.
   */
  val DEFAULT_REDIRECT_URL: String = "clerk://${Clerk.applicationId}.oauth"

  internal fun emailLinkRedirectUrl(
    applicationId: String,
    proxyUrl: String? = Clerk.proxyUrl,
  ): String {
    val portSuffix = resolveNonDefaultHttpsPort(proxyUrl)
    return "clerk://$applicationId.oauth$portSuffix"
  }

  private fun resolveNonDefaultHttpsPort(proxyUrl: String?): String {
    val parsedPort =
      proxyUrl?.takeUnless { it.isBlank() }?.let { runCatching { URL(it) }.getOrNull() }?.port
    val nonDefaultPort = parsedPort?.takeIf { it > 0 && it != DEFAULT_HTTPS_PORT }
    return nonDefaultPort?.let { ":$it" }.orEmpty()
  }

  private const val DEFAULT_HTTPS_PORT = 443
}
