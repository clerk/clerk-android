package com.clerk.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.lang.ref.WeakReference

internal fun observeNetworkConnectivity(
  runtime: CoreRuntime,
  subscribe: ((Boolean) -> Unit) -> (() -> Unit),
) {
  val reference = WeakReference(runtime)
  val stop = subscribe { reference.get()?.setNetworkOnline(it) }
  runtime.addTeardown(stop)
}

internal fun subscribeNetworkConnectivity(context: Context, receive: (Boolean) -> Unit): () -> Unit {
  val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java) ?: return {}
  fun NetworkCapabilities?.permitsInternet() = this?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
    hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  try {
    var current = manager.activeNetwork
    receive(current?.let(manager::getNetworkCapabilities).permitsInternet())
    val callback = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) { current = network }
      override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
        if (current == network) receive(capabilities.permitsInternet())
      }
      override fun onLost(network: Network) {
        if (current == network) { current = null; receive(false) }
      }
    }
    manager.registerDefaultNetworkCallback(callback)
    return { runCatching { manager.unregisterNetworkCallback(callback) }; Unit }
  } catch (_: RuntimeException) {
    // Unknown connectivity must still allow HTTP (e.g. a host removed the permission).
    receive(true)
    return {}
  }
}
