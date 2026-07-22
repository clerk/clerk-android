package com.clerk.api.sso

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.clerk.api.Clerk
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SSOReceiverActivityManifestTest {
  @Test
  fun canonicalHostedAuthCallbackResolvesToSsoReceiverActivity() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val originalApplicationId = Clerk.applicationId
    try {
      Clerk.applicationId = context.packageName
      assertIntentResolvesToReceiver(Uri.parse(RedirectConfiguration.DEFAULT_REDIRECT_URL))
    } finally {
      Clerk.applicationId = originalApplicationId
    }
  }

  @Test
  fun legacyOauthHostResolvesToSsoReceiverActivity() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    assertIntentResolvesToReceiver(Uri.parse("clerk://${context.packageName}.oauth"))
  }

  private fun assertIntentResolvesToReceiver(redirectUri: Uri) {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
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
