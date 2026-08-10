package com.clerk.api.sso

import androidx.annotation.VisibleForTesting
import com.clerk.api.Clerk
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Value representing an OAuth provider strategy for authentication.
 *
 * Each OAuth provider corresponds to a third-party authentication service that users can use to
 * sign in to your application. Built-in providers are available as constants, while custom
 * providers retain their complete strategy string so it can be sent to Clerk without losing the
 * provider key.
 *
 * ## Supported Providers
 * The type includes support for major OAuth providers such as:
 * - **Social platforms**: Facebook, Google, Twitter, Instagram, TikTok, Discord
 * - **Professional platforms**: LinkedIn, Microsoft, Slack
 * - **Developer platforms**: GitHub, GitLab, Bitbucket, Atlassian, Vercel
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
 * val customProvider = OAuthProvider.custom("oauth_custom_patreon")
 * ```
 *
 * @see [OAuthProviderData]
 */
@kotlinx.serialization.Serializable(with = OAuthProviderSerializer::class)
@ConsistentCopyVisibility
data class OAuthProvider private constructor(val strategy: String) {
  companion object {
    /** Facebook OAuth authentication provider. */
    @JvmField val FACEBOOK = OAuthProvider("oauth_facebook")

    /** Google OAuth authentication provider. */
    @JvmField val GOOGLE = OAuthProvider("oauth_google")

    /** HubSpot OAuth authentication provider. */
    @JvmField val HUBSPOT = OAuthProvider("oauth_hubspot")

    /** GitHub OAuth authentication provider. */
    @JvmField val GITHUB = OAuthProvider("oauth_github")

    /** TikTok OAuth authentication provider. */
    @JvmField val TIKTOK = OAuthProvider("oauth_tiktok")

    /** GitLab OAuth authentication provider. */
    @JvmField val GITLAB = OAuthProvider("oauth_gitlab")

    /** Discord OAuth authentication provider. */
    @JvmField val DISCORD = OAuthProvider("oauth_discord")

    /** Twitter OAuth authentication provider. */
    @JvmField val TWITTER = OAuthProvider("oauth_twitter")

    /** Twitch OAuth authentication provider. */
    @JvmField val TWITCH = OAuthProvider("oauth_twitch")

    /** LinkedIn OAuth authentication provider (legacy). */
    @JvmField val LINKEDIN = OAuthProvider("oauth_linkedin")

    /** LinkedIn OpenID Connect authentication provider. */
    @JvmField val LINKEDIN_OIDC = OAuthProvider("oauth_linkedin_oidc")

    /** Dropbox OAuth authentication provider. */
    @JvmField val DROPBOX = OAuthProvider("oauth_dropbox")

    /** Atlassian OAuth authentication provider. */
    @JvmField val ATLASSIAN = OAuthProvider("oauth_atlassian")

    /** Bitbucket OAuth authentication provider. */
    @JvmField val BITBUCKET = OAuthProvider("oauth_bitbucket")

    /** Microsoft OAuth authentication provider. */
    @JvmField val MICROSOFT = OAuthProvider("oauth_microsoft")

    /** Notion OAuth authentication provider. */
    @JvmField val NOTION = OAuthProvider("oauth_notion")

    /** Apple OAuth authentication provider. */
    @JvmField val APPLE = OAuthProvider("oauth_apple")

    /** LINE OAuth authentication provider. */
    @JvmField val LINE = OAuthProvider("oauth_line")

    /** Instagram OAuth authentication provider. */
    @JvmField val INSTAGRAM = OAuthProvider("oauth_instagram")

    /** Coinbase OAuth authentication provider. */
    @JvmField val COINBASE = OAuthProvider("oauth_coinbase")

    /** Spotify OAuth authentication provider. */
    @JvmField val SPOTIFY = OAuthProvider("oauth_spotify")

    /** Xero OAuth authentication provider. */
    @JvmField val XERO = OAuthProvider("oauth_xero")

    /** Box OAuth authentication provider. */
    @JvmField val BOX = OAuthProvider("oauth_box")

    /** Slack OAuth authentication provider. */
    @JvmField val SLACK = OAuthProvider("oauth_slack")

    /** Linear OAuth authentication provider. */
    @JvmField val LINEAR = OAuthProvider("oauth_linear")

    /** Hugging Face OAuth authentication provider. */
    @JvmField val HUGGING_FACE = OAuthProvider("oauth_huggingface")

    /** Vercel OAuth authentication provider. */
    @JvmField val VERCEL = OAuthProvider("oauth_vercel")

    /** Generic custom OAuth strategy retained for source compatibility. */
    @JvmField val CUSTOM = OAuthProvider("oauth_custom")

    /** Unknown OAuth provider - used as fallback for non-OAuth strategies. */
    @JvmField val UNKNOWN = OAuthProvider("oauth_unknown")

    /** Built-in provider values, matching the former enum entry order. */
    @JvmField
    val entries: List<OAuthProvider> =
      listOf(
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
        VERCEL,
        CUSTOM,
        UNKNOWN,
      )

    private val entryNameByStrategy: Map<String, String> =
      listOf(
          "FACEBOOK",
          "GOOGLE",
          "HUBSPOT",
          "GITHUB",
          "TIKTOK",
          "GITLAB",
          "DISCORD",
          "TWITTER",
          "TWITCH",
          "LINKEDIN",
          "LINKEDIN_OIDC",
          "DROPBOX",
          "ATLASSIAN",
          "BITBUCKET",
          "MICROSOFT",
          "NOTION",
          "APPLE",
          "LINE",
          "INSTAGRAM",
          "COINBASE",
          "SPOTIFY",
          "XERO",
          "BOX",
          "SLACK",
          "LINEAR",
          "HUGGING_FACE",
          "VERCEL",
          "CUSTOM",
          "UNKNOWN",
        )
        .zip(entries)
        .associate { (name, provider) -> provider.strategy to name }

    private val providerDataByStrategy: Map<String, OAuthProviderData> =
      listOf(
          OAuthProviderData("facebook", FACEBOOK.strategy, "Facebook"),
          OAuthProviderData("google", GOOGLE.strategy, "Google"),
          OAuthProviderData("hubspot", HUBSPOT.strategy, "HubSpot"),
          OAuthProviderData("github", GITHUB.strategy, "GitHub"),
          OAuthProviderData("tiktok", TIKTOK.strategy, "TikTok"),
          OAuthProviderData("gitlab", GITLAB.strategy, "GitLab"),
          OAuthProviderData("discord", DISCORD.strategy, "Discord"),
          OAuthProviderData("twitter", TWITTER.strategy, "Twitter"),
          OAuthProviderData("twitch", TWITCH.strategy, "Twitch"),
          OAuthProviderData("linkedin", LINKEDIN.strategy, "LinkedIn"),
          OAuthProviderData("linkedin_oidc", LINKEDIN_OIDC.strategy, "LinkedIn"),
          OAuthProviderData("dropbox", DROPBOX.strategy, "Dropbox"),
          OAuthProviderData("atlassian", ATLASSIAN.strategy, "Atlassian"),
          OAuthProviderData("bitbucket", BITBUCKET.strategy, "Bitbucket"),
          OAuthProviderData("microsoft", MICROSOFT.strategy, "Microsoft"),
          OAuthProviderData("notion", NOTION.strategy, "Notion"),
          OAuthProviderData("apple", APPLE.strategy, "Apple"),
          OAuthProviderData("line", LINE.strategy, "LINE"),
          OAuthProviderData("instagram", INSTAGRAM.strategy, "Instagram"),
          OAuthProviderData("coinbase", COINBASE.strategy, "Coinbase"),
          OAuthProviderData("spotify", SPOTIFY.strategy, "Spotify"),
          OAuthProviderData("xero", XERO.strategy, "Xero"),
          OAuthProviderData("box", BOX.strategy, "Box"),
          OAuthProviderData("slack", SLACK.strategy, "Slack"),
          OAuthProviderData("linear", LINEAR.strategy, "Linear"),
          OAuthProviderData("huggingface", HUGGING_FACE.strategy, "Hugging Face"),
          OAuthProviderData("vercel", VERCEL.strategy, "Vercel"),
          OAuthProviderData("custom", CUSTOM.strategy, "Custom"),
          OAuthProviderData("unknown", UNKNOWN.strategy, "Unknown"),
        )
        .associateBy { it.strategy }

    /**
     * Converts a strategy string to the corresponding [OAuthProvider].
     *
     * This convenience function is primarily used to convert strategy strings from
     * [com.clerk.network.model.environment.UserSettings.SocialConfig.strategy] into type-safe
     * [OAuthProvider] values. This is useful when processing configuration data or API responses
     * that contain strategy strings.
     *
     * @param strategy The OAuth strategy string to convert (e.g., "oauth_google", "oauth_github").
     *   The strategy string should match one of the supported provider strategies. Custom and
     *   otherwise unrecognized OAuth strategies retain the complete string. Non-OAuth strategies
     *   return [UNKNOWN].
     * @return The corresponding [OAuthProvider] value, preserving custom OAuth strategy keys.
     *
     * ### Example usage:
     * ```kotlin
     * val provider = OAuthProvider.fromStrategy("oauth_google") // Returns OAuthProvider.GOOGLE
     * val githubProvider = OAuthProvider.fromStrategy("oauth_github") // Returns OAuthProvider.GITHUB
     * ```
     */
    fun fromStrategy(strategy: String): OAuthProvider {
      return entries.find { it.strategy == strategy }
        ?: if (strategy.startsWith("oauth_")) OAuthProvider(strategy) else UNKNOWN
    }

    /** Creates a custom OAuth provider while preserving its complete strategy key. */
    @JvmStatic
    fun custom(strategy: String): OAuthProvider {
      require(strategy == CUSTOM.strategy || strategy.startsWith("oauth_custom_")) {
        "Custom OAuth strategy must be oauth_custom or start with oauth_custom_"
      }
      return OAuthProvider(strategy)
    }

    /** Returns the built-in provider values, matching the former enum API. */
    @JvmStatic fun values(): Array<OAuthProvider> = entries.toTypedArray()

    /** Returns a built-in provider by its former enum constant name. */
    @JvmStatic
    fun valueOf(name: String): OAuthProvider =
      entries.firstOrNull { it.name == name }
        ?: throw IllegalArgumentException("No OAuthProvider with name $name")
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
    get() {
      providerDataByStrategy[strategy]?.let {
        return it
      }
      val configuredProvider = Clerk.socialProviders.values.find { it.strategy == strategy }
      return OAuthProviderData(
        provider = strategy.removePrefix("oauth_"),
        strategy = strategy,
        name = configuredProvider?.name?.takeIf { it.isNotBlank() } ?: "Custom",
      )
    }

  /** Enum-style constant name retained for source compatibility. */
  val name: String
    get() =
      when {
        strategy in entryNameByStrategy -> entryNameByStrategy.getValue(strategy)
        strategy.startsWith("oauth_") -> "CUSTOM"
        else -> "UNKNOWN"
      }

  override fun toString(): String = name
}

internal object OAuthProviderSerializer : KSerializer<OAuthProvider> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("OAuthProvider", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: OAuthProvider) {
    val builtInProvider = OAuthProvider.entries.firstOrNull { it == value }
    encoder.encodeString(builtInProvider?.name ?: value.strategy)
  }

  override fun deserialize(decoder: Decoder): OAuthProvider {
    val serializedValue = decoder.decodeString()
    return if (serializedValue.startsWith("oauth_")) {
      OAuthProvider.fromStrategy(serializedValue)
    } else {
      OAuthProvider.valueOf(serializedValue)
    }
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

// In-memory override store used for tests to inject logo URLs
private val logoUrlOverrides: MutableMap<OAuthProvider, String?> = mutableMapOf()

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
    // Test override takes precedence when present
    logoUrlOverrides[this]?.let { it.trim().takeIf { trimmed -> trimmed.isNotEmpty() } }
      ?: Clerk.socialProviders.values
        .find { it.strategy == strategy }
        ?.logoUrl
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

@VisibleForTesting
fun OAuthProvider.setLogoUrl(url: String?) {
  val trimmed = url?.trim()
  if (trimmed.isNullOrEmpty()) {
    logoUrlOverrides.remove(this)
  } else {
    logoUrlOverrides[this] = trimmed
  }
}

@VisibleForTesting
fun OAuthProvider.clearLogoUrlOverride() {
  this.setLogoUrl(null)
}
