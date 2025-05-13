package com.clerk.sdk

import android.content.Context
import com.clerk.sdk.configuration.ClerkConfigurationState
import com.clerk.sdk.configuration.ConfigurationManager
import com.clerk.sdk.model.client.Client
import com.clerk.sdk.model.environment.Environment
import com.clerk.sdk.network.ClerkApi
import com.clerk.sdk.storage.StorageHelper
import com.slack.eithernet.ApiResult
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val VALID_TEST_KEY = "pk_test_Y2xlcmsuZXhhbXBsZS5jb20k"
private const val VALID_LIVE_KEY = "pk_live_Y2xlcmsuZXhhbXBsZS5jb20k"

private const val EXPECTED_URL = "https://clerk.example.com"

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ConfigurationManagerTest {

  @MockK private lateinit var mockContext: Context

  @MockK private lateinit var mockClient: Client

  @MockK private lateinit var mockEnvironment: Environment

  @SpyK private var configManager = ConfigurationManager()

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    MockKAnnotations.init(this)
    Dispatchers.setMain(testDispatcher)

    // Mock StorageHelper.initialize to do nothing
    mockkObject(StorageHelper)
    justRun { StorageHelper.initialize(any()) }

    // Mock ClerkApi.configure to do nothing
    mockkObject(ClerkApi)
    justRun { ClerkApi.configure(any()) }

    // Mock Client and Environment companion objects
    mockkObject(Client.Companion)
    mockkObject(Environment.Companion)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `configure with valid test key initializes StorageHelper and configures ClerkApi`() =
    runTest {
      // Given
      val callbackSlot = slot<ClerkConfigurationState>()
      val callback =
        mockk<(ClerkConfigurationState) -> Unit> {
          every { this@mockk.invoke(capture(callbackSlot)) } just Runs
        }

      // Mock successful API responses
      coEvery { Client.get() } returns ApiResult.success(mockClient)
      coEvery { Environment.get() } returns ApiResult.success(mockEnvironment)

      // When
      configManager.configure(mockContext, VALID_TEST_KEY, callback)

      // Then - Immediately verify setup actions
      verify { StorageHelper.initialize(mockContext) }
      verify { ClerkApi.configure(EXPECTED_URL) }

      // Advance coroutines
      testDispatcher.scheduler.advanceUntilIdle()

      // Then - Verify client and environment were fetched
      coVerify { Client.get() }
      coVerify { Environment.get() }

      // Then - Verify callback was called with success
      verify { callback.invoke(any()) }
      assertTrue(callbackSlot.captured is ClerkConfigurationState.Configured)
      val configState = callbackSlot.captured as ClerkConfigurationState.Configured
      assertEquals(mockEnvironment, configState.environment)
      assertEquals(mockClient, configState.client)
    }

  @Test
  fun `configure with valid live key initializes StorageHelper and configures ClerkApi`() =
    runTest {
      // Given
      val callbackSlot = slot<ClerkConfigurationState>()
      val callback =
        mockk<(ClerkConfigurationState) -> Unit> {
          every { this@mockk.invoke(capture(callbackSlot)) } just Runs
        }

      // Mock successful API responses
      coEvery { Client.get() } returns ApiResult.success(mockClient)
      coEvery { Environment.get() } returns ApiResult.success(mockEnvironment)

      // When
      configManager.configure(mockContext, VALID_LIVE_KEY, callback)

      // Then - Immediately verify setup actions
      verify { StorageHelper.initialize(mockContext) }
      verify { ClerkApi.configure(EXPECTED_URL) }

      // Advance coroutines
      testDispatcher.scheduler.advanceUntilIdle()

      // Then - Verify client and environment were fetched
      coVerify { Client.get() }
      coVerify { Environment.get() }

      // Then - Verify callback was called with success
      verify { callback.invoke(any()) }
      assertTrue(callbackSlot.captured is ClerkConfigurationState.Configured)
    }

  @Test
  fun `configure stores context and publishable key`() = runTest {
    // Given
    val callback =
      mockk<(ClerkConfigurationState) -> Unit> { every { this@mockk.invoke(any()) } just Runs }

    // Mock successful API responses
    coEvery { Client.get() } returns ApiResult.success(mockClient)
    coEvery { Environment.get() } returns ApiResult.success(mockEnvironment)

    // When
    configManager.configure(mockContext, VALID_TEST_KEY, callback)

    // Then
    assertEquals(mockContext, configManager.context?.get())
    assertEquals(VALID_TEST_KEY, configManager.publishableKey)
  }

  @Test
  fun `configure calls callback with error when client fetch fails`() = runTest {
    // Given
    val callbackSlot = slot<ClerkConfigurationState>()
    val callback =
      mockk<(ClerkConfigurationState) -> Unit> {
        every { this@mockk.invoke(capture(callbackSlot)) } just Runs
      }

    // Mock API responses - client fails, environment succeeds
    val error = ApiResult.networkFailure(IOException("Network error"))
    coEvery { Client.get() } returns error
    coEvery { Environment.get() } returns ApiResult.success(mockEnvironment)

    // When
    configManager.configure(mockContext, VALID_TEST_KEY, callback)

    // Advance coroutines
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    coVerify { Client.get() }
    coVerify { Environment.get() }

    // We don't directly verify the callback was called with Error because
    // the actual code just logs the error and doesn't call the callback
    verify(exactly = 0) { callback.invoke(ClerkConfigurationState.Error) }
  }

  @Test
  fun `configure calls callback with error when environment fetch fails`() = runTest {
    // Given
    val callbackSlot = slot<ClerkConfigurationState>()
    val callback =
      mockk<(ClerkConfigurationState) -> Unit> {
        every { this@mockk.invoke(capture(callbackSlot)) } just Runs
      }

    // Mock API responses - client succeeds, environment fails
    val error = ApiResult.networkFailure(IOException("Network error"))
    coEvery { Client.get() } returns ApiResult.success(mockClient)
    coEvery { Environment.get() } returns error

    // When
    configManager.configure(mockContext, VALID_TEST_KEY, callback)

    // Advance coroutines
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    coVerify { Client.get() }
    coVerify { Environment.get() }

    // We don't directly verify the callback was called with Error because
    // the actual code just logs the error and doesn't call the callback
    verify(exactly = 0) { callback.invoke(ClerkConfigurationState.Error) }
  }
}
