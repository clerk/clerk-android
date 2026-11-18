package com.clerk.api.sdk

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.configuration.connectivity.NetworkConnectivityMonitor
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.storage.StorageHelper
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Verifies the activity-tracking guarantees that callers rely on for Credential Manager flows
 * (Google sign-in, passkeys). Without these, `Clerk.credentialActivity()` returns null on cold
 * start in any host that calls [Clerk.initialize] after the host Activity has already passed
 * onResume — e.g. React Native bridges, late-init flows behind a permission gate.
 */
@RunWith(RobolectricTestRunner::class)
class ClerkAttachActivityTest {

  @Before
  fun setUp() {
    StorageHelper.initialize(RuntimeEnvironment.getApplication())
    Clerk.reset()
  }

  @After
  fun tearDown() {
    Clerk.reset()
    unmockkAll()
    StorageHelper.reset(RuntimeEnvironment.getApplication())
    NetworkConnectivityMonitor.resetForTesting()
  }

  @Test
  fun `attachActivity seeds credentialActivity`() {
    val activity = mockk<Activity>(relaxed = true)
    Clerk.attachActivity(activity)

    val seeded = readCredentialActivity()

    assertSame(activity, seeded)
  }

  @Test
  fun `attachActivity holds Activity weakly`() {
    var activity: Activity? = mockk<Activity>(relaxed = true)
    Clerk.attachActivity(activity!!)

    val ref =
      Clerk::class.java.getDeclaredField("currentActivity").apply { isAccessible = true }.get(Clerk)
        as java.lang.ref.WeakReference<*>?

    assertSame(activity, ref?.get())

    // Drop the strong reference; we don't assert collection happens (the GC
    // is non-deterministic in unit tests), only that the field type lets it.
    activity = null
    @Suppress("UNUSED_EXPRESSION") activity
  }

  @Test
  fun `credentialActivity returns null when no activity attached`() {
    val seeded = readCredentialActivity()

    assertNull(seeded)
  }

  /**
   * `credentialActivity` is a Kotlin `internal` function on the Clerk singleton. Kotlin mangles its
   * JVM name with a build-flavor suffix (`$api_debug` in tests, `$api_release` in published
   * artifacts), so we locate it dynamically by base name rather than hardcoding the suffix.
   */
  private fun readCredentialActivity(): Activity? {
    val method =
      Clerk::class
        .java
        .declaredMethods
        .first { it.name == "credentialActivity" || it.name.startsWith("credentialActivity\$") }
        .apply { isAccessible = true }
    return method.invoke(Clerk) as Activity?
  }

  @Test
  fun `initialize seeds credentialActivity from an Activity wrapped in a ContextWrapper`() =
    runBlocking {
      val activity = Robolectric.buildActivity(Activity::class.java).create().get()
      val wrapper: Context = object : ContextWrapper(activity) {}
      mockkObject(Client.Companion)
      mockkObject(Environment.Companion)
      coEvery { Client.get() } returns ClerkResult.success(Client())
      coEvery { Client.getSkippingClientId() } returns ClerkResult.success(Client())
      coEvery { Environment.get() } returns ClerkResult.success(mockk<Environment>(relaxed = true))

      Clerk.initialize(
        context = wrapper,
        publishableKey = PUBLISHABLE_KEY,
        options = ClerkConfigurationOptions(proxyUrl = PROXY_URL),
      )
      withTimeout(TIMEOUT_MS) { Clerk.isInitialized.first { it } }

      assertSame(activity, readCredentialActivity())
    }

  private companion object {
    const val PUBLISHABLE_KEY = "pk_test_attach_activity"
    const val PROXY_URL = "https://proxy.example.com/__clerk"
    const val TIMEOUT_MS = 5_000L
  }
}
