package com.clerk.api.sso

import com.clerk.api.Clerk
import java.net.URL

internal object RedirectConfiguration {
  private const val SCHEME = "clerk"
  private const val DEFAULT_HOST_SUFFIX = "callback"
  private const val LEGACY_HOST_SUFFIX = "oauth"
  private const val DEFAULT_HTTPS_PORT = 443

  val DEFAULT_REDIRECT_URL: String
    get() = buildRedirectUrl(DEFAULT_HOST_SUFFIX)

  /**
   * The legacy redirect URL used by previous SDK versions.
   *
   * The manifest still registers this host so in-flight or stale OAuth redirects from older app
   * versions continue to resolve after upgrading.
   */
  val LEGACY_REDIRECT_URL: String
    get() = buildRedirectUrl(LEGACY_HOST_SUFFIX)

  internal fun emailLinkRedirectUrl(
    applicationId: String,
    proxyUrl: String? = Clerk.proxyUrl,
  ): String {
    val portSuffix = resolveNonDefaultHttpsPort(proxyUrl)
    return "$SCHEME://$applicationId.$DEFAULT_HOST_SUFFIX$portSuffix"
  }

  private fun buildRedirectUrl(hostSuffix: String): String {
    return "$SCHEME://${Clerk.applicationId}.$hostSuffix"
  }

  private fun resolveNonDefaultHttpsPort(proxyUrl: String?): String {
    val parsedPort =
      proxyUrl?.takeUnless { it.isBlank() }?.let { runCatching { URL(it) }.getOrNull() }?.port
    val nonDefaultPort = parsedPort?.takeIf { it > 0 && it != DEFAULT_HTTPS_PORT }
    return nonDefaultPort?.let { ":$it" }.orEmpty()
  }
}
