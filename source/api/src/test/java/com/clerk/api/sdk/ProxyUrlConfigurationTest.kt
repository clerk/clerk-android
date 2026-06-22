package com.clerk.api.sdk

import android.content.Context
import android.util.Base64
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.configuration.ConfigurationManager
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.AuthConfig
import com.clerk.api.network.model.environment.DisplayConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.UserSettings
import com.clerk.api.network.serialization.ClerkResult
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ProxyUrlConfigurationTest {

  private lateinit var mockContext: Context
  private val configurationManagers = mutableListOf<ConfigurationManager>()

  @Before
  fun setUp() {
    mockContext = mockk(relaxed = true)
    mockkObject(Client.Companion)
    mockkObject(Environment.Companion)
    coEvery { Client.get() } returns ClerkResult.success(Client())
    coEvery { Environment.get() } returns ClerkResult.success(testEnvironment())
  }

  @After
  fun tearDown() {
    configurationManagers.forEach { it.reset() }
    configurationManagers.clear()
    unmockkAll()
  }

  @Test
  fun `proxyUrl is passed from options and used for base url`() {
    // Given
    val proxyUrl = "https://proxy.example.com/__clerk"
    val options = ClerkConfigurationOptions(proxyUrl = proxyUrl)

    // When: configure using a fresh ConfigurationManager
    configure("pk_test_dummy", options)

    // Then
    assertEquals(proxyUrl, Clerk.baseUrl)
    assertEquals(proxyUrl, ClerkApi.configuredBaseUrl)
    assertEquals("$proxyUrl/v1/", ClerkApi.configuredUrlWithVersion)
  }

  @Test
  fun `customHeaders are passed from options and used for api configuration`() {
    // Given
    val proxyUrl = "https://proxy.example.com/__clerk"
    val customHeaders = mapOf("x-clerk-host-sdk" to "expo", "x-clerk-host-sdk-version" to "3.4.3")
    val options = ClerkConfigurationOptions(proxyUrl = proxyUrl).withCustomHeaders(customHeaders)

    // When: configure using a fresh ConfigurationManager
    configure("pk_test_dummy", options)

    // Then
    assertEquals(customHeaders, ClerkApi.configuredCustomHeaders)
  }

  @Test
  fun `fallback to publishableKey extraction when proxyUrl is not provided`() {
    // Given
    val domain = "clerk.example.com"
    val encodedDomain = Base64.encodeToString("${domain}x".toByteArray(), Base64.DEFAULT)
    val publishableKey = "pk_test_" + encodedDomain

    // When: configure using a fresh ConfigurationManager without proxy
    configure(publishableKey, null)

    // Then
    val expectedBaseUrl = "https://$domain"
    assertEquals(expectedBaseUrl, Clerk.baseUrl)
    assertEquals(expectedBaseUrl, ClerkApi.configuredBaseUrl)
    assertEquals("$expectedBaseUrl/v1/", ClerkApi.configuredUrlWithVersion)
  }

  private fun configure(publishableKey: String, options: ClerkConfigurationOptions?) {
    ConfigurationManager()
      .also { configurationManagers += it }
      .configure(mockContext, publishableKey, options)
  }

  private fun testEnvironment(): Environment {
    return Environment(
      authConfig = AuthConfig(singleSessionMode = false),
      displayConfig =
        DisplayConfig(
          applicationName = "Test Application",
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
}
