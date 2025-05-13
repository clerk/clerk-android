package com.clerk.sdk.model.environment

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
  val attributes: Map<String, AttributesConfig>,
  val signUp: SignUp,
  val social: Map<String, SocialConfig>,
  val actions: Actions,
  val passkeySettings: PasskeySettings?,
) {
  @Serializable
  data class AttributesConfig(
    val enabled: Boolean,
    val required: Boolean,
    val usedForFirstFactor: Boolean,
    val firstFactors: List<String>?,
    val usedForSecondFactor: Boolean,
    val secondFactors: List<String>?,
    val verifications: List<String>?,
    val verifyAtSignUp: Boolean,
  )

  @Serializable
  data class SignUp(
    val customActionRequired: Boolean,
    val progressive: Boolean,
    val mode: String,
    val legalConsentEnabled: Boolean,
  )

  @Serializable
  data class SocialConfig(
    val enabled: Boolean,
    val required: Boolean,
    val authenticatable: Boolean,
    val strategy: String,
    val notSelectable: Boolean,
    val name: String,
    val logoUrl: String?,
  )

  @Serializable
  data class Actions(var deleteSelf: Boolean = false, var createOrganization: Boolean = false)

  @Serializable
  data class PasskeySettings(val allowAutofill: Boolean, val showSignInButton: Boolean)
}
