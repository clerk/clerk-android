package com.clerk.ui.updaterequired

import android.app.Application
import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.clerk.api.Clerk
import com.clerk.api.appversion.AppVersionSupportStatus
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class UpdateRequiredViewTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val status = MutableStateFlow(unsupported(updateUrl = PLAY_URL))

  @Before
  fun setUp() {
    mockkObject(Clerk)
    every { Clerk.appVersionSupportStatus } returns status
    every { Clerk.organizationLogoUrlFlow } returns MutableStateFlow(null)
    every { Clerk.applicationName } returns "Acme"
    every { Clerk.supportEmail } returns "support@example.com"
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `tells the user to update the app`() {
    composeTestRule.setContent { UpdateRequiredView() }

    composeTestRule.onNodeWithText("Update required").assertExists()
    composeTestRule
      .onNodeWithText(
        "This version of Acme is no longer supported. To keep your account secure, please " +
          "update to continue."
      )
      .assertExists()
    composeTestRule.onNodeWithText("Can't update?").assertExists()
    composeTestRule.onNodeWithText("Get help").assertExists()
  }

  @Test
  fun `shows this build's version name`() {
    setVersionName("4.8.4")
    composeTestRule.setContent { UpdateRequiredView() }

    composeTestRule.onNodeWithText("Your version").assertExists()
    composeTestRule.onNodeWithText("4.8.4").assertExists()
  }

  @Test
  fun `leaves out the version when the app has no version name`() {
    setVersionName(null)
    composeTestRule.setContent { UpdateRequiredView() }

    composeTestRule.onNodeWithText("Your version").assertDoesNotExist()
  }

  @Test
  fun `opens the store page`() {
    composeTestRule.setContent { UpdateRequiredView() }

    composeTestRule.onNodeWithText("Go to Google Play").performClick()

    val intent =
      shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity
    assertEquals(Intent.ACTION_VIEW, intent.action)
    assertEquals(PLAY_URL, intent.dataString)
  }

  @Test
  fun `names the button after a store other than Google Play`() {
    status.value = unsupported(updateUrl = "https://galaxystore.samsung.com/detail/com.example")
    composeTestRule.setContent { UpdateRequiredView() }

    composeTestRule.onNodeWithText("Update app").assertExists()
    composeTestRule.onNodeWithText("Go to Google Play").assertDoesNotExist()
  }

  @Test
  fun `leaves out the store button and help link it has nothing for`() {
    status.value = unsupported(updateUrl = null)
    every { Clerk.supportEmail } returns null
    composeTestRule.setContent { UpdateRequiredView() }

    composeTestRule.onNodeWithText("Update required").assertExists()
    composeTestRule.onNodeWithText("Go to Google Play").assertDoesNotExist()
    composeTestRule.onNodeWithText("Update app").assertDoesNotExist()
    composeTestRule.onNodeWithText("Get help").assertDoesNotExist()
  }

  private fun setVersionName(versionName: String?) {
    val application = ApplicationProvider.getApplicationContext<Application>()
    shadowOf(application.packageManager)
      .getInternalMutablePackageInfo(application.packageName)
      .versionName = versionName
  }

  private fun unsupported(updateUrl: String?) =
    AppVersionSupportStatus(
      isSupported = false,
      currentVersion = 41,
      minimumVersion = 42,
      updateUrl = updateUrl,
    )

  private companion object {
    const val PLAY_URL = "https://play.google.com/store/apps/details?id=com.example.app"
  }
}
