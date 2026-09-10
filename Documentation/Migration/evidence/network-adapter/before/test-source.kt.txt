package com.clerk.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowConnectivityManager
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities
import org.robolectric.shadows.ShadowNetworkInfo

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 36], manifest = Config.NONE)
class AndroidNetworkTest {
  private lateinit var context: Context
  private lateinit var manager: ConnectivityManager
  private lateinit var shadow: ShadowConnectivityManager
  private val stops = mutableListOf<() -> Unit>()

  @Before
  fun setup() {
    context = RuntimeEnvironment.getApplication()
    manager = context.getSystemService(ConnectivityManager::class.java)
    shadow = shadowOf(manager)
    shadow.setNetworkCallbacksEnabled(false)
    shadow.clearAllNetworks()
    shadow.setActiveNetworkInfo(null)
  }

  @After
  fun close() {
    stops.forEach { it() }
    check(shadow.networkCallbacks.isEmpty())
  }

  private fun capabilities(internet: Boolean, validated: Boolean): NetworkCapabilities {
    val value = ShadowNetworkCapabilities.newInstance()
    shadowOf(value).clearCapabilities()
    if (internet) shadowOf(value).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    if (validated) shadowOf(value).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    return value
  }

  @Suppress(
    "DEPRECATION"
  ) // Robolectric configures its default-network fixture through NetworkInfo.
  private fun activeNetwork(internet: Boolean, validated: Boolean) {
    shadow.setActiveNetworkInfo(
      ShadowNetworkInfo.newInstance(
        NetworkInfo.DetailedState.CONNECTED,
        ConnectivityManager.TYPE_WIFI,
        0,
        true,
        true,
      )
    )
    shadow.setNetworkCapabilities(
      checkNotNull(manager.activeNetwork),
      capabilities(internet, validated),
    )
  }

  private fun subscribe(events: MutableList<Boolean>): () -> Unit {
    val stop = subscribeNetworkConnectivity(context) { events += it }
    stops += stop
    return stop
  }

  @Test
  fun initialStateRequiresBothInternetAndValidation() {
    for ((internet, validated) in
      listOf(false to false, false to true, true to false, true to true)) {
      activeNetwork(internet, validated)
      val events = mutableListOf<Boolean>()
      val stop = subscribe(events)
      check(events == listOf(internet && validated))
      check(shadow.networkCallbacks.size == 1)
      stop()
      check(shadow.networkCallbacks.isEmpty())
    }
  }

  @Test
  @Config(sdk = [36])
  fun initiallyOfflineWaitsForValidatedAvailabilityAndReportsLoss() {
    val events = mutableListOf<Boolean>()
    subscribe(events)
    check(events == listOf(false))
    val callback = shadow.networkCallbacks.single()
    val network = ShadowNetwork.newInstance(101)
    callback.onAvailable(network)
    check(events == listOf(false))
    callback.onCapabilitiesChanged(network, capabilities(true, false))
    check(!events.last())
    callback.onCapabilitiesChanged(network, capabilities(true, true))
    check(events.last())
    callback.onLost(network)
    check(!events.last())
  }

  @Test
  fun currentNetworkCapabilityChangesUseTheDeliveredCapabilities() {
    activeNetwork(true, true)
    val network = checkNotNull(manager.activeNetwork)
    val events = mutableListOf<Boolean>()
    subscribe(events)
    val callback = shadow.networkCallbacks.single()
    callback.onCapabilitiesChanged(network, capabilities(false, false))
    check(events == listOf(true, false))
    callback.onCapabilitiesChanged(network, capabilities(true, true))
    check(events == listOf(true, false, true))
  }

  @Test
  fun handoverIgnoresLateEventsFromTheReplacedDefaultNetwork() {
    activeNetwork(true, true)
    val previous = checkNotNull(manager.activeNetwork)
    val events = mutableListOf<Boolean>()
    subscribe(events)
    val callback = shadow.networkCallbacks.single()
    val next = ShadowNetwork.newInstance(102)
    callback.onAvailable(next)
    callback.onCapabilitiesChanged(next, capabilities(true, true))
    events.clear()
    callback.onLost(previous)
    callback.onCapabilitiesChanged(previous, capabilities(false, false))
    check(events.isEmpty())
    callback.onLost(next)
    check(events == listOf(false))
  }

  @Test
  fun subscriptionsHaveIndependentCallbacksAndCleanup() {
    val first = mutableListOf<Boolean>()
    val second = mutableListOf<Boolean>()
    val stopFirst = subscribe(first)
    val firstCallback = shadow.networkCallbacks.single()
    subscribe(second)
    check(shadow.networkCallbacks.size == 2)
    stopFirst()
    check(shadow.networkCallbacks.size == 1 && firstCallback !in shadow.networkCallbacks)
    val remaining = shadow.networkCallbacks.single()
    val network = ShadowNetwork.newInstance(103)
    remaining.onAvailable(network)
    remaining.onCapabilitiesChanged(network, capabilities(true, true))
    check(first == listOf(false) && !second.first() && second.last())
  }

  @Test
  @Config(sdk = [24, 25])
  fun olderAndroidAvailabilityRestoresEligibilityWithoutCapabilityChange() {
    val events = mutableListOf<Boolean>()
    subscribe(events)
    check(events == listOf(false))
    val callback = shadow.networkCallbacks.single()
    val network = ShadowNetwork.newInstance(104)
    // Android 7 can announce an existing default without a following capabilities event.
    callback.onAvailable(network)
    check(events.last()) {
      "Availability left HTTP ineligible while waiting for an optional callback"
    }
    callback.onCapabilitiesChanged(network, capabilities(true, false))
    check(!events.last())
    callback.onLost(network)
    check(!events.last())
  }

  @Test
  @Config(shadows = [RegistrationFailureConnectivityManager::class])
  fun registrationFailureLeavesRequestsEligibleInsteadOfPermanentlyOffline() {
    val events = mutableListOf<Boolean>()
    val stop = subscribe(events)
    check(events.last())
    check(shadow.networkCallbacks.isEmpty())
    stop()
  }
}

@Implements(ConnectivityManager::class)
class RegistrationFailureConnectivityManager : ShadowConnectivityManager() {
  @Implementation
  override fun registerDefaultNetworkCallback(callback: ConnectivityManager.NetworkCallback) {
    throw SecurityException("fixture registration denied")
  }
}
