package com.clerk.api.network.model.environment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents user settings and configuration from the Clerk environment.
 *
 * This data class contains all the configuration settings that control user authentication, sign-up
 * flows, social providers, and other user-related features for the application.
 *
 * @property attributes Configuration for user attributes (email, phone, username, etc.)
 * @property signUp Sign-up flow configuration and settings
 * @property social Configuration for social authentication providers
 * @property actions Available user actions and permissions
 * @property passkeySettings Configuration for passkey authentication (optional)
 */
@Serializable
data class UserSettings(
  /** Configuration for user attributes (email, phone, username, etc.) */
  val attributes: Map<String, AttributesConfig>,

  /** Sign-up flow configuration and settings */
  @SerialName("sign_up") val signUp: SignUpUserSettings,

  /** Configuration for social authentication providers */
  val social: Map<String, SocialConfig>,

  /** Available user actions and permissions */
  val actions: Actions,

  /** Configuration for passkey authentication (optional) */
  @SerialName("passkey_settings") val passkeySettings: PasskeySettings?,
) {
  /**
   * Configuration for a user attribute (email, phone, username, etc.).
   *
   * This class defines how a specific user attribute behaves in the authentication system,
   * including whether it's enabled, required, and what verification methods are available.
   *
   * @property enabled Whether this attribute is enabled for the application
   * @property required Whether this attribute is required during sign-up
   * @property usedForFirstFactor Whether this attribute can be used for first-factor authentication
   * @property firstFactors List of first-factor authentication strategies available for this
   *   attribute
   * @property usedForSecondFactor Whether this attribute can be used for second-factor
   *   authentication
   * @property secondFactors List of second-factor authentication strategies available for this
   *   attribute
   * @property verifications List of verification methods available for this attribute
   * @property verifyAtSignUp Whether this attribute must be verified during the sign-up process
   */
  @Serializable
  data class AttributesConfig(
    /** Whether this attribute is enabled for the application */
    val enabled: Boolean,

    /** Whether this attribute is required during sign-up */
    val required: Boolean,

    /** Whether this attribute can be used for first-factor authentication */
    @SerialName("used_for_first_factor") val usedForFirstFactor: Boolean,

    /** List of first-factor authentication strategies available for this attribute */
    @SerialName("first_factors") val firstFactors: List<String>?,

    /** Whether this attribute can be used for second-factor authentication */
    @SerialName("used_for_second_factor") val usedForSecondFactor: Boolean,

    /** List of second-factor authentication strategies available for this attribute */
    @SerialName("second_factors") val secondFactors: List<String>?,

    /** List of verification methods available for this attribute */
    val verifications: List<String>?,

    /** Whether this attribute must be verified during the sign-up process */
    @SerialName("verify_at_sign_up") val verifyAtSignUp: Boolean,
  )

  /**
   * Configuration for the sign-up flow and requirements.
   *
   * This class defines how the user registration process behaves, including whether custom actions
   * are required and if legal consent is needed.
   *
   * @property customActionRequired Whether a custom action is required during sign-up
   * @property progressive Whether progressive sign-up is enabled
   * @property mode The sign-up mode configuration
   * @property legalConsentEnabled Whether legal consent is required during sign-up
   */
  @Serializable
  data class SignUpUserSettings(
    /** Whether a custom action is required during sign-up */
    @SerialName("custom_action_required") val customActionRequired: Boolean,

    /** Whether progressive sign-up is enabled */
    val progressive: Boolean,

    /** The sign-up mode configuration */
    val mode: String,

    /** Whether legal consent is required during sign-up */
    @SerialName("legal_consent_enabled") val legalConsentEnabled: Boolean,
  )

  /**
   * Configuration for a social authentication provider.
   *
   * This class defines the settings for a specific OAuth provider, including whether it's enabled,
   * required, and how it should be displayed to users.
   *
   * @property enabled Whether this social provider is enabled
   * @property required Whether this social provider is required for authentication
   * @property authenticatable Whether this provider can be used for authentication
   * @property strategy The OAuth strategy identifier for this provider
   * @property notSelectable Whether this provider should be hidden from user selection
   * @property name The display name of the social provider
   * @property logoUrl The URL of the provider's logo image (optional)
   */
  @Serializable
  data class SocialConfig(
    /** Whether this social provider is enabled */
    val enabled: Boolean,

    /** Whether this social provider is required for authentication */
    val required: Boolean,

    /** Whether this provider can be used for authentication */
    val authenticatable: Boolean,

    /** The OAuth strategy identifier for this provider */
    val strategy: String,

    /** Whether this provider should be hidden from user selection */
    @SerialName("not_selectable") val notSelectable: Boolean,

    /** The display name of the social provider */
    val name: String,

    /** The URL of the provider's logo image (optional) */
    @SerialName("logo_url") val logoUrl: String? = null,
  )

  /**
   * Configuration for user actions and permissions.
   *
   * This class defines what actions users are allowed to perform within the application, such as
   * deleting their own account or creating organizations.
   *
   * @property deleteSelf Whether users can delete their own accounts
   * @property createOrganization Whether users can create organizations
   */
  @Serializable
  data class Actions(
    /** Whether users can delete their own accounts */
    @SerialName("delete_self") val deleteSelf: Boolean = false,

    /** Whether users can create organizations */
    @SerialName("create_organization") val createOrganization: Boolean = false,
  )

  /**
   * Configuration for passkey authentication features.
   *
   * This class defines the behavior and display options for passkey authentication, including
   * autofill capabilities and UI elements.
   *
   * @property allowAutofill Whether passkey autofill is allowed
   * @property showSignInButton Whether to show a dedicated passkey sign-in button
   */
  @Serializable
  data class PasskeySettings(
    /** Whether passkey autofill is allowed */
    @SerialName("allow_autofill") val allowAutofill: Boolean,

    /** Whether to show a dedicated passkey sign-in button */
    @SerialName("show_sign_in_button") val showSignInButton: Boolean,
  )
}
