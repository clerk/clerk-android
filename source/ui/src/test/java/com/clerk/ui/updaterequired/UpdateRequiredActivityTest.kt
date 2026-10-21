package com.clerk.ui.updaterequired

import com.clerk.api.Clerk
import com.clerk.api.appversion.AppVersionSupportStatus
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class UpdateRequiredActivityTest {
  private val status =
    MutableStateFlow(AppVersionSupportStatus(isSupported = false, currentVersion = 41))

  @Before
  fun setUp() {
    mockkObject(Clerk)
    every { Clerk.appVersionSupportStatus } returns status
    every { Clerk.organizationLogoUrlFlow } returns MutableStateFlow(null)
    every { Clerk.applicationName } returns "Acme"
    every { Clerk.supportEmail } returns null
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `back doesn't dismiss the screen`() {
    val activity = Robolectric.buildActivity(UpdateRequiredActivity::class.java).setup().get()

    activity.onBackPressedDispatcher.onBackPressed()

    assertFalse(activity.isFinishing)
  }

  @Test
  fun `the screen closes once the app is supported`() {
    val activity = Robolectric.buildActivity(UpdateRequiredActivity::class.java).setup().get()
    assertFalse(activity.isFinishing)

    status.value = AppVersionSupportStatus(isSupported = true, currentVersion = 41)
    ShadowLooper.idleMainLooper()

    assertTrue(activity.isFinishing)
  }
}
