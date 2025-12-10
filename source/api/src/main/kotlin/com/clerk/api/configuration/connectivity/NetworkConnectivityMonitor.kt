package com.clerk.api.configuration.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLog
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Internal utility for monitoring network connectivity state.
 *
 * This object uses Android's ConnectivityManager to detect online/offline state and provides
 * reactive state updates. When the device regains internet access after being offline, registered
 * callbacks are notified to trigger operations like SDK initialization retry.
 *
 * Features:
 * - Real-time connectivity monitoring using NetworkCallback
 * - StateFlow-based reactive connectivity state
 * - Automatic callback invocation when connectivity is restored
 * - Safe cleanup to prevent memory leaks
 *
 * This is an internal utility and should not be used outside the Clerk SDK.
 */
internal object NetworkConnectivityMonitor {

  /** Weak reference to application context to prevent memory leaks. */
  private var contextRef: WeakReference<Context>? = null

  /** Internal mutable state flow for connectivity status. */
  private val _isConnected = MutableStateFlow(true)

  /**
   * Public read-only state flow indicating current network connectivity.
   *
   * Emits true when the device has an active internet connection, false otherwise.
   */
  val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

  /** Callback to execute when connectivity is restored after being offline. */
  private var onConnectivityRestored: (() -> Unit)? = null

  /** Flag to track if we were previously disconnected to detect connectivity restoration. */
  @Volatile private var wasDisconnected = false

  /** Flag to track if monitoring has been started. */
  @Volatile private var isMonitoring = false

  /** Reference to the ConnectivityManager for managing network callbacks. */
  private var connectivityManager: ConnectivityManager? = null

  /** The network callback used to monitor connectivity changes. */
  private val networkCallback =
    object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        if (Clerk.debugMode) {
          ClerkLog.d("NetworkConnectivityMonitor: Network available")
        }
        handleConnectivityChange(true)
      }

      override fun onLost(network: Network) {
        if (Clerk.debugMode) {
          ClerkLog.d("NetworkConnectivityMonitor: Network lost")
        }
        // Check if we still have any other network connection
        val stillConnected = checkCurrentConnectivity()
        if (!stillConnected) {
          handleConnectivityChange(false)
        }
      }

      override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
          capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        if (Clerk.debugMode) {
          ClerkLog.d("NetworkConnectivityMonitor: Capabilities changed, hasInternet=$hasInternet")
        }
        handleConnectivityChange(hasInternet)
      }
    }

  /**
   * Configures and starts the network connectivity monitor.
   *
   * This method sets up the ConnectivityManager callback to monitor network state changes. It
   * should be called once during SDK initialization.
   *
   * @param context The application context used to access ConnectivityManager.
   * @param onConnectivityRestored Optional callback to execute when connectivity is restored after
   *   being offline.
   */
  fun configure(context: Context, onConnectivityRestored: (() -> Unit)? = null) {
    if (isMonitoring) {
      ClerkLog.d("NetworkConnectivityMonitor already monitoring. Updating callback only.")
      this.onConnectivityRestored = onConnectivityRestored
      return
    }

    this.contextRef = WeakReference(context.applicationContext)
    this.onConnectivityRestored = onConnectivityRestored

    try {
      connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

      connectivityManager?.let { cm ->
        // Check initial connectivity state
        val initiallyConnected = checkCurrentConnectivity()
        _isConnected.value = initiallyConnected
        wasDisconnected = !initiallyConnected

        if (Clerk.debugMode) {
          ClerkLog.d("NetworkConnectivityMonitor: Initial connectivity state = $initiallyConnected")
        }

        // Register for network callbacks
        val networkRequest =
          NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(networkRequest, networkCallback)
        isMonitoring = true

        ClerkLog.d("NetworkConnectivityMonitor configured and started")
      }
        ?: run {
          ClerkLog.w("NetworkConnectivityMonitor: ConnectivityManager not available")
        }
    } catch (e: Exception) {
      ClerkLog.e("NetworkConnectivityMonitor: Failed to configure: ${e.message}")
    }
  }

  /**
   * Checks the current network connectivity state.
   *
   * @return true if the device has an active, validated internet connection, false otherwise.
   */
  private fun checkCurrentConnectivity(): Boolean {
    return try {
      connectivityManager?.let { cm ->
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
          capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
      }
        ?: false
    } catch (e: Exception) {
      ClerkLog.w("NetworkConnectivityMonitor: Error checking connectivity: ${e.message}")
      false
    }
  }

  /**
   * Handles connectivity state changes.
   *
   * Updates the connectivity state flow and triggers the restoration callback if connectivity has
   * been restored after being offline.
   *
   * @param connected The new connectivity state.
   */
  private fun handleConnectivityChange(connected: Boolean) {
    val previousState = _isConnected.value
    _isConnected.value = connected

    if (connected && wasDisconnected) {
      ClerkLog.d("NetworkConnectivityMonitor: Connectivity restored after being offline")
      wasDisconnected = false
      onConnectivityRestored?.invoke()
    } else if (!connected && previousState) {
      ClerkLog.d("NetworkConnectivityMonitor: Device went offline")
      wasDisconnected = true
    }
  }

  /**
   * Stops monitoring network connectivity and cleans up resources.
   *
   * This method should be called when the SDK is no longer needed to prevent memory leaks.
   */
  fun stop() {
    if (!isMonitoring) {
      return
    }

    try {
      connectivityManager?.unregisterNetworkCallback(networkCallback)
      ClerkLog.d("NetworkConnectivityMonitor stopped")
    } catch (e: Exception) {
      ClerkLog.w("NetworkConnectivityMonitor: Error stopping: ${e.message}")
    } finally {
      isMonitoring = false
      connectivityManager = null
      contextRef = null
      onConnectivityRestored = null
    }
  }

  /**
   * Returns whether the device currently has network connectivity.
   *
   * This is a convenience method for synchronous access to the current state.
   *
   * @return true if connected, false otherwise.
   */
  fun isCurrentlyConnected(): Boolean = _isConnected.value

  /**
   * Resets the monitor state for testing purposes.
   *
   * This method clears all state and callbacks without unregistering from the system. Should only
   * be used in tests.
   */
  internal fun resetForTesting() {
    stop()
    _isConnected.value = true
    wasDisconnected = false
  }
}
