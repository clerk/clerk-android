package com.clerk.api.sso

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
  const val DEFAULT_REDIRECT_URL: String = "clerk://oauth"
}
