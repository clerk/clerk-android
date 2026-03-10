package com.clerk.api.sso

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SSOReceiverActivityManifestTest {
  @Test
  fun callbackHostResolvesToSsoReceiverActivity() {
    assertIntentResolvesToReceiver("callback")
  }

  @Test
  fun legacyOauthHostResolvesToSsoReceiverActivity() {
    assertIntentResolvesToReceiver("oauth")
  }

  private fun assertIntentResolvesToReceiver(hostSuffix: String) {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val redirectUri = Uri.parse("clerk://${context.packageName}.$hostSuffix")
    val intent =
      Intent(Intent.ACTION_VIEW, redirectUri)
        .addCategory(Intent.CATEGORY_DEFAULT)
        .addCategory(Intent.CATEGORY_BROWSABLE)

    val matchingActivities =
      context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

    assertTrue(
      "Expected $redirectUri to resolve to ${SSOReceiverActivity::class.java.name}",
      matchingActivities.any { it.activityInfo?.name == SSOReceiverActivity::class.java.name },
    )
  }
}
