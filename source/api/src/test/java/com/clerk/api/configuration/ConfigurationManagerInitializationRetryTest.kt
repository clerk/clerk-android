package com.clerk.api.configuration

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.configuration.connectivity.NetworkConnectivityMonitor
import com.clerk.api.configuration.lifecycle.AppLifecycleListener
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.AuthConfig
import com.clerk.api.network.model.environment.DisplayConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.UserSettings
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.storage.StorageHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConfigurationManagerInitializationRetryTest {
  private lateinit var context: Context
  private lateinit var manager: ConfigurationManager
  private val onConnectivityRestored = slot<() -> Unit>()

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    StorageHelper.initialize(context)
    StorageHelper.reset(context)
    Clerk.reset()
    mockkObject(Client.Companion, Environment.Companion)
    every { Client.serializer() } answers { callOriginal() }
    every { Environment.serializer() } answers { callOriginal() }
    mockkObject(NetworkConnectivityMonitor, AppLifecycleListener)
    every { NetworkConnectivityMonitor.configure(any(), capture(onConnectivityRestored)) } returns
      Unit
    every { AppLifecycleListener.configure(any()) } returns Unit
    stubOfflineRefresh()
  }

  @After
  fun tearDown() {
    if (::manager.isInitialized) manager.reset()
    Clerk.reset()
    unmockkAll()
    StorageHelper.reset(context)
    NetworkConnectivityMonitor.resetForTesting()
  }

  @Test
  fun `initialization recovers after prolonged outage without a connectivity callback`() = runTest {
    val attempts = mutableListOf<Long>()
    coEvery { Environment.get() } coAnswers
      {
        attempts += currentTime
        ClerkResult.unknownFailure(IOException("offline"))
      }
    initialize()

    advanceTimeBy(195_000)
    runCurrent()

    assertEquals(listOf(0L, 5_000L, 15_000L, 35_000L, 75_000L, 135_000L, 195_000L), attempts)
    assertFalse(manager.isInitialized.value)
    assertNotNull(manager.initializationError.value)

    stubSuccessfulRefresh()
    advanceTimeBy(60_000)
    runCurrent()

    assertTrue(manager.isInitialized.value)
    assertNull(manager.initializationError.value)
    assertEquals("client_recovered", Clerk.client.id)
    assertEquals("Recovered App", Clerk.applicationName)

    advanceTimeBy(300_000)
    runCurrent()
    coVerify(exactly = 8) { Environment.get() }
  }

  @Test
  fun `connectivity restoration retries immediately and cancels pending backoff`() = runTest {
    initialize()
    runCurrent()
    advanceTimeBy(2_000)
    stubSuccessfulRefresh()

    onConnectivityRestored.captured.invoke()
    runCurrent()

    assertTrue(manager.isInitialized.value)
    advanceTimeBy(300_000)
    runCurrent()
    coVerify(exactly = 2) { Environment.get() }
  }

  @Test
  fun `manual retry replaces the pending retry instead of starting another retry chain`() =
    runTest {
      initialize()
      runCurrent()
      advanceTimeBy(2_000)

      assertTrue(manager.reinitialize())
      runCurrent()
      advanceTimeBy(3_000)
      runCurrent()
      coVerify(exactly = 2) { Environment.get() }

      advanceTimeBy(2_000)
      runCurrent()
      coVerify(exactly = 3) { Environment.get() }
    }

  @Test
  fun `reset cancels pending retries`() = runTest {
    initialize()
    runCurrent()

    manager.reset()
    advanceTimeBy(300_000)
    runCurrent()

    assertFalse(manager.isConfigured())
    coVerify(exactly = 1) { Environment.get() }
  }

  @Test
  fun `configuration switch discards retries for the old instance`() = runTest {
    initialize()
    runCurrent()
    advanceTimeBy(2_000)

    manager.reset()
    stubSuccessfulRefresh()
    configure("https://other.example.com")
    runCurrent()
    advanceTimeBy(300_000)
    runCurrent()

    assertTrue(manager.isInitialized.value)
    assertEquals("https://other.example.com", Clerk.baseUrl)
    coVerify(exactly = 2) { Environment.get() }
  }

  @Test
  fun `refresh timeout schedules another initialization attempt`() = runTest {
    coEvery { Environment.get() } coAnswers { awaitCancellation() }
    initialize()
    advanceTimeBy(30_000)
    runCurrent()

    assertFalse(manager.isInitialized.value)
    assertNotNull(manager.initializationError.value)
    stubSuccessfulRefresh()
    advanceTimeBy(5_000)
    runCurrent()

    assertTrue(manager.isInitialized.value)
    assertNull(manager.initializationError.value)
    coVerify(exactly = 2) { Environment.get() }
  }

  @Test
  fun `reset cancels an in-flight refresh without scheduling a retry`() = runTest {
    coEvery { Environment.get() } coAnswers { awaitCancellation() }
    initialize()
    runCurrent()

    manager.reset()
    advanceTimeBy(300_000)
    runCurrent()

    assertNull(manager.initializationError.value)
    coVerify(exactly = 1) { Environment.get() }
  }

  private fun TestScope.initialize() {
    manager = ConfigurationManager(backgroundScope)
    configure()
  }

  private fun configure(proxyUrl: String = "https://proxy.example.com") {
    manager.configure(
      context = context,
      publishableKey = "pk_test_initialization_retry",
      options = ClerkConfigurationOptions(proxyUrl = proxyUrl),
    )
  }

  private fun stubOfflineRefresh() {
    coEvery { Client.get() } returns ClerkResult.unknownFailure(IOException("offline"))
    coEvery { Environment.get() } returns ClerkResult.unknownFailure(IOException("offline"))
  }

  private fun stubSuccessfulRefresh() {
    coEvery { Client.get() } returns ClerkResult.success(Client(id = "client_recovered"))
    coEvery { Environment.get() } returns ClerkResult.success(testEnvironment())
  }

  private fun testEnvironment(): Environment =
    Environment(
      authConfig = AuthConfig(singleSessionMode = false),
      displayConfig =
        DisplayConfig(
          applicationName = "Recovered App",
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
