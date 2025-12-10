package com.clerk.api.configuration.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NetworkConnectivityMonitorTest {

  private lateinit var context: Context
  private lateinit var mockConnectivityManager: ConnectivityManager
  private lateinit var mockNetwork: Network
  private lateinit var mockNetworkCapabilities: NetworkCapabilities

  @Before
  fun setup() {
    context = RuntimeEnvironment.getApplication()
    mockConnectivityManager = mockk(relaxed = true)
    mockNetwork = mockk(relaxed = true)
    mockNetworkCapabilities = mockk(relaxed = true)

    // Reset the monitor before each test
    NetworkConnectivityMonitor.resetForTesting()
  }

  @After
  fun tearDown() {
    NetworkConnectivityMonitor.resetForTesting()
    unmockkAll()
  }

  @Test
  fun `initial connectivity state is true by default`() {
    // Given - fresh state
    NetworkConnectivityMonitor.resetForTesting()

    // Then
    assertTrue("Initial connectivity state should be true", NetworkConnectivityMonitor.isConnected.value)
    assertTrue("isCurrentlyConnected should return true", NetworkConnectivityMonitor.isCurrentlyConnected())
  }

  @Test
  fun `configure sets up connectivity monitoring and checks initial state`() {
    // Given
    val mockContext = mockk<Context>(relaxed = true)
    val mockAppContext = mockk<Context>(relaxed = true)
    val callbackInvoked = AtomicBoolean(false)

    every { mockContext.applicationContext } returns mockAppContext
    every { mockAppContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager
    every { mockConnectivityManager.activeNetwork } returns mockNetwork
    every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
    every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
    every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true

    // When
    NetworkConnectivityMonitor.configure(mockContext) {
      callbackInvoked.set(true)
    }

    // Then
    assertTrue("Should be connected when network has internet capability", NetworkConnectivityMonitor.isConnected.value)
    assertFalse("Callback should not be invoked on initial configure", callbackInvoked.get())

    // Verify network callback was registered
    verify { mockConnectivityManager.registerNetworkCallback(any<NetworkRequest>(), any<ConnectivityManager.NetworkCallback>()) }
  }

  @Test
  fun `configure detects initial offline state correctly`() {
    // Given
    val mockContext = mockk<Context>(relaxed = true)
    val mockAppContext = mockk<Context>(relaxed = true)

    every { mockContext.applicationContext } returns mockAppContext
    every { mockAppContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager
    every { mockConnectivityManager.activeNetwork } returns null // No active network

    // When
    NetworkConnectivityMonitor.configure(mockContext)

    // Then
    assertFalse("Should be disconnected when no active network", NetworkConnectivityMonitor.isConnected.value)
  }

  @Test
  fun `stop unregisters network callback and cleans up resources`() {
    // Given
    val mockContext = mockk<Context>(relaxed = true)
    val mockAppContext = mockk<Context>(relaxed = true)

    every { mockContext.applicationContext } returns mockAppContext
    every { mockAppContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager
    every { mockConnectivityManager.activeNetwork } returns mockNetwork
    every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
    every { mockNetworkCapabilities.hasCapability(any()) } returns true

    NetworkConnectivityMonitor.configure(mockContext)

    // When
    NetworkConnectivityMonitor.stop()

    // Then
    verify { mockConnectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) }
  }

  @Test
  fun `callback is invoked when connectivity is restored after being offline`() {
    // Given
    val mockContext = mockk<Context>(relaxed = true)
    val mockAppContext = mockk<Context>(relaxed = true)
    val callbackInvoked = AtomicBoolean(false)
    val networkCallbackSlot = slot<ConnectivityManager.NetworkCallback>()

    every { mockContext.applicationContext } returns mockAppContext
    every { mockAppContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager
    every { mockConnectivityManager.activeNetwork } returns null // Start offline
    every { mockConnectivityManager.registerNetworkCallback(any<NetworkRequest>(), capture(networkCallbackSlot)) } returns Unit

    NetworkConnectivityMonitor.configure(mockContext) {
      callbackInvoked.set(true)
    }

    // Verify we're initially offline
    assertFalse("Should start disconnected", NetworkConnectivityMonitor.isConnected.value)

    // When - Simulate network becoming available
    every { mockConnectivityManager.activeNetwork } returns mockNetwork
    every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
    every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
    every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true

    networkCallbackSlot.captured.onAvailable(mockNetwork)

    // Then
    assertTrue("Callback should be invoked when connectivity is restored", callbackInvoked.get())
    assertTrue("Should now be connected", NetworkConnectivityMonitor.isConnected.value)
  }

  @Test
  fun `callback is not invoked when network is lost but still has other connection`() {
    // Given
    val mockContext = mockk<Context>(relaxed = true)
    val mockAppContext = mockk<Context>(relaxed = true)
    val mockSecondNetwork = mockk<Network>(relaxed = true)
    val callbackInvocationCount = AtomicInteger(0)
    val networkCallbackSlot = slot<ConnectivityManager.NetworkCallback>()

    every { mockContext.applicationContext } returns mockAppContext
    every { mockAppContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager
    every { mockConnectivityManager.activeNetwork } returns mockNetwork
    every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
    every { mockNetworkCapabilities.hasCapability(any()) } returns true
    every { mockConnectivityManager.registerNetworkCallback(any<NetworkRequest>(), capture(networkCallbackSlot)) } returns Unit

    NetworkConnectivityMonitor.configure(mockContext) {
      callbackInvocationCount.incrementAndGet()
    }

    assertTrue("Should start connected", NetworkConnectivityMonitor.isConnected.value)

    // When - First network is lost but another is still available
    every { mockConnectivityManager.activeNetwork } returns mockSecondNetwork
    every { mockConnectivityManager.getNetworkCapabilities(mockSecondNetwork) } returns mockNetworkCapabilities

    networkCallbackSlot.captured.onLost(mockNetwork)

    // Then - Should still be connected, callback not invoked
    assertTrue("Should still be connected via second network", NetworkConnectivityMonitor.isConnected.value)
    assertEquals("Callback should not have been invoked", 0, callbackInvocationCount.get())
  }

  @Test
  fun `multiple configure calls only update callback without re-registering`() {
    // Given
    val mockContext = mockk<Context>(relaxed = true)
    val mockAppContext = mockk<Context>(relaxed = true)
    val firstCallback = AtomicBoolean(false)
    val secondCallback = AtomicBoolean(false)

    every { mockContext.applicationContext } returns mockAppContext
    every { mockAppContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager
    every { mockConnectivityManager.activeNetwork } returns mockNetwork
    every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
    every { mockNetworkCapabilities.hasCapability(any()) } returns true

    // First configure
    NetworkConnectivityMonitor.configure(mockContext) {
      firstCallback.set(true)
    }

    // When - Configure again with different callback
    NetworkConnectivityMonitor.configure(mockContext) {
      secondCallback.set(true)
    }

    // Then - Only one registration should have happened
    verify(exactly = 1) { mockConnectivityManager.registerNetworkCallback(any<NetworkRequest>(), any<ConnectivityManager.NetworkCallback>()) }
  }

  @Test
  fun `resetForTesting resets all state`() {
    // Given
    val mockContext = mockk<Context>(relaxed = true)
    val mockAppContext = mockk<Context>(relaxed = true)

    every { mockContext.applicationContext } returns mockAppContext
    every { mockAppContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager
    every { mockConnectivityManager.activeNetwork } returns null // Offline

    NetworkConnectivityMonitor.configure(mockContext)
    assertFalse("Should be disconnected", NetworkConnectivityMonitor.isConnected.value)

    // When
    NetworkConnectivityMonitor.resetForTesting()

    // Then
    assertTrue("isConnected should be reset to true", NetworkConnectivityMonitor.isConnected.value)
    assertTrue("isCurrentlyConnected should return true", NetworkConnectivityMonitor.isCurrentlyConnected())
  }

  @Test
  fun `network capabilities change triggers state update`() {
    // Given
    val mockContext = mockk<Context>(relaxed = true)
    val mockAppContext = mockk<Context>(relaxed = true)
    val networkCallbackSlot = slot<ConnectivityManager.NetworkCallback>()

    every { mockContext.applicationContext } returns mockAppContext
    every { mockAppContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager
    every { mockConnectivityManager.activeNetwork } returns mockNetwork
    every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
    every { mockNetworkCapabilities.hasCapability(any()) } returns true
    every { mockConnectivityManager.registerNetworkCallback(any<NetworkRequest>(), capture(networkCallbackSlot)) } returns Unit

    NetworkConnectivityMonitor.configure(mockContext)
    assertTrue("Should start connected", NetworkConnectivityMonitor.isConnected.value)

    // When - Capabilities change to no internet
    val newCapabilities = mockk<NetworkCapabilities>()
    every { newCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns false
    every { newCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns false

    networkCallbackSlot.captured.onCapabilitiesChanged(mockNetwork, newCapabilities)

    // Then
    assertFalse("Should be disconnected when capabilities indicate no internet", NetworkConnectivityMonitor.isConnected.value)
  }
}
