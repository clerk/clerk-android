package com.clerk.api.sso

import com.clerk.api.Clerk

/**
 * Enum class representing supported OAuth providers for authentication.
 *
 * Each OAuth provider corresponds to a third-party authentication service that users can use to
 * sign in to your application. The enum provides a type-safe way to reference OAuth providers and
 * automatically handles the mapping to the appropriate strategy strings used in Clerk API requests.
 *
 * ## Supported Providers
 * The enum includes support for major OAuth providers such as:
 * - **Social platforms**: Facebook, Google, Twitter, Instagram, TikTok, Discord
 * - **Professional platforms**: LinkedIn, Microsoft, Slack
 * - **Developer platforms**: GitHub, GitLab, Bitbucket, Atlassian
 * - **Business platforms**: HubSpot, Notion, Dropbox, Box, Xero
 * - **Entertainment platforms**: Spotify, Twitch
 * - **AI platforms**: Hugging Face
 * - **Custom providers**: For enterprise or specialized OAuth implementations
 *
 * ## Usage
 * OAuth providers are typically used when configuring sign-in flows or when processing
 * authentication redirects from external services.
 *
 * ### Example usage:
 * ```kotlin
 * // Use with sign-in authentication
 * SignIn.authenticateWithRedirect(
 *   AuthenticateWithRedirectParams.OAuth(strategy = OAuthProvider.GOOGLE)
 * )
 *
 * // Convert from strategy string
 * val provider = OAuthProvider.fromStrategy("oauth_github")
 * ```
 *
 * @see [OAuthProviderData]
 */
@kotlinx.serialization.Serializable
enum class OAuthProvider {
  /** Facebook OAuth authentication provider. */
  FACEBOOK,

  /** Google OAuth authentication provider. */
  GOOGLE,

  /** HubSpot OAuth authentication provider. */
  HUBSPOT,

  /** GitHub OAuth authentication provider. */
  GITHUB,

  /** TikTok OAuth authentication provider. */
  TIKTOK,

  /** GitLab OAuth authentication provider. */
  GITLAB,

  /** Discord OAuth authentication provider. */
  DISCORD,

  /** Twitter OAuth authentication provider. */
  TWITTER,

  /** Twitch OAuth authentication provider. */
  TWITCH,

  /** LinkedIn OAuth authentication provider (legacy). */
  LINKEDIN,

  /** LinkedIn OpenID Connect authentication provider. */
  LINKEDIN_OIDC,

  /** Dropbox OAuth authentication provider. */
  DROPBOX,

  /** Atlassian OAuth authentication provider. */
  ATLASSIAN,

  /** Bitbucket OAuth authentication provider. */
  BITBUCKET,

  /** Microsoft OAuth authentication provider. */
  MICROSOFT,

  /** Notion OAuth authentication provider. */
  NOTION,

  /** Apple OAuth authentication provider. */
  APPLE,

  /** LINE OAuth authentication provider. */
  LINE,

  /** Instagram OAuth authentication provider. */
  INSTAGRAM,

  /** Coinbase OAuth authentication provider. */
  COINBASE,

  /** Spotify OAuth authentication provider. */
  SPOTIFY,

  /** Xero OAuth authentication provider. */
  XERO,

  /** Box OAuth authentication provider. */
  BOX,

  /** Slack OAuth authentication provider. */
  SLACK,

  /** Linear OAuth authentication provider. */
  LINEAR,

  /** Hugging Face OAuth authentication provider. */
  HUGGING_FACE,

  /** Custom OAuth authentication provider for enterprise or specialized implementations. */
  CUSTOM,

  /** Unknown OAuth provider - used as fallback for unrecognized providers. */
  UNKNOWN;

  companion object {
    /**
     * Converts a strategy string to the corresponding [OAuthProvider].
     *
     * This convenience function is primarily used to convert strategy strings from
     * [com.clerk.network.model.environment.UserSettings.SocialConfig.strategy] into type-safe
     * [OAuthProvider] enum values. This is useful when processing configuration data or API
     * responses that contain strategy strings.
     *
     * @param strategy The OAuth strategy string to convert (e.g., "oauth_google", "oauth_github").
     *   The strategy string should match one of the supported provider strategies. For custom OAuth
     *   providers, any strategy string starting with "oauth_" will return [CUSTOM]. Non-OAuth
     *   strategies will return [UNKNOWN].
     * @return The corresponding [OAuthProvider] enum value. Returns [CUSTOM] for unrecognized OAuth
     *   strategies and [UNKNOWN] for non-OAuth strategies.
     *
     * ### Example usage:
     * ```kotlin
     * val provider = OAuthProvider.fromStrategy("oauth_google") // Returns OAuthProvider.GOOGLE
     * val githubProvider = OAuthProvider.fromStrategy("oauth_github") // Returns OAuthProvider.GITHUB
     * ```
     */
    fun fromStrategy(strategy: String): OAuthProvider {
      return OAuthProvider.entries.find { it.providerData.strategy == strategy }
        ?: when {
          strategy.startsWith("oauth_") -> CUSTOM
          else -> UNKNOWN
        }
    }
  }

  /**
   * Internal property that provides the OAuth provider configuration data.
   *
   * This property returns the [OAuthProviderData] containing the provider identifier, strategy
   * string, and display name for each OAuth provider. The data is used internally by the Clerk SDK
   * to construct API requests and display provider information in the UI.
   *
   * The provider data includes:
   * - **provider**: The internal provider identifier used by Clerk
   * - **strategy**: The OAuth strategy string used in API requests
   * - **name**: The human-readable display name for the provider
   *
   * @see [OAuthProviderData]
   */
  internal val providerData: OAuthProviderData
    get() =
      when (this) {
        FACEBOOK ->
          OAuthProviderData(provider = "facebook", strategy = "oauth_facebook", name = "Facebook")
        GOOGLE -> OAuthProviderData(provider = "google", strategy = "oauth_google", name = "Google")
        HUBSPOT ->
          OAuthProviderData(provider = "hubspot", strategy = "oauth_hubspot", name = "HubSpot")
        GITHUB -> OAuthProviderData(provider = "github", strategy = "oauth_github", name = "GitHub")
        TIKTOK -> OAuthProviderData(provider = "tiktok", strategy = "oauth_tiktok", name = "TikTok")
        GITLAB -> OAuthProviderData(provider = "gitlab", strategy = "oauth_gitlab", name = "GitLab")
        DISCORD ->
          OAuthProviderData(provider = "discord", strategy = "oauth_discord", name = "Discord")
        TWITTER ->
          OAuthProviderData(provider = "twitter", strategy = "oauth_twitter", name = "Twitter")
        TWITCH -> OAuthProviderData(provider = "twitch", strategy = "oauth_twitch", name = "Twitch")
        LINKEDIN ->
          OAuthProviderData(provider = "linkedin", strategy = "oauth_linkedin", name = "LinkedIn")
        LINKEDIN_OIDC ->
          OAuthProviderData(
            provider = "linkedin_oidc",
            strategy = "oauth_linkedin_oidc",
            name = "LinkedIn",
          )
        DROPBOX ->
          OAuthProviderData(provider = "dropbox", strategy = "oauth_dropbox", name = "Dropbox")
        ATLASSIAN ->
          OAuthProviderData(
            provider = "atlassian",
            strategy = "oauth_atlassian",
            name = "Atlassian",
          )
        BITBUCKET ->
          OAuthProviderData(
            provider = "bitbucket",
            strategy = "oauth_bitbucket",
            name = "Bitbucket",
          )
        MICROSOFT ->
          OAuthProviderData(
            provider = "microsoft",
            strategy = "oauth_microsoft",
            name = "Microsoft",
          )
        NOTION -> OAuthProviderData(provider = "notion", strategy = "oauth_notion", name = "Notion")
        APPLE -> OAuthProviderData(provider = "apple", strategy = "oauth_apple", name = "Apple")
        LINE -> OAuthProviderData(provider = "line", strategy = "oauth_line", name = "LINE")
        INSTAGRAM ->
          OAuthProviderData(
            provider = "instagram",
            strategy = "oauth_instagram",
            name = "Instagram",
          )
        COINBASE ->
          OAuthProviderData(provider = "coinbase", strategy = "oauth_coinbase", name = "Coinbase")
        SPOTIFY ->
          OAuthProviderData(provider = "spotify", strategy = "oauth_spotify", name = "Spotify")
        XERO -> OAuthProviderData(provider = "xero", strategy = "oauth_xero", name = "Xero")
        BOX -> OAuthProviderData(provider = "box", strategy = "oauth_box", name = "Box")
        SLACK -> OAuthProviderData(provider = "slack", strategy = "oauth_slack", name = "Slack")
        LINEAR -> OAuthProviderData(provider = "linear", strategy = "oauth_linear", name = "Linear")
        HUGGING_FACE ->
          OAuthProviderData(
            provider = "huggingface",
            strategy = "oauth_huggingface",
            name = "Hugging Face",
          )
        CUSTOM -> OAuthProviderData(provider = "custom", strategy = "oauth_custom", name = "Custom")
        UNKNOWN ->
          OAuthProviderData(provider = "unknown", strategy = "oauth_unknown", name = "Unknown")
      }
}

/**
 * Data class containing OAuth provider configuration information.
 *
 * This class holds the essential information needed to identify and interact with an OAuth
 * provider, including internal identifiers, API strategy strings, and user-facing display names.
 *
 * @property provider The internal provider identifier used by Clerk's backend services. This is
 *   typically a lowercase string that uniquely identifies the OAuth provider.
 * @property strategy The OAuth strategy string used in Clerk API requests and responses. This
 *   follows the pattern "oauth_{provider}" (e.g., "oauth_google", "oauth_github").
 * @property name The human-readable display name for the OAuth provider. This is used in user
 *   interfaces and error messages.
 */
data class OAuthProviderData(val provider: String, val strategy: String, val name: String)

/**
 * Extension property to get the human-readable name of the OAuth provider.
 *
 * This property provides a convenient way to access the display name of an OAuth provider without
 * directly accessing the internal [providerData] property.
 *
 * @return The human-readable name of the OAuth provider (e.g., "Google", "GitHub", "Facebook").
 *
 * ### Example usage:
 * ```kotlin
 * val provider = OAuthProvider.GOOGLE
 * val displayName = provider.providerName // Returns "Google"
 * ```
 */
val OAuthProvider.providerName: String
  get() = this.providerData.name

/**
 * Extension property to get the logo URL for the OAuth provider.
 *
 * This property retrieves the logo URL for the OAuth provider from the Clerk environment
 * configuration. The logo URL can be used to display provider logos in authentication UIs. The URL
 * is automatically trimmed of whitespace and validated to ensure it's not empty.
 *
 * @return The logo URL for the OAuth provider, or `null` if no logo URL is configured or available
 *   in the current environment settings.
 *
 * ### Example usage:
 * ```kotlin
 * val provider = OAuthProvider.GOOGLE
 * val logoUrl = provider.logoUrl // Returns the Google logo URL or null
 *
 * // Use in UI
 * logoUrl?.let { url ->
 *   // Load and display the logo image
 * }
 * ```
 */
val OAuthProvider.logoUrl: String?
  get() =
    Clerk.environment.userSettings.social.values
      .find { it.strategy == this.providerData.strategy }
      ?.logoUrl
      ?.trim()
      ?.takeIf { it.isNotEmpty() }
