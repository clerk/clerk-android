package com.clerk.api.appversion

import android.app.Activity
import android.content.ComponentName
import android.os.Looper
import com.clerk.api.Clerk
import java.lang.ref.WeakReference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class UpdateRequiredViewLauncherTest {
  private lateinit var controller: ActivityController<Activity>
  private val activity: Activity
    get() = controller.get()

  @Before
  fun setUp() {
    AppVersionSupport.reset()
    AppVersionSupport.configure(AppBuild(TEST_PACKAGE_NAME, versionCode = 41))
    controller = Robolectric.buildActivity(Activity::class.java).setup()
  }

  @After
  fun tearDown() {
    UpdateRequiredViewLauncher.stop()
    AppVersionSupport.reset()
    Clerk.currentActivity = null
    controller.destroy()
  }

  @Test
  fun `an unsupported app shows the update screen when an activity resumes`() {
    includeUiLibrary()
    UpdateRequiredViewLauncher.start(activity, enabled = true)
    block()

    UpdateRequiredViewLauncher.onActivityResumed(activity)

    assertEquals(
      UpdateRequiredViewLauncher.UI_LIBRARY_ACTIVITY_CLASS_NAME,
      shadowOf(activity).nextStartedActivity?.component?.className,
    )
  }

  @Test
  fun `the update screen shows over the current activity when the app becomes unsupported`() {
    includeUiLibrary()
    Clerk.currentActivity = WeakReference(activity)
    UpdateRequiredViewLauncher.start(activity, enabled = true)

    block()
    shadowOf(Looper.getMainLooper()).idle()

    assertEquals(
      UpdateRequiredViewLauncher.UI_LIBRARY_ACTIVITY_CLASS_NAME,
      shadowOf(activity).nextStartedActivity?.component?.className,
    )
  }

  @Test
  fun `a supported app shows nothing`() {
    includeUiLibrary()
    UpdateRequiredViewLauncher.start(activity, enabled = true)

    UpdateRequiredViewLauncher.onActivityResumed(activity)

    assertNull(shadowOf(activity).nextStartedActivity)
  }

  @Test
  fun `an app that turned the update screen off shows nothing`() {
    includeUiLibrary()
    UpdateRequiredViewLauncher.start(activity, enabled = false)
    block()

    UpdateRequiredViewLauncher.onActivityResumed(activity)

    assertNull(shadowOf(activity).nextStartedActivity)
  }

  @Test
  fun `an app without the UI library shows nothing`() {
    UpdateRequiredViewLauncher.start(activity, enabled = true)
    block()

    UpdateRequiredViewLauncher.onActivityResumed(activity)

    assertNull(shadowOf(activity).nextStartedActivity)
  }

  private fun block() {
    AppVersionSupport.applyUnsupportedAppVersion(meta = null, AppVersionSupport.nextRequest())
  }

  private fun includeUiLibrary() {
    shadowOf(activity.packageManager)
      .addActivityIfNotPresent(
        ComponentName(
          activity.packageName,
          UpdateRequiredViewLauncher.UI_LIBRARY_ACTIVITY_CLASS_NAME,
        )
      )
  }
}
