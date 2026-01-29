package com.clerk.ui.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clerk.api.Constants
import com.clerk.api.sso.OAuthProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LastUsedAuthTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    val prefs =
      context.getSharedPreferences(
        Constants.Storage.CLERK_PREFERENCES_FILE_NAME,
        Context.MODE_PRIVATE,
      )
    prefs.edit().clear().commit()
  }

  @Test
  fun fromReturnsSocialWhenOAuthStrategyIsAuthenticatable() {
    val result =
      LastUsedAuth.from(
        lastAuthenticationStrategy = "oauth_google",
        enabledFirstFactorAttributes = listOf("email_address"),
        authenticatableSocialProviders = listOf(OAuthProvider.GOOGLE),
        storedIdentifierType = null,
      )

    assertTrue(result is LastUsedAuth.Social)
    assertEquals(OAuthProvider.GOOGLE, (result as LastUsedAuth.Social).provider)
  }

  @Test
  fun fromReturnsNullWhenOAuthStrategyNotAuthenticatable() {
    val result =
      LastUsedAuth.from(
        lastAuthenticationStrategy = "oauth_google",
        enabledFirstFactorAttributes = listOf("email_address"),
        authenticatableSocialProviders = listOf(OAuthProvider.FACEBOOK),
        storedIdentifierType = null,
      )

    assertNull(result)
  }

  @Test
  fun fromReturnsPhoneWhenPhoneStrategyAndBadgeAllowed() {
    val result =
      LastUsedAuth.from(
        lastAuthenticationStrategy = "phone_code",
        enabledFirstFactorAttributes = listOf("phone_number"),
        authenticatableSocialProviders = listOf(OAuthProvider.GOOGLE),
        storedIdentifierType = null,
      )

    assertEquals(LastUsedAuth.Phone, result)
  }

  @Test
  fun fromReturnsEmailWhenEmailStrategyAndBadgeAllowed() {
    val result =
      LastUsedAuth.from(
        lastAuthenticationStrategy = "email_code",
        enabledFirstFactorAttributes = listOf("email_address"),
        authenticatableSocialProviders = listOf(OAuthProvider.GOOGLE),
        storedIdentifierType = null,
      )

    assertEquals(LastUsedAuth.Email, result)
  }

  @Test
  fun fromUsesStoredIdentifierTypeForPasswordEmail() {
    val result =
      LastUsedAuth.from(
        lastAuthenticationStrategy = "password",
        enabledFirstFactorAttributes = listOf("email_address"),
        authenticatableSocialProviders = listOf(OAuthProvider.GOOGLE),
        storedIdentifierType = IdentifierType.Email,
      )

    assertEquals(LastUsedAuth.Email, result)
  }

  @Test
  fun fromUsesStoredIdentifierTypeForPasswordUsername() {
    val result =
      LastUsedAuth.from(
        lastAuthenticationStrategy = "password",
        enabledFirstFactorAttributes = listOf("username"),
        authenticatableSocialProviders = listOf(OAuthProvider.GOOGLE),
        storedIdentifierType = IdentifierType.Username,
      )

    assertEquals(LastUsedAuth.Username, result)
  }

  @Test
  fun fromReturnsNullWhenIdentifierStrategiesDisallowedByBadgeRules() {
    val result =
      LastUsedAuth.from(
        lastAuthenticationStrategy = "phone_code",
        enabledFirstFactorAttributes = listOf("phone_number", "email_address"),
        authenticatableSocialProviders = listOf(OAuthProvider.GOOGLE),
        storedIdentifierType = null,
      )

    assertNull(result)
  }

  @Test
  fun fromReturnsNullWhenTotalEnabledFirstFactorMethodsIsOne() {
    val result =
      LastUsedAuth.from(
        lastAuthenticationStrategy = "email_code",
        enabledFirstFactorAttributes = listOf("email_address"),
        authenticatableSocialProviders = emptyList(),
        storedIdentifierType = null,
      )

    assertNull(result)
  }

  @Test
  fun fromReturnsNullWhenLastAuthBlankOrUnsupported() {
    val blankResult =
      LastUsedAuth.from(
        lastAuthenticationStrategy = "   ",
        enabledFirstFactorAttributes = listOf("email_address"),
        authenticatableSocialProviders = listOf(OAuthProvider.GOOGLE),
        storedIdentifierType = null,
      )

    val unsupportedResult =
      LastUsedAuth.from(
        lastAuthenticationStrategy = "magic_link",
        enabledFirstFactorAttributes = listOf("email_address"),
        authenticatableSocialProviders = listOf(OAuthProvider.GOOGLE),
        storedIdentifierType = null,
      )

    assertNull(blankResult)
    assertNull(unsupportedResult)
  }

  @Test
  fun identifierStorageStoreRetrieveClearRoundTrip() {
    val prefs =
      context.getSharedPreferences(
        Constants.Storage.CLERK_PREFERENCES_FILE_NAME,
        Context.MODE_PRIVATE,
      )
    val key = identifierStorageKey()

    assertNull(LastUsedIdentifierStorage.retrieve(context))

    LastUsedIdentifierStorage.store(context, IdentifierType.Email)
    assertEquals("email", prefs.getString(key, null))
    assertEquals(IdentifierType.Email, LastUsedIdentifierStorage.retrieve(context))

    LastUsedIdentifierStorage.store(context, IdentifierType.Phone)
    assertEquals("phone", prefs.getString(key, null))
    assertEquals(IdentifierType.Phone, LastUsedIdentifierStorage.retrieve(context))

    LastUsedIdentifierStorage.store(context, IdentifierType.Username)
    assertEquals("username", prefs.getString(key, null))
    assertEquals(IdentifierType.Username, LastUsedIdentifierStorage.retrieve(context))

    LastUsedIdentifierStorage.clear(context)
    assertFalse(prefs.contains(key))
    assertNull(LastUsedIdentifierStorage.retrieve(context))
  }

  private fun identifierStorageKey(): String {
    val field = LastUsedIdentifierStorage::class.java.getDeclaredField("identifierStorageKey")
    field.isAccessible = true
    return field.get(null) as String
  }
}
