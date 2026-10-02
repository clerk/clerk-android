package com.clerk.ui.auth

import android.content.SharedPreferences
import androidx.core.content.edit
import com.clerk.api.auth.types.Strategy
import com.clerk.api.sso.OAuthProvider

internal sealed class LastUsedAuth {
  data class Social(val provider: OAuthProvider) : LastUsedAuth()

  data object Email : LastUsedAuth()

  data object Username : LastUsedAuth()

  data object Phone : LastUsedAuth()

  data object BiometricCredential : LastUsedAuth()

  val socialProvider: OAuthProvider?
    get() =
      when (this) {
        is Social -> provider
        Email,
        Username,
        Phone,
        BiometricCredential -> null
      }

  val showsEmailUsernameBadge: Boolean
    get() = this == Email || this == Username

  val showsPhoneBadge: Boolean
    get() = this == Phone

  val showsBiometricCredentialBadge: Boolean
    get() = this == BiometricCredential

  companion object {
    @Suppress("ReturnCount")
    fun from(
      lastAuthenticationStrategy: String?,
      enabledFirstFactorAttributes: List<String>,
      authenticatableSocialProviders: List<OAuthProvider>,
      storedIdentifierType: IdentifierType?,
      biometricSignInIsVisible: Boolean = false,
    ): LastUsedAuth? {
      val lastAuth =
        lastAuthenticationStrategy?.takeIf { it.isNotBlank() }?.let(Strategy::from) ?: return null
      val visibleMethodCount =
        totalEnabledFirstFactorMethods(
          enabledFirstFactorAttributes,
          authenticatableSocialProviders,
        ) + (if (biometricSignInIsVisible) 1 else 0)
      if (visibleMethodCount <= 1) {
        return null
      }

      if (biometricSignInIsVisible && lastAuth == Strategy.TrustedDevice) {
        return BiometricCredential
      }

      if (lastAuth is Strategy.OAuth) {
        val provider = lastAuth.provider
        if (provider != OAuthProvider.UNKNOWN) {
          authenticatableSocialProviders
            .firstOrNull { it == provider }
            ?.let {
              return Social(it)
            }
        }
      }

      if (
        shouldShowBadge(
          strategies = phoneStrategies,
          lastAuth = lastAuth,
          enabledFirstFactorAttributes = enabledFirstFactorAttributes,
          storedIdentifierType = storedIdentifierType,
        )
      ) {
        return Phone
      }

      if (
        shouldShowBadge(
          strategies = emailStrategies,
          lastAuth = lastAuth,
          enabledFirstFactorAttributes = enabledFirstFactorAttributes,
          storedIdentifierType = storedIdentifierType,
        )
      ) {
        return Email
      }

      if (
        shouldShowBadge(
          strategies = usernameStrategies,
          lastAuth = lastAuth,
          enabledFirstFactorAttributes = enabledFirstFactorAttributes,
          storedIdentifierType = storedIdentifierType,
        )
      ) {
        return Username
      }

      return null
    }
  }
}

internal enum class IdentifierType {
  Email,
  Phone,
  Username,
}

internal object LastUsedIdentifierStorage {
  private const val identifierStorageKey = "clerk_last_used_identifier_type"

  fun store(sharedPreferences: SharedPreferences, identifierType: IdentifierType) {
    sharedPreferences.edit(commit = true) {
      putString(identifierStorageKey, identifierType.storageValue)
    }
  }

  fun retrieve(sharedPreferences: SharedPreferences): IdentifierType? {
    return when (sharedPreferences.getString(identifierStorageKey, null)) {
      "email" -> IdentifierType.Email
      "phone" -> IdentifierType.Phone
      "username" -> IdentifierType.Username
      else -> null
    }
  }

  fun clear(sharedPreferences: SharedPreferences) {
    sharedPreferences.edit(commit = true) { remove(identifierStorageKey) }
  }
}

private val emailStrategies =
  listOf(Strategy.EmailCode, Strategy.Password, Strategy.ResetPasswordEmailCode)

private val phoneStrategies =
  listOf(Strategy.PhoneCode, Strategy.Password, Strategy.ResetPasswordPhoneCode)

private val usernameStrategies = listOf(Strategy.Password)

private val IdentifierType.storageValue: String
  get() =
    when (this) {
      IdentifierType.Email -> "email"
      IdentifierType.Phone -> "phone"
      IdentifierType.Username -> "username"
    }

@Suppress("ReturnCount")
private fun shouldShowBadge(
  strategies: List<Strategy>,
  lastAuth: Strategy,
  enabledFirstFactorAttributes: List<String>,
  storedIdentifierType: IdentifierType?,
): Boolean {
  if (lastAuth == Strategy.Password && storedIdentifierType != null) {
    return storedIdentifierType.matches(strategies)
  }

  if (lastAuth == Strategy.Password && !canShowLastUsedBadge(enabledFirstFactorAttributes)) {
    return false
  }

  return strategies.contains(lastAuth)
}

private fun IdentifierType.matches(strategies: List<Strategy>): Boolean {
  return when (this) {
    IdentifierType.Email -> strategies.contains(Strategy.EmailCode)
    IdentifierType.Phone -> strategies.contains(Strategy.PhoneCode)
    IdentifierType.Username ->
      strategies.contains(Strategy.Password) &&
        strategies.none { it == Strategy.EmailCode || it == Strategy.PhoneCode }
  }
}

private fun canShowLastUsedBadge(enabledFirstFactorAttributes: List<String>): Boolean {
  val hasEmail = enabledFirstFactorAttributes.contains("email_address")
  val hasPhone = enabledFirstFactorAttributes.contains("phone_number")
  val hasUsername = enabledFirstFactorAttributes.contains("username")

  if (hasPhone && (hasEmail || hasUsername)) {
    return false
  }

  return true
}

private fun totalEnabledFirstFactorMethods(
  enabledFirstFactorAttributes: List<String>,
  authenticatableSocialProviders: List<OAuthProvider>,
): Int {
  val identifierKeys = setOf("email_address", "phone_number", "username")
  val identifierCount = enabledFirstFactorAttributes.count { it in identifierKeys }
  return identifierCount + authenticatableSocialProviders.size
}
