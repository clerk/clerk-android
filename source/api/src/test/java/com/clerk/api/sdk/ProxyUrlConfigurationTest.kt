package com.clerk.api.sdk

import android.content.Context
import android.util.Base64
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.configuration.ConfigurationManager
import com.clerk.api.network.ClerkApi
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
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
  }

  @After
  fun tearDown() {
    runBlocking {
      configurationManagers.forEach { manager ->
        manager.backgroundScope().coroutineContext[Job]?.children?.toList()?.forEach { child ->
          child.cancelAndJoin()
        }
      }
    }
    configurationManagers.clear()
  }

  @Test
  fun `proxyUrl is passed from options and used for base url`() {
    // Given
    val proxyUrl = "https://proxy.example.com/__clerk"
    val options = ClerkConfigurationOptions(proxyUrl = proxyUrl)

    // When: configure using a fresh ConfigurationManager
    createConfigurationManager().configure(mockContext, "pk_test_dummy", options)

    // Then
    assertEquals(proxyUrl, Clerk.baseUrl)
    assertEquals(proxyUrl, ClerkApi.configuredBaseUrl)
    assertEquals("$proxyUrl/v1/", ClerkApi.configuredUrlWithVersion)
  }

  @Test
  fun `fallback to publishableKey extraction when proxyUrl is not provided`() {
    // Given
    val domain = "clerk.example.com"
    val encodedDomain = Base64.encodeToString("${domain}x".toByteArray(), Base64.DEFAULT)
    val publishableKey = "pk_test_" + encodedDomain

    // When: configure using a fresh ConfigurationManager without proxy
    createConfigurationManager().configure(mockContext, publishableKey, null)

    // Then
    val expectedBaseUrl = "https://$domain"
    assertEquals(expectedBaseUrl, Clerk.baseUrl)
    assertEquals(expectedBaseUrl, ClerkApi.configuredBaseUrl)
    assertEquals("$expectedBaseUrl/v1/", ClerkApi.configuredUrlWithVersion)
  }

  private fun createConfigurationManager(): ConfigurationManager {
    return ConfigurationManager().also { configurationManagers += it }
  }

  private fun ConfigurationManager.backgroundScope(): CoroutineScope {
    val field = ConfigurationManager::class.java.getDeclaredField("scope")
    field.isAccessible = true
    return field.get(this) as CoroutineScope
  }
}
