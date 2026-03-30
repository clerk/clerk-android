package com.clerk.ui.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.navigation3.runtime.NavBackStack
import androidx.test.core.app.ApplicationProvider
import com.clerk.api.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthStateConfigurationTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    authPreferences().edit().clear().commit()
  }

  @Test
  fun defaultConfigurationLoadsPersistedValues() {
    val prefs = authPreferences()
    prefs.edit().putString(AUTH_START_IDENTIFIER_STORAGE_KEY, "stored@example.com").commit()
    prefs.edit().putString(AUTH_START_PHONE_NUMBER_STORAGE_KEY, "+15555550100").commit()

    val authState = createAuthState(sharedPreferences = prefs)

    assertEquals("stored@example.com", authState.authStartIdentifier)
    assertEquals("+15555550100", authState.authStartPhoneNumber)
  }

  @Test
  fun defaultConfigurationPersistsEdits() {
    val prefs = authPreferences()
    val authState = createAuthState(sharedPreferences = prefs)

    authState.authStartIdentifier = "edited@example.com"
    authState.authStartPhoneNumber = "+16666660123"

    assertEquals("edited@example.com", prefs.getString(AUTH_START_IDENTIFIER_STORAGE_KEY, null))
    assertEquals("+16666660123", prefs.getString(AUTH_START_PHONE_NUMBER_STORAGE_KEY, null))
  }

  @Test
  fun initialEmailOverridesPersistedValues() {
    val prefs = authPreferences()
    prefs.edit().putString(AUTH_START_IDENTIFIER_STORAGE_KEY, "stored@example.com").commit()
    prefs.edit().putString(AUTH_START_PHONE_NUMBER_STORAGE_KEY, "+15555550100").commit()

    val authState =
      createAuthState(
        sharedPreferences = prefs,
        identifierConfig = AuthIdentifierConfig(initialIdentifier = "seed@example.com"),
      )

    assertEquals("seed@example.com", authState.authStartIdentifier)
    assertTrue(authState.authStartPhoneNumber.isEmpty())
    assertEquals("seed@example.com", prefs.getString(AUTH_START_IDENTIFIER_STORAGE_KEY, null))
  }

  @Test
  fun initialPhoneNumberOverridesPersistedValues() {
    val prefs = authPreferences()
    prefs.edit().putString(AUTH_START_IDENTIFIER_STORAGE_KEY, "stored@example.com").commit()
    prefs.edit().putString(AUTH_START_PHONE_NUMBER_STORAGE_KEY, "+15555550100").commit()

    val authState =
      createAuthState(
        sharedPreferences = prefs,
        identifierConfig = AuthIdentifierConfig(initialIdentifier = "+17777770123"),
      )

    assertEquals("+17777770123", authState.authStartPhoneNumber)
    assertTrue(authState.authStartIdentifier.isEmpty())
  }

  @Test
  fun disablingPersistenceClearsStoredValues() {
    val prefs = authPreferences()
    prefs.edit().putString(AUTH_START_IDENTIFIER_STORAGE_KEY, "stored@example.com").commit()
    prefs.edit().putString(AUTH_START_PHONE_NUMBER_STORAGE_KEY, "+15555550100").commit()
    LastUsedIdentifierStorage.store(prefs, IdentifierType.Email)

    val authState =
      createAuthState(
        sharedPreferences = prefs,
        identifierConfig = AuthIdentifierConfig(persistIdentifiers = false),
      )

    assertTrue(authState.authStartIdentifier.isEmpty())
    assertTrue(authState.authStartPhoneNumber.isEmpty())
    assertNull(prefs.getString(AUTH_START_IDENTIFIER_STORAGE_KEY, null))
    assertNull(prefs.getString(AUTH_START_PHONE_NUMBER_STORAGE_KEY, null))
    assertNull(LastUsedIdentifierStorage.retrieve(prefs))
  }

  @Test
  fun disablingPersistenceSuppressesFutureWrites() {
    val prefs = authPreferences()
    val authState =
      createAuthState(
        sharedPreferences = prefs,
        identifierConfig = AuthIdentifierConfig(persistIdentifiers = false),
      )

    authState.authStartIdentifier = "new@example.com"
    authState.authStartPhoneNumber = "+19999990123"
    authState.storeLastUsedIdentifierType(IdentifierType.Phone)

    assertNull(prefs.getString(AUTH_START_IDENTIFIER_STORAGE_KEY, null))
    assertNull(prefs.getString(AUTH_START_PHONE_NUMBER_STORAGE_KEY, null))
    assertNull(LastUsedIdentifierStorage.retrieve(prefs))
  }

  @Test
  fun disablingPersistenceWithInitialEmailShowsButDoesNotStore() {
    val prefs = authPreferences()
    prefs.edit().putString(AUTH_START_IDENTIFIER_STORAGE_KEY, "stored@example.com").commit()
    prefs.edit().putString(AUTH_START_PHONE_NUMBER_STORAGE_KEY, "+15555550100").commit()
    LastUsedIdentifierStorage.store(prefs, IdentifierType.Phone)

    val authState =
      createAuthState(
        sharedPreferences = prefs,
        identifierConfig =
          AuthIdentifierConfig(initialIdentifier = "seed@example.com", persistIdentifiers = false),
      )

    assertEquals("seed@example.com", authState.authStartIdentifier)
    assertTrue(authState.authStartPhoneNumber.isEmpty())
    assertNull(prefs.getString(AUTH_START_IDENTIFIER_STORAGE_KEY, null))
    assertNull(prefs.getString(AUTH_START_PHONE_NUMBER_STORAGE_KEY, null))
    assertNull(LastUsedIdentifierStorage.retrieve(prefs))
  }

  private fun createAuthState(
    sharedPreferences: SharedPreferences = authPreferences(),
    identifierConfig: AuthIdentifierConfig = AuthIdentifierConfig(),
  ): AuthState {
    return AuthState(
      backStack = NavBackStack(AuthDestination.AuthStart),
      sharedPreferences = sharedPreferences,
      identifierConfig = identifierConfig,
    )
  }

  private fun authPreferences(): SharedPreferences {
    return context.getSharedPreferences(
      Constants.Storage.CLERK_PREFERENCES_FILE_NAME,
      Context.MODE_PRIVATE,
    )
  }
}
