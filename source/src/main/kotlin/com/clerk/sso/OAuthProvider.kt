package com.clerk.sso

import com.clerk.Clerk

/**
 * Enum class representing supported OAuth providers for authentication. Each provider is associated
 * with a specific strategy string used in Clerk API requests.
 *
 * @property [strategy] property holds the API-specific identifier for the provider.
 */
enum class OAuthProvider {
  FACEBOOK,
  GOOGLE,
  HUBSPOT,
  GITHUB,
  TIKTOK,
  GITLAB,
  DISCORD,
  TWITTER,
  TWITCH,
  LINKEDIN,
  LINKEDIN_OIDC,
  DROPBOX,
  ATLASSIAN,
  BITBUCKET,
  MICROSOFT,
  NOTION,
  APPLE,
  LINE,
  INSTAGRAM,
  COINBASE,
  SPOTIFY,
  XERO,
  BOX,
  SLACK,
  LINEAR,
  HUGGING_FACE,
  CUSTOM;

  companion object {
    /**
     * Convenience function to retrieve an OAuthProvider from its strategy string. Generally used to
     * take a [com.clerk.network.model.environment.UserSettings.SocialConfig.strategy] and convert
     * it to an OAuthProvider.
     *
     * @param [strategy] the strategy string to match against.
     * @return the corresponding OAuthProvider.
     * @throws [IllegalArgumentException] if no matching provider is found.
     */
    fun fromStrategy(strategy: String): OAuthProvider {
      return OAuthProvider.entries.find { it.providerData.strategy == strategy }
        ?: error("Unknown strategy")
    }
  }

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
        CUSTOM -> OAuthProviderData(provider = "", strategy = this.providerData.strategy, name = "")
      }
}

data class OAuthProviderData(val provider: String, val strategy: String, val name: String)

val OAuthProvider.providerName: String
  get() = this.providerData.name

/** Convenience function to get the logoUrl for the given [OAuthProvider] */
val OAuthProvider.logoUrl: String?
  get() =
    Clerk.environment.userSettings.social.values
      .find { it.strategy == this.providerData.strategy }
      ?.logoUrl
      ?.trim()
      ?.takeIf { it.isNotEmpty() }
