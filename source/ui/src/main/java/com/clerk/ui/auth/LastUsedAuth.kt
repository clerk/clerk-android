package com.clerk.ui.auth

import android.content.Context
import androidx.core.content.edit
import com.clerk.api.Constants
import com.clerk.api.sso.OAuthProvider
import com.clerk.ui.core.common.StrategyKeys

internal sealed class LastUsedAuth {
  data class Social(val provider: OAuthProvider) : LastUsedAuth()

  data object Email : LastUsedAuth()

  data object Username : LastUsedAuth()

  data object Phone : LastUsedAuth()

  val socialProvider: OAuthProvider?
    get() =
      when (this) {
        is Social -> provider
        Email,
        Username,
        Phone -> null
      }

  val showsEmailUsernameBadge: Boolean
    get() = this == Email || this == Username

  val showsPhoneBadge: Boolean
    get() = this == Phone

  companion object {
    @Suppress("ReturnCount")
    fun from(
      lastAuthenticationStrategy: String?,
      enabledFirstFactorAttributes: List<String>,
      authenticatableSocialProviders: List<OAuthProvider>,
      storedIdentifierType: IdentifierType?,
    ): LastUsedAuth? {
      val lastAuth = lastAuthenticationStrategy?.takeIf { it.isNotBlank() } ?: return null
      if (
        totalEnabledFirstFactorMethods(
          enabledFirstFactorAttributes,
          authenticatableSocialProviders,
        ) <= 1
      ) {
        return null
      }

      if (isOAuthStrategy(lastAuth)) {
        val provider = OAuthProvider.fromStrategy(lastAuth)
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

  fun store(context: Context, identifierType: IdentifierType) {
    val prefs =
      context.applicationContext.getSharedPreferences(
        Constants.Storage.CLERK_PREFERENCES_FILE_NAME,
        Context.MODE_PRIVATE,
      )
    prefs.edit(commit = true) { putString(identifierStorageKey, identifierType.storageValue) }
  }

  fun retrieve(context: Context): IdentifierType? {
    val prefs =
      context.applicationContext.getSharedPreferences(
        Constants.Storage.CLERK_PREFERENCES_FILE_NAME,
        Context.MODE_PRIVATE,
      )
    return when (prefs.getString(identifierStorageKey, null)) {
      "email" -> IdentifierType.Email
      "phone" -> IdentifierType.Phone
      "username" -> IdentifierType.Username
      else -> null
    }
  }

  fun clear(context: Context) {
    val prefs =
      context.applicationContext.getSharedPreferences(
        Constants.Storage.CLERK_PREFERENCES_FILE_NAME,
        Context.MODE_PRIVATE,
      )
    prefs.edit(commit = true) { remove(identifierStorageKey) }
  }
}

private val emailStrategies =
  listOf(StrategyKeys.EMAIL_CODE, StrategyKeys.PASSWORD, StrategyKeys.RESET_PASSWORD_EMAIL_CODE)

private val phoneStrategies =
  listOf(StrategyKeys.PHONE_CODE, StrategyKeys.PASSWORD, StrategyKeys.RESET_PASSWORD_PHONE_CODE)

private val usernameStrategies = listOf(StrategyKeys.PASSWORD)

private val IdentifierType.storageValue: String
  get() =
    when (this) {
      IdentifierType.Email -> "email"
      IdentifierType.Phone -> "phone"
      IdentifierType.Username -> "username"
    }

@Suppress("ReturnCount")
private fun shouldShowBadge(
  strategies: List<String>,
  lastAuth: String,
  enabledFirstFactorAttributes: List<String>,
  storedIdentifierType: IdentifierType?,
): Boolean {
  if (lastAuth == StrategyKeys.PASSWORD && storedIdentifierType != null) {
    return storedIdentifierType.matches(strategies)
  }

  val identifierStrategies = emailStrategies + phoneStrategies + usernameStrategies
  if (
    identifierStrategies.contains(lastAuth) && !canShowLastUsedBadge(enabledFirstFactorAttributes)
  ) {
    return false
  }

  return strategies.contains(lastAuth)
}

private fun IdentifierType.matches(strategies: List<String>): Boolean {
  return when (this) {
    IdentifierType.Email -> strategies.contains(StrategyKeys.EMAIL_CODE)
    IdentifierType.Phone -> strategies.contains(StrategyKeys.PHONE_CODE)
    IdentifierType.Username ->
      strategies.contains(StrategyKeys.PASSWORD) &&
        strategies.none { it == StrategyKeys.EMAIL_CODE || it == StrategyKeys.PHONE_CODE }
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

private fun isOAuthStrategy(strategy: String): Boolean {
  return strategy.startsWith("oauth_") && !strategy.startsWith("oauth_token_")
}
