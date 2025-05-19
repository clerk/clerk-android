package com.clerk.sdk.model.signin

import kotlinx.serialization.Serializable

/** A type that represents the OAuth provider. */
@Serializable
sealed class OAuthProvider : Comparable<OAuthProvider> {
  object Facebook : OAuthProvider()

  object Google : OAuthProvider()

  object Hubspot : OAuthProvider()

  object Github : OAuthProvider()

  object Tiktok : OAuthProvider()

  object Gitlab : OAuthProvider()

  object Discord : OAuthProvider()

  object Twitter : OAuthProvider()

  object Twitch : OAuthProvider()

  object Linkedin : OAuthProvider()

  object LinkedinOidc : OAuthProvider()

  object Dropbox : OAuthProvider()

  object Atlassian : OAuthProvider()

  object Bitbucket : OAuthProvider()

  object Microsoft : OAuthProvider()

  object Notion : OAuthProvider()

  object Apple : OAuthProvider()

  object Line : OAuthProvider()

  object Instagram : OAuthProvider()

  object Coinbase : OAuthProvider()

  object Spotify : OAuthProvider()

  object Xero : OAuthProvider()

  object Box : OAuthProvider()

  object Slack : OAuthProvider()

  object Linear : OAuthProvider()

  object HuggingFace : OAuthProvider()

  companion object {
    /** All available OAuth providers. Note: Custom providers are not included in this list. */
    val allCases: List<OAuthProvider> =
      listOf(
        Facebook,
        Google,
        Hubspot,
        Github,
        Tiktok,
        Gitlab,
        Discord,
        Twitter,
        Twitch,
        Linkedin,
        LinkedinOidc,
        Dropbox,
        Atlassian,
        Bitbucket,
        Microsoft,
        Notion,
        Apple,
        Line,
        Instagram,
        Coinbase,
        Spotify,
        Xero,
        Box,
        Slack,
        Linear,
        HuggingFace,
      )
  }

  private data class OAuthProviderData(
    val provider: String,
    val strategy: String,
    val name: String,
  )

  private val providerData: OAuthProviderData
    get() =
      when (this) {
        Facebook ->
          OAuthProviderData(provider = "facebook", strategy = "oauth_facebook", name = "Facebook")
        Google -> OAuthProviderData(provider = "google", strategy = "oauth_google", name = "Google")
        Hubspot ->
          OAuthProviderData(provider = "hubspot", strategy = "oauth_hubspot", name = "HubSpot")
        Github -> OAuthProviderData(provider = "github", strategy = "oauth_github", name = "GitHub")
        Tiktok -> OAuthProviderData(provider = "tiktok", strategy = "oauth_tiktok", name = "TikTok")
        Gitlab -> OAuthProviderData(provider = "gitlab", strategy = "oauth_gitlab", name = "GitLab")
        Discord ->
          OAuthProviderData(provider = "discord", strategy = "oauth_discord", name = "Discord")
        Twitter ->
          OAuthProviderData(provider = "twitter", strategy = "oauth_twitter", name = "Twitter")
        Twitch -> OAuthProviderData(provider = "twitch", strategy = "oauth_twitch", name = "Twitch")
        Linkedin ->
          OAuthProviderData(provider = "linkedin", strategy = "oauth_linkedin", name = "LinkedIn")
        LinkedinOidc ->
          OAuthProviderData(
            provider = "linkedin_oidc",
            strategy = "oauth_linkedin_oidc",
            name = "LinkedIn",
          )
        Dropbox ->
          OAuthProviderData(provider = "dropbox", strategy = "oauth_dropbox", name = "Dropbox")
        Atlassian ->
          OAuthProviderData(
            provider = "atlassian",
            strategy = "oauth_atlassian",
            name = "Atlassian",
          )
        Bitbucket ->
          OAuthProviderData(
            provider = "bitbucket",
            strategy = "oauth_bitbucket",
            name = "Bitbucket",
          )
        Microsoft ->
          OAuthProviderData(
            provider = "microsoft",
            strategy = "oauth_microsoft",
            name = "Microsoft",
          )
        Notion -> OAuthProviderData(provider = "notion", strategy = "oauth_notion", name = "Notion")
        Apple -> OAuthProviderData(provider = "apple", strategy = "oauth_apple", name = "Apple")
        Line -> OAuthProviderData(provider = "line", strategy = "oauth_line", name = "LINE")
        Instagram ->
          OAuthProviderData(
            provider = "instagram",
            strategy = "oauth_instagram",
            name = "Instagram",
          )
        Coinbase ->
          OAuthProviderData(provider = "coinbase", strategy = "oauth_coinbase", name = "Coinbase")
        Spotify ->
          OAuthProviderData(provider = "spotify", strategy = "oauth_spotify", name = "Spotify")
        Xero -> OAuthProviderData(provider = "xero", strategy = "oauth_xero", name = "Xero")
        Box -> OAuthProviderData(provider = "box", strategy = "oauth_box", name = "Box")
        Slack -> OAuthProviderData(provider = "slack", strategy = "oauth_slack", name = "Slack")
        Linear -> OAuthProviderData(provider = "linear", strategy = "oauth_linear", name = "Linear")
        HuggingFace ->
          OAuthProviderData(
            provider = "huggingface",
            strategy = "oauth_huggingface",
            name = "Hugging Face",
          )
      }

  override fun compareTo(other: OAuthProvider): Int {
    val lhsName = providerData.name
    val rhsName = other.providerData.name

    return when {
      lhsName.isEmpty() && rhsName.isEmpty() -> 0
      lhsName.isEmpty() -> -1
      rhsName.isEmpty() -> 1
      else -> lhsName.compareTo(rhsName)
    }
  }
}
