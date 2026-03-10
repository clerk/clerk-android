package com.clerk.api.sso

import com.clerk.api.Clerk

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
  private const val SCHEME = "clerk"
  private const val DEFAULT_HOST_SUFFIX = "callback"
  private const val LEGACY_HOST_SUFFIX = "oauth"

  /**
   * The default redirect URL used for OAuth authentication flows.
   *
   * This URL is used as the redirect target when users complete OAuth authentication with external
   * providers. The custom scheme "clerk://" allows the application to handle the OAuth callback and
   * complete the authentication process.
   */
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

  private fun buildRedirectUrl(hostSuffix: String): String {
    return "$SCHEME://${Clerk.applicationId}.$hostSuffix"
  }
}
