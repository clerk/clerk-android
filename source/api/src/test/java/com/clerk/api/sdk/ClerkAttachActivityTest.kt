package com.clerk.api.sdk

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.clerk.api.Clerk
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies the activity-tracking guarantees that callers rely on for Credential Manager flows
 * (Google sign-in, passkeys). Without these, `Clerk.credentialActivity()` returns null on cold
 * start in any host that calls [Clerk.initialize] after the host Activity has already passed
 * onResume — e.g. React Native bridges, late-init flows behind a permission gate.
 */
@RunWith(RobolectricTestRunner::class)
class ClerkAttachActivityTest {

  @After
  fun tearDown() {
    // Clear the Clerk-singleton state we mutated.
    val field = Clerk::class.java.getDeclaredField("currentActivity")
    field.isAccessible = true
    field.set(Clerk, null)
    unmockkAll()
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

    // Same instance through the WeakReference while it's alive.
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

  /**
   * Smoke test for the [Context]-walk inside [Clerk.initialize]. We exercise the helper directly
   * rather than going through `initialize()` (which would spin up the configuration manager); the
   * helper's contract is the load-bearing piece for the seeding guarantee.
   */
  @Test
  fun `Context findActivityOrNull walks ContextWrapper chain`() {
    val activity = mockk<Activity>(relaxed = true)
    val outer: Context = object : ContextWrapper(activity) {}

    val helper =
      Clerk::class.java.getDeclaredMethod("findActivityOrNull", Context::class.java).apply {
        isAccessible = true
      }

    val found = helper.invoke(Clerk, outer) as Activity?
    assertSame(activity, found)
  }

  @Test
  fun `Context findActivityOrNull returns null when no Activity in chain`() {
    val plain = mockk<Context>(relaxed = true)
    every { plain.applicationContext } returns plain

    val helper =
      Clerk::class.java.getDeclaredMethod("findActivityOrNull", Context::class.java).apply {
        isAccessible = true
      }

    val found = helper.invoke(Clerk, plain) as Activity?
    assertNull(found)
  }
}
