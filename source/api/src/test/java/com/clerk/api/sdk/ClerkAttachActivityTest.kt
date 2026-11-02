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

@RunWith(RobolectricTestRunner::class)
class ClerkAttachActivityTest {

  @After
  fun tearDown() {
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
    val activity = mockk<Activity>(relaxed = true)
    Clerk.attachActivity(activity)

    val ref =
      Clerk::class.java.getDeclaredField("currentActivity").apply { isAccessible = true }.get(Clerk)
        as java.lang.ref.WeakReference<*>?

    assertSame(activity, ref?.get())
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
