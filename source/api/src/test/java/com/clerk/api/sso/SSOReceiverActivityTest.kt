package com.clerk.api.sso

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.clerk.api.hostedauth.HostedAuthService
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class SSOReceiverActivityTest {
  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun invalidHostedAuthCallbackIsNotForwarded() {
    val callbackUri = Uri.parse("clerk://example.callback?state=forged")
    mockkObject(HostedAuthService)
    every { HostedAuthService.canHandle(callbackUri) } returns true
    every { HostedAuthService.isValidCallback(callbackUri) } returns false

    val activity = createReceiver(callbackUri)

    assertNull(Shadows.shadowOf(activity).nextStartedActivity)
    assertTrue(activity.isFinishing)
    verify(exactly = 1) { HostedAuthService.isValidCallback(callbackUri) }
  }

  @Test
  fun validHostedAuthCallbackIsForwardedToManager() {
    val callbackUri = Uri.parse("clerk://example.callback?state=expected")
    mockkObject(HostedAuthService)
    every { HostedAuthService.canHandle(callbackUri) } returns true
    every { HostedAuthService.isValidCallback(callbackUri) } returns true

    val activity = createReceiver(callbackUri)

    val forwardedIntent = Shadows.shadowOf(activity).nextStartedActivity
    assertEquals(SSOManagerActivity::class.java.name, forwardedIntent.component?.className)
    assertEquals(callbackUri, forwardedIntent.data)
  }

  private fun createReceiver(callbackUri: Uri): SSOReceiverActivity {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent = Intent(context, SSOReceiverActivity::class.java).apply { data = callbackUri }
    return Robolectric.buildActivity(SSOReceiverActivity::class.java, intent).create().get()
  }
}
