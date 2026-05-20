package com.clerk.api.sdk

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.configuration.connectivity.NetworkConnectivityMonitor
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.AuthConfig
import com.clerk.api.network.model.environment.DisplayConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.UserSettings
import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.session.SessionTokensCache
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import com.clerk.api.user.User
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ClerkConfigurationSwitchTest {
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    StorageHelper.initialize(context)
    StorageHelper.reset(context)
    Clerk.reset()
    mockkObject(Client.Companion)
    mockkObject(Environment.Companion)
    coEvery { Client.get() } returns ClerkResult.success(Client())
    coEvery { Client.getSkippingClientId() } returns ClerkResult.success(Client())
    coEvery { Environment.get() } returns ClerkResult.success(testEnvironment("Ready"))
  }

  @After
  fun tearDown() {
    Clerk.reset()
    unmockkAll()
    StorageHelper.reset(context)
    NetworkConnectivityMonitor.resetForTesting()
  }

  @Test
  fun `reset clears local configuration and runtime auth state while preserving device id`() =
    runTest {
      Clerk.initialize(
        context = context,
        publishableKey = FIRST_KEY,
        options = ClerkConfigurationOptions(enableDebugMode = true, proxyUrl = FIRST_PROXY),
      )
      val (client, user) = signedInClient()
      Clerk.updateEnvironment(testEnvironment("Old Application"))
      Clerk.updateClient(client)
      StorageHelper.saveValue(StorageKey.DEVICE_ID, "device_id_123")
      StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "device_token_123")
      SessionTokensCache.setToken("sess_123", TokenResource(jwt = "jwt_123"))

      assertEquals(user, Clerk.user)
      assertEquals("Old Application", Clerk.applicationName)
      assertEquals("device_token_123", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))

      Clerk.reset()

      assertNull(Clerk.session)
      assertNull(Clerk.user)
      assertNull(Clerk.applicationName)
      assertNull(Clerk.publishableKey)
      assertNull(Clerk.proxyUrl)
      assertEquals("", Clerk.baseUrl)
      assertNull(ClerkApi.configuredBaseUrl)
      assertNull(StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
      assertEquals("device_id_123", StorageHelper.loadValue(StorageKey.DEVICE_ID))
      assertEquals(0, SessionTokensCache.size)
      assertFalse(Clerk.debugMode)
      assertFalse(Clerk.isInitialized.value)
    }

  @Test
  fun `switchConfiguration resets and configures the new tenant synchronously`() {
    Clerk.initialize(
      context = context,
      publishableKey = FIRST_KEY,
      options = ClerkConfigurationOptions(enableDebugMode = true, proxyUrl = FIRST_PROXY),
    )

    Clerk.switchConfiguration(
      context = context,
      publishableKey = SECOND_KEY,
      options = ClerkConfigurationOptions(proxyUrl = SECOND_PROXY),
    )

    assertEquals(SECOND_KEY, Clerk.publishableKey)
    assertEquals(SECOND_PROXY, Clerk.proxyUrl)
    assertEquals(SECOND_PROXY, Clerk.baseUrl)
    assertEquals(SECOND_PROXY, ClerkApi.configuredBaseUrl)
    assertEquals("$SECOND_PROXY/v1/", ClerkApi.configuredUrlWithVersion)
    assertFalse(Clerk.debugMode)
  }

  @Test
  fun `initialize after configuration is a no-op and does not partially mutate options`() {
    Clerk.initialize(
      context = context,
      publishableKey = FIRST_KEY,
      options = ClerkConfigurationOptions(enableDebugMode = true, proxyUrl = FIRST_PROXY),
    )

    Clerk.initialize(
      context = context,
      publishableKey = SECOND_KEY,
      options = ClerkConfigurationOptions(enableDebugMode = false, proxyUrl = SECOND_PROXY),
    )

    assertEquals(FIRST_KEY, Clerk.publishableKey)
    assertEquals(FIRST_PROXY, Clerk.proxyUrl)
    assertEquals(FIRST_PROXY, Clerk.baseUrl)
    assertEquals(FIRST_PROXY, ClerkApi.configuredBaseUrl)
    assertEquals("$FIRST_PROXY/v1/", ClerkApi.configuredUrlWithVersion)
    assertTrue(Clerk.debugMode)
  }

  private fun signedInClient(): Pair<Client, User> {
    val user = mockk<User>(relaxed = true)
    val session = mockk<Session>(relaxed = true)
    every { session.id } returns "sess_123"
    every { session.status } returns Session.SessionStatus.ACTIVE
    every { session.user } returns user
    return Client(
      id = "client_123",
      sessions = listOf(session),
      lastActiveSessionId = "sess_123",
    ) to user
  }

  private fun testEnvironment(applicationName: String): Environment {
    return Environment(
      authConfig = AuthConfig(singleSessionMode = false),
      displayConfig =
        DisplayConfig(
          applicationName = applicationName,
          branded = true,
          logoImageUrl = "https://example.com/logo.png",
          homeUrl = "/",
          privacyPolicyUrl = null,
          termsUrl = null,
          googleOneTapClientId = null,
        ),
      userSettings =
        UserSettings(
          attributes = emptyMap(),
          signUp =
            UserSettings.SignUpUserSettings(
              customActionRequired = false,
              progressive = false,
              mode = "public",
              legalConsentEnabled = false,
            ),
          social = emptyMap(),
          actions = UserSettings.Actions(),
          passkeySettings = null,
        ),
    )
  }

  private companion object {
    const val FIRST_KEY = "pk_test_first"
    const val SECOND_KEY = "pk_test_second"
    const val FIRST_PROXY = "https://first.example.com/__clerk"
    const val SECOND_PROXY = "https://second.example.com/__clerk"
  }
}
