package com.clerk.api.configuration

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.FrameworkIntegrationApi
import com.clerk.api.SharedSessionSyncConfig
import com.clerk.api.configuration.connectivity.NetworkConnectivityMonitor
import com.clerk.api.configuration.lifecycle.AppLifecycleListener
import com.clerk.api.devicekey.JvmDeviceKey
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.io.IOException
import java.util.Base64
import java.util.Collections
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ConfigurationManagerDeviceBindingTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val manager = ConfigurationManager()
  private val tokensSentByRequests = Collections.synchronizedList(mutableListOf<String?>())

  @Before
  fun setUp() {
    StorageHelper.initialize(context)
    StorageHelper.reset(context)
    Clerk.reset()
    mockkObject(Client.Companion, Environment.Companion, NetworkConnectivityMonitor)
    mockkObject(AppLifecycleListener)
    every { Client.serializer() } answers { callOriginal() }
    every { Environment.serializer() } answers { callOriginal() }
    every { NetworkConnectivityMonitor.configure(any(), any()) } returns Unit
    every { AppLifecycleListener.configure(any()) } returns Unit
    coEvery { Client.get() } coAnswers { offlineAfterRecordingToken() }
    coEvery { Client.getSkippingClientId() } coAnswers { offlineAfterRecordingToken() }
    coEvery { Environment.get() } coAnswers { offlineAfterRecordingToken() }
  }

  @After
  fun tearDown() {
    manager.reset()
    Clerk.reset()
    unmockkAll()
    StorageHelper.reset(context)
    NetworkConnectivityMonitor.resetForTesting()
  }

  @Test
  fun `shared session sync drops a device-bound token before the first request`() {
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, BOUND_DEVICE_TOKEN)

    configure(sharedSessionSync = SharedSessionSyncConfig.enabled)
    awaitFirstRequests()

    assertNull(StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
    assertTrue(tokensSentByRequests.all { it == null }, "requests sent $tokensSentByRequests")
  }

  @Test
  fun `shared session sync keeps an unbound token`() {
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, UNBOUND_DEVICE_TOKEN)

    configure(sharedSessionSync = SharedSessionSyncConfig.enabled)
    awaitFirstRequests()

    assertEquals(UNBOUND_DEVICE_TOKEN, StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }

  @Test
  fun `a signing runtime keeps its device-bound token`() {
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, BOUND_DEVICE_TOKEN)

    configure(sharedSessionSync = null)
    awaitFirstRequests()

    assertEquals(BOUND_DEVICE_TOKEN, StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }

  @Test
  fun `deleteDeviceKey deletes the stored device key`() = runBlocking {
    configure(sharedSessionSync = null)
    val before = JvmDeviceKey.getOrCreate().thumbprint

    assertTrue(manager.deleteDeviceKey())

    assertNotEquals(before, JvmDeviceKey.getOrCreate().thumbprint)
  }

  @Test
  fun `deleteDeviceKey does nothing before configure`() = runBlocking {
    val before = JvmDeviceKey.getOrCreate().thumbprint

    assertFalse(manager.deleteDeviceKey())

    assertEquals(before, JvmDeviceKey.getOrCreate().thumbprint)
  }

  @OptIn(FrameworkIntegrationApi::class)
  @Test
  fun `Clerk deleteDeviceKey reports false before initialize`() = runBlocking {
    assertFalse(Clerk.deleteDeviceKey())
  }

  private fun configure(sharedSessionSync: SharedSessionSyncConfig?) {
    manager.configure(
      context = context,
      publishableKey = "pk_test_device_binding",
      options =
        ClerkConfigurationOptions(
          proxyUrl = "https://proxy.example.com",
          sharedSessionSync = sharedSessionSync,
        ),
    )
  }

  private fun awaitFirstRequests() {
    coVerify(timeout = 5_000) { Client.get() }
    coVerify(timeout = 5_000) { Environment.get() }
  }

  private fun <T : Any> offlineAfterRecordingToken(): ClerkResult<T, Nothing> {
    tokensSentByRequests += StorageHelper.loadValue(StorageKey.DEVICE_TOKEN)
    return ClerkResult.unknownFailure(IOException("offline"))
  }

  private companion object {
    val BOUND_DEVICE_TOKEN = jwt("""{"id":"client_bound","cnf":{"jkt":"0ZcOCORZNYy"}}""")
    val UNBOUND_DEVICE_TOKEN = jwt("""{"id":"client_unbound"}""")

    fun jwt(payload: String): String =
      listOf("""{"alg":"RS256"}""", payload).joinToString(".", postfix = ".signature") {
        Base64.getUrlEncoder().withoutPadding().encodeToString(it.toByteArray())
      }
  }
}
