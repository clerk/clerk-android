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

internal object NetworkConnectivityMonitor {
  private var contextRef: WeakReference<Context>? = null

  private val _isConnected = MutableStateFlow(true)

  val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

  private var onConnectivityRestored: (() -> Unit)? = null

  @Volatile private var wasDisconnected = false

  @Volatile private var isMonitoring = false

  private var connectivityManager: ConnectivityManager? = null

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
        val stillConnected = checkCurrentConnectivity()
        if (!stillConnected) {
          handleConnectivityChange(false)
        }
      }

      override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
        val hasInternet =
          capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        if (Clerk.debugMode) {
          ClerkLog.d("NetworkConnectivityMonitor: Capabilities changed, hasInternet=$hasInternet")
        }
        handleConnectivityChange(hasInternet)
      }
    }

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
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
          as? ConnectivityManager

      connectivityManager?.let { cm ->
        val initiallyConnected = checkCurrentConnectivity()
        _isConnected.value = initiallyConnected
        wasDisconnected = !initiallyConnected

        if (Clerk.debugMode) {
          ClerkLog.d("NetworkConnectivityMonitor: Initial connectivity state = $initiallyConnected")
        }

        val networkRequest =
          NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(networkRequest, networkCallback)
        isMonitoring = true

        ClerkLog.d("NetworkConnectivityMonitor configured and started")
      } ?: run { ClerkLog.w("NetworkConnectivityMonitor: ConnectivityManager not available") }
    } catch (e: Exception) {
      ClerkLog.e("NetworkConnectivityMonitor: Failed to configure: ${e.message}")
    }
  }

  private fun checkCurrentConnectivity(): Boolean {
    return try {
      val cm = connectivityManager ?: return false

      val activeNetwork = cm.activeNetwork
      val capabilities = activeNetwork?.let { cm.getNetworkCapabilities(it) }

      capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } catch (e: Exception) {
      ClerkLog.w("NetworkConnectivityMonitor: Error checking connectivity: ${e.message}")
      false
    }
  }

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

  fun isCurrentlyConnected(): Boolean =
    if (isMonitoring) checkCurrentConnectivity() else _isConnected.value

  internal fun resetForTesting() {
    stop()
    _isConnected.value = true
    wasDisconnected = false
  }
}
