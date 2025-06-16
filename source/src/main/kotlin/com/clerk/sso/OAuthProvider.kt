package com.clerk.sso
/**
 * Enum class representing supported OAuth providers for authentication. Each provider is associated
 * with a specific strategy string used in Clerk API requests.
 *
 * @property [strategy] property holds the API-specific identifier for the provider.
 */
enum class OAuthProvider(val strategy: String) {
  FACEBOOK("oauth_facebook"),
  GOOGLE("oauth_google"),
  HUBSPOT("oauth_hubspot"),
  GITHUB("oauth_github"),
  TIKTOK("oauth_tiktok"),
  GITLAB("oauth_gitlab"),
  DISCORD("oauth_discord"),
  TWITTER("oauth_twitter"),
  TWITCH("oauth_twitch"),
  LINKEDIN("oauth_linkedin"),
  LINKEDIN_OIDC("oauth_linkedin_oidc"),
  DROPBOX("oauth_dropbox"),
  ATLASSIAN("oauth_atlassian"),
  BITBUCKET("oauth_bitbucket"),
  MICROSOFT("oauth_microsoft"),
  NOTION("oauth_notion"),
  APPLE("oauth_apple"),
  LINE("oauth_line"),
  INSTAGRAM("oauth_instagram"),
  COINBASE("oauth_coinbase"),
  SPOTIFY("oauth_spotify"),
  XERO("oauth_xero"),
  BOX("oauth_box"),
  SLACK("oauth_slack"),
  LINEAR("oauth_linear"),
  HUGGING_FACE("oauth_hugging_face"),
  CUSTOM("oauth_custom");

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
      return OAuthProvider.entries.find { it.strategy == strategy } ?: error("Unknown strategy")
    }
  }
}
