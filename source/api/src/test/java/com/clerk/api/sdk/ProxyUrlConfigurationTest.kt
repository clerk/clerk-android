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
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ProxyUrlConfigurationTest {

  private lateinit var context: Context
  private val configurationManagers = mutableListOf<ConfigurationManager>()

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    ClerkApi.reset()
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
    val proxyUrl = "https://proxy.example.com/__clerk"
    val options = ClerkConfigurationOptions(proxyUrl = proxyUrl)

    configure("pk_test_dummy", options)

    assertEquals(proxyUrl, Clerk.baseUrl)
    assertEquals(proxyUrl, ClerkApi.configuredBaseUrl)
    assertEquals("$proxyUrl/v1/", ClerkApi.configuredUrlWithVersion)
  }

  @Test
  fun `proxyUrl with a non-default port is accepted`() {
    val proxyUrl = "https://proxy.example.com:8443/__clerk"

    configure("pk_test_dummy", ClerkConfigurationOptions(proxyUrl = proxyUrl))

    assertEquals(proxyUrl, ClerkApi.configuredBaseUrl)
  }

  @Test
  fun `http proxyUrl is rejected`() {
    assertProxyUrlRejected("http://proxy.example.com/__clerk")
  }

  @Test
  fun `proxyUrl without a scheme is rejected`() {
    assertProxyUrlRejected("proxy.example.com/__clerk")
  }

  @Test
  fun `proxyUrl without a host is rejected`() {
    assertProxyUrlRejected("https://")
  }

  @Test
  fun `proxyUrl with a port but no host is rejected`() {
    assertProxyUrlRejected("https://:8443/__clerk")
  }

  @Test
  fun `proxyUrl with a non-http scheme is rejected`() {
    assertProxyUrlRejected("ftp://proxy.example.com/__clerk")
  }

  @Test
  fun `proxyUrl with userinfo is rejected`() {
    assertProxyUrlRejected("https://user:secret@proxy.example.com/__clerk")
  }

  @Test
  fun `proxyUrl with only a username is rejected`() {
    assertProxyUrlRejected("https://user@proxy.example.com/__clerk")
  }

  @Test
  fun `proxyUrl with a query is rejected`() {
    assertProxyUrlRejected("https://proxy.example.com/__clerk?env=dev")
  }

  @Test
  fun `proxyUrl with a fragment is rejected`() {
    assertProxyUrlRejected("https://proxy.example.com/__clerk#section")
  }

  @Test
  fun `proxyUrl with a trailing space is rejected`() {
    assertProxyUrlRejected("https://proxy.example.com/__clerk ")
  }

  @Test
  fun `proxyUrl with a leading space is rejected`() {
    assertProxyUrlRejected(" https://proxy.example.com/__clerk")
  }

  @Test
  fun `proxyUrl with a trailing newline is rejected`() {
    assertProxyUrlRejected("https://proxy.example.com/__clerk\n")
  }

  @Test
  fun `rejected whitespace proxyUrl error does not echo the url`() {
    val error =
      assertThrows(IllegalArgumentException::class.java) {
        configure(
          "pk_test_dummy",
          ClerkConfigurationOptions(proxyUrl = "https://proxy.example.com/__clerk "),
        )
      }

    assertFalse(error.message.orEmpty().contains("proxy.example.com"))
  }

  @Test
  fun `rejected proxyUrl error does not echo credentials`() {
    val error =
      assertThrows(IllegalArgumentException::class.java) {
        configure(
          "pk_test_dummy",
          ClerkConfigurationOptions(proxyUrl = "https://user:secret@proxy.example.com"),
        )
      }

    assertFalse(error.message.orEmpty().contains("secret"))
  }

  @Test
  fun `customHeaders are passed from options and used for api configuration`() {
    val proxyUrl = "https://proxy.example.com/__clerk"
    val customHeaders = mapOf("x-clerk-host-sdk" to "expo", "x-clerk-host-sdk-version" to "3.4.3")
    val options = ClerkConfigurationOptions(proxyUrl = proxyUrl).withCustomHeaders(customHeaders)

    configure("pk_test_dummy", options)

    assertEquals(customHeaders, ClerkApi.configuredCustomHeaders)
  }

  @Test
  fun `fallback to publishableKey extraction when proxyUrl is not provided`() {
    val domain = "clerk.example.com"
    val encodedDomain = Base64.encodeToString("${domain}x".toByteArray(), Base64.DEFAULT)
    val publishableKey = "pk_test_" + encodedDomain

    configure(publishableKey, null)

    val expectedBaseUrl = "https://$domain"
    assertEquals(expectedBaseUrl, Clerk.baseUrl)
    assertEquals(expectedBaseUrl, ClerkApi.configuredBaseUrl)
    assertEquals("$expectedBaseUrl/v1/", ClerkApi.configuredUrlWithVersion)
  }

  private fun assertProxyUrlRejected(proxyUrl: String) {
    val manager = ConfigurationManager().also { configurationManagers += it }

    assertThrows(IllegalArgumentException::class.java) {
      manager.configure(context, "pk_test_dummy", ClerkConfigurationOptions(proxyUrl = proxyUrl))
    }
    assertFalse(manager.isConfigured())
    assertNull(ClerkApi.configuredBaseUrl)
  }

  private fun configure(publishableKey: String, options: ClerkConfigurationOptions?) {
    ConfigurationManager()
      .also { configurationManagers += it }
      .configure(context, publishableKey, options)
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
