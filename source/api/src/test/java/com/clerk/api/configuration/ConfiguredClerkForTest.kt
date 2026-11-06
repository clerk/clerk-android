package com.clerk.api.configuration

import android.content.Context
import com.clerk.api.Clerk
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow

internal object ConfiguredClerkForTest {
  @Suppress("UNCHECKED_CAST")
  fun setConfigured(context: Context, configured: Boolean) {
    val manager = manager()
    field("context").set(manager, if (configured) WeakReference(context) else null)
    field("hasConfigured").set(manager, configured)
    field("storageInitialized").set(manager, false)
    (field("_isInitialized").get(manager) as MutableStateFlow<Boolean>).value = configured
  }

  fun fenceGeneration(): Int =
    (field("sharedDeviceTokenFenceGeneration").get(manager()) as AtomicInteger).get()

  private fun manager(): ConfigurationManager =
    Clerk::class.java.getDeclaredField("configurationManager").let {
      it.isAccessible = true
      it.get(Clerk) as ConfigurationManager
    }

  private fun field(name: String) =
    ConfigurationManager::class.java.getDeclaredField(name).apply { isAccessible = true }
}
