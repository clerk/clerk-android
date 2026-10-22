package com.clerk.api.appversion

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AppVersionSupportTest {
  @Before
  fun setUp() {
    AppVersionSupport.reset()
    AppVersionSupport.configure(AppBuild(TEST_PACKAGE_NAME, versionCode = 41))
  }

  @After
  fun tearDown() {
    AppVersionSupport.reset()
  }

  @Test
  fun `an app with no minimum is supported`() {
    AppVersionSupport.applyFetchedEnvironment(
      appVersionTestEnvironment(androidPolicy("50", packageName = "com.example.other"))
    )

    assertEquals(AppVersionSupportStatus(isSupported = true, currentVersion = 41), status())
  }

  @Test
  fun `an app older than its minimum is unsupported`() {
    AppVersionSupport.applyFetchedEnvironment(appVersionTestEnvironment(androidPolicy("42")))

    assertEquals(
      AppVersionSupportStatus(
        isSupported = false,
        currentVersion = 41,
        minimumVersion = 42,
        updateUrl = TEST_PLAY_URL,
      ),
      status(),
    )
  }

  @Test
  fun `an app at its minimum is supported`() {
    AppVersionSupport.applyFetchedEnvironment(appVersionTestEnvironment(androidPolicy("41")))

    assertTrue(status().isSupported)
  }

  @Test
  fun `an unreadable minimum fails open`() {
    listOf("", "0", "4.2", "042", "abc").forEach { minimumVersion ->
      AppVersionSupport.applyFetchedEnvironment(
        appVersionTestEnvironment(androidPolicy(minimumVersion))
      )

      assertTrue(minimumVersion, status().isSupported)
      assertNull(minimumVersion, status().minimumVersion)
    }
  }

  @Test
  fun `an update URL that isn't a web page is dropped`() {
    listOf("market://details?id=com.example.app", "javascript:alert(1)", "https://").forEach {
      AppVersionSupport.applyFetchedEnvironment(
        appVersionTestEnvironment(androidPolicy("42", updateUrl = it))
      )

      assertFalse(it, status().isSupported)
      assertNull(it, status().updateUrl)
    }
  }

  @Test
  fun `a supported response keeps the minimum and store page the environment set`() {
    AppVersionSupport.applyFetchedEnvironment(appVersionTestEnvironment(androidPolicy("40")))

    AppVersionSupport.applySupportedAppVersion(AppVersionSupport.generation)

    assertEquals(
      AppVersionSupportStatus(
        isSupported = true,
        currentVersion = 41,
        minimumVersion = 40,
        updateUrl = TEST_PLAY_URL,
      ),
      status(),
    )
  }

  @Test
  fun `a supported response drops a minimum the server no longer enforces`() {
    AppVersionSupport.applyFetchedEnvironment(appVersionTestEnvironment(androidPolicy("42")))

    AppVersionSupport.applySupportedAppVersion(AppVersionSupport.generation)

    assertEquals(AppVersionSupportStatus(isSupported = true, currentVersion = 41), status())
  }

  @Test
  fun `a rejected request blocks the app until a later response marks it supported`() {
    AppVersionSupport.applyFetchedEnvironment(appVersionTestEnvironment())
    val generation = AppVersionSupport.generation

    AppVersionSupport.applyUnsupportedAppVersion(
      buildJsonObject {
        put("current_version", JsonPrimitive("41"))
        put("minimum_version", JsonPrimitive("43"))
        put("update_url", JsonPrimitive(TEST_PLAY_URL))
      },
      generation,
    )

    assertEquals(
      AppVersionSupportStatus(
        isSupported = false,
        currentVersion = 41,
        minimumVersion = 43,
        updateUrl = TEST_PLAY_URL,
      ),
      status(),
    )

    AppVersionSupport.applySupportedAppVersion(generation)

    assertTrue(status().isSupported)
  }

  @Test
  fun `a rejected request without meta still blocks the app`() {
    AppVersionSupport.applyUnsupportedAppVersion(meta = null, AppVersionSupport.generation)

    assertEquals(AppVersionSupportStatus(isSupported = false, currentVersion = 41), status())
  }

  @Test
  fun `a rejected request falls back to the environment for what its meta leaves out`() {
    AppVersionSupport.applyFetchedEnvironment(appVersionTestEnvironment(androidPolicy("42")))

    AppVersionSupport.applyUnsupportedAppVersion(
      buildJsonObject { put("update_url", JsonPrimitive("")) },
      AppVersionSupport.generation,
    )

    assertEquals(
      AppVersionSupportStatus(
        isSupported = false,
        currentVersion = 41,
        minimumVersion = 42,
        updateUrl = TEST_PLAY_URL,
      ),
      status(),
    )
  }

  @Test
  fun `a fetched environment replaces what an earlier response said`() {
    AppVersionSupport.applyUnsupportedAppVersion(meta = null, AppVersionSupport.generation)

    AppVersionSupport.applyFetchedEnvironment(appVersionTestEnvironment(androidPolicy("41")))

    assertTrue(status().isSupported)
  }

  @Test
  fun `a saved environment doesn't replace what a response said`() {
    AppVersionSupport.applyUnsupportedAppVersion(meta = null, AppVersionSupport.generation)

    AppVersionSupport.applySavedEnvironment(appVersionTestEnvironment())

    assertFalse(status().isSupported)
  }

  @Test
  fun `a saved environment blocks the app before any response`() {
    AppVersionSupport.applySavedEnvironment(appVersionTestEnvironment(androidPolicy("42")))

    assertFalse(status().isSupported)
  }

  @Test
  fun `a response to a request sent before reset is ignored`() {
    val generation = AppVersionSupport.generation
    AppVersionSupport.reset()
    AppVersionSupport.configure(AppBuild(TEST_PACKAGE_NAME, versionCode = 41))

    AppVersionSupport.applyUnsupportedAppVersion(meta = null, generation)

    assertTrue(status().isSupported)
  }

  @Test
  fun `reset forgets the block and the build`() {
    AppVersionSupport.applyFetchedEnvironment(appVersionTestEnvironment(androidPolicy("42")))

    AppVersionSupport.reset()

    assertEquals(AppVersionSupportStatus(isSupported = true, currentVersion = null), status())
    assertNull(AppVersionSupport.build)
  }

  @Test
  fun `a build reads this app's versionCode`() {
    setVersionCode(41)

    assertEquals(
      AppBuild(context.packageName, versionCode = 41),
      AppBuild.from(context),
    )
  }

  @Test
  fun `a versionCode below 1 can't be checked, so the app is supported`() {
    setVersionCode(0)
    AppVersionSupport.configure(AppBuild.from(context))

    AppVersionSupport.applyFetchedEnvironment(appVersionTestEnvironment(androidPolicy("42")))

    assertNull(AppVersionSupport.build)
    assertEquals(AppVersionSupportStatus(isSupported = true, currentVersion = null), status())
  }

  private fun setVersionCode(versionCode: Long) {
    shadowOf(context.packageManager)
      .getInternalMutablePackageInfo(context.packageName)
      .longVersionCode = versionCode
  }

  private val context: Application
    get() = ApplicationProvider.getApplicationContext()

  private fun status() = AppVersionSupport.status.value
}
