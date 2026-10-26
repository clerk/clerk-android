package com.clerk.api.configuration

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.FrameworkIntegrationApi
import com.clerk.api.appversion.AppBuild
import com.clerk.api.appversion.AppVersionSupport
import com.clerk.api.appversion.TEST_PLAY_URL
import com.clerk.api.appversion.androidPolicy
import com.clerk.api.appversion.appVersionTestEnvironment
import com.clerk.api.configuration.connectivity.NetworkConnectivityMonitor
import com.clerk.api.configuration.lifecycle.AppLifecycleListener
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.storage.StorageHelper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

private const val PROXY_URL = "https://proxy.example.com"

@OptIn(ExperimentalCoroutinesApi::class, FrameworkIntegrationApi::class)
@RunWith(RobolectricTestRunner::class)
class ConfigurationManagerAppVersionTest {
  private lateinit var context: Context
  private lateinit var manager: ConfigurationManager

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
    every { NetworkConnectivityMonitor.configure(any(), any()) } returns Unit
    every { AppLifecycleListener.configure(any()) } returns Unit
    coEvery { Environment.get() } returns
      ClerkResult.success(
        appVersionTestEnvironment(
          androidPolicy("42", packageName = context.packageName),
          applicationName = "Blocked App",
        )
      )
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
  fun `an unsupported app still loads the environment its update screen shows`() = runTest {
    coEvery { Client.get() } returns clientFailure("unsupported_app_version")

    initialize()
    runCurrent()

    assertEquals("Blocked App", Clerk.applicationName)
    assertFalse(AppVersionSupport.status.value.isSupported)
    assertEquals(42L, AppVersionSupport.status.value.minimumVersion)
    assertFalse(manager.isInitialized.value)
    assertNull(manager.initializationError.value)
  }

  @Test
  fun `a rejected client keeps the app blocked when the environment has no minimum for it`() =
    runTest {
      coEvery { Environment.get() } returns
        ClerkResult.success(appVersionTestEnvironment(applicationName = "Blocked App"))
      val meta = buildJsonObject {
        put("minimum_version", JsonPrimitive("42"))
        put("update_url", JsonPrimitive(TEST_PLAY_URL))
      }
      coEvery { Client.get() } answers
        {
          AppVersionSupport.applyUnsupportedAppVersion(meta, AppVersionSupport.nextRequest())
          ClerkResult.apiFailure(
            ClerkErrorResponse(
              errors = listOf(Error(code = "unsupported_app_version", meta = meta))
            )
          )
        }

      initialize()
      runCurrent()

      assertEquals("Blocked App", Clerk.applicationName)
      assertFalse(AppVersionSupport.status.value.isSupported)
      assertEquals(42L, AppVersionSupport.status.value.minimumVersion)
      assertEquals(TEST_PLAY_URL, AppVersionSupport.status.value.updateUrl)
    }

  @Test
  fun `a blocked app retries initialization only while it's in the foreground`() = runTest {
    var clientRequests = 0
    coEvery { Client.get() } answers
      {
        clientRequests++
        clientFailure("unsupported_app_version")
      }
    every { AppLifecycleListener.isInBackground } returns true

    initialize()
    runCurrent()
    assertEquals(1, clientRequests)

    advanceTimeBy(10.minutes)
    assertEquals(1, clientRequests)

    every { AppLifecycleListener.isInBackground } returns false
    advanceTimeBy(6.seconds)
    assertEquals(2, clientRequests)
  }

  @Test
  fun `a blocked app refreshes on foreground even when the foreground refresh is off`() = runTest {
    val onForeground = slot<() -> Unit>()
    every { AppLifecycleListener.configure(capture(onForeground)) } returns Unit
    var clientRequests = 0
    coEvery { Client.get() } answers
      {
        clientRequests++
        clientFailure("unsupported_app_version")
      }

    initialize(
      options = ClerkConfigurationOptions(proxyUrl = PROXY_URL).withForegroundRefreshDisabled()
    )
    runCurrent()
    val requestsAtLaunch = clientRequests

    onForeground.captured()
    runCurrent()

    assertEquals(requestsAtLaunch + 1, clientRequests)
  }

  @Test
  fun `another client failure still leaves the environment unloaded`() = runTest {
    coEvery { Client.get() } returns clientFailure("authorization_invalid")

    initialize()
    runCurrent()

    assertNull(Clerk.applicationName)
    assertNotNull(manager.initializationError.value)
  }

  private fun TestScope.initialize(
    options: ClerkConfigurationOptions = ClerkConfigurationOptions(proxyUrl = PROXY_URL),
    versionCode: Long = 41,
  ) {
    manager = ConfigurationManager(backgroundScope)
    manager.configure(context = context, publishableKey = "pk_test_app_version", options = options)
    AppVersionSupport.configure(AppBuild(context.packageName, versionCode = versionCode))
  }

  private fun clientFailure(code: String): ClerkResult<Client, ClerkErrorResponse> =
    ClerkResult.apiFailure(ClerkErrorResponse(errors = listOf(Error(code = code))))
}
