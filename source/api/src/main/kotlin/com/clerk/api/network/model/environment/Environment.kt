package com.clerk.api.network.model.environment

import com.clerk.api.Clerk.environment
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class Environment(
  @SerialName("auth_config") val authConfig: AuthConfig,
  @SerialName("display_config") val displayConfig: DisplayConfig,
  @SerialName("user_settings") val userSettings: UserSettings,
  @SerialName("organization_settings")
  val organizationSettings: OrganizationSettings = OrganizationSettings(),
) {
  val passkeyIsEnabled: Boolean
    get() = userSettings.attributes.any { (key, value) -> key == "passkey" && value.enabled }

  val mfaIsEnabled: Boolean
    get() = userSettings.attributes.any { (_, value) -> value.enabled && value.usedForSecondFactor }

  val mfaAuthenticatorAppIsEnabled: Boolean
    get() =
      userSettings.attributes["authenticator_app"]?.enabled == true &&
        userSettings.attributes["authenticator_app"]?.usedForSecondFactor == true

  val passwordIsEnabled: Boolean
    get() = userSettings.attributes.any { (key, value) -> key == "password" && value.enabled }

  val usernameIsEnabled: Boolean
    get() = userSettings.attributes.any { (key, value) -> key == "username" && value.enabled }

  val firstNameIsEnabled: Boolean
    get() = userSettings.attributes.any { (key, value) -> key == "first_name" && value.enabled }

  val lastNameIsEnabled: Boolean
    get() = userSettings.attributes.any { (key, value) -> key == "last_name" && value.enabled }

  val emailIsEnabled: Boolean
    get() = userSettings.attributes["email_address"]?.enabled == true

  val phoneNumberIsEnabled: Boolean
    get() = userSettings.attributes["phone_number"]?.enabled == true

  val emailIsImmutable: Boolean
    get() = userSettings.attributes["email_address"]?.immutable == true

  val phoneNumberIsImmutable: Boolean
    get() = userSettings.attributes["phone_number"]?.immutable == true

  val usernameIsImmutable: Boolean
    get() = userSettings.attributes["username"]?.immutable == true

  val mfaPhoneCodeIsEnabled: Boolean
    get() =
      userSettings.attributes.any { (key, value) ->
        key == "phone_number" && value.enabled && value.usedForSecondFactor
      }

  val mfaBackupCodeIsEnabled: Boolean
    get() =
      userSettings.attributes.any { (key, value) ->
        key == "backup_code" && value.enabled && value.usedForSecondFactor
      }

  companion object {

    /** Fetches the environment configuration from the Clerk API. */
    suspend fun get(): ClerkResult<Environment, ClerkErrorResponse> = ClerkApi.environment.get()
  }
}

internal fun Environment.enabledFirstFactorAttributes(): List<String> {
  return environment.userSettings.attributes
    .filter { it.value.enabled && it.value.usedForFirstFactor }
    .keys
    .toList()
}
