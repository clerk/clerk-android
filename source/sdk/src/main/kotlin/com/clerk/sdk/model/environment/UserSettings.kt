package com.clerk.sdk.model.environment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
  val attributes: Map<String, AttributesConfig>,
  @SerialName("sign_up") val signUp: SignUp,
  val social: Map<String, SocialConfig>,
  val actions: Actions,
  @SerialName("passkey_settings") val passkeySettings: PasskeySettings?,
) {
  @Serializable
  data class AttributesConfig(
    val enabled: Boolean,
    val required: Boolean,
    @SerialName("used_for_first_factor") val usedForFirstFactor: Boolean,
    @SerialName("first_factors") val firstFactors: List<String>?,
    @SerialName("used_for_second_factor") val usedForSecondFactor: Boolean,
    @SerialName("second_factors") val secondFactors: List<String>?,
    val verifications: List<String>?,
    @SerialName("verify_at_sign_up") val verifyAtSignUp: Boolean,
  )

  @Serializable
  data class SignUp(
    @SerialName("custom_action_required") val customActionRequired: Boolean,
    val progressive: Boolean,
    val mode: String,
    @SerialName("legal_consent_enabled") val legalConsentEnabled: Boolean,
  )

  @Serializable
  data class SocialConfig(
    val enabled: Boolean,
    val required: Boolean,
    val authenticatable: Boolean,
    val strategy: String,
    @SerialName("not_selectable") val notSelectable: Boolean,
    val name: String,
    @SerialName("logo_url") val logoUrl: String?,
  )

  @Serializable
  data class Actions(
    @SerialName("delete_self") val deleteSelf: Boolean = false,
    @SerialName("create_organization") val createOrganization: Boolean = false,
  )

  @Serializable
  data class PasskeySettings(
    @SerialName("allow_autofill") val allowAutofill: Boolean,
    @SerialName("show_sign_in_button") val showSignInButton: Boolean,
  )
}
