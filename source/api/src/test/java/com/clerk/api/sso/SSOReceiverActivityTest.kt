package com.clerk.api.sso

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.clerk.api.redirect.ReceiverDelivery
import com.clerk.api.redirect.RedirectCoordinator
import io.mockk.every
import io.mockk.justRun
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
  fun callbackThatDoesNotBelongToThePendingFlowIsNotForwarded() {
    val callbackUri = Uri.parse("clerk://example.callback?state=forged")
    mockkObject(RedirectCoordinator)
    every { RedirectCoordinator.receiverDelivery(callbackUri) } returns ReceiverDelivery.DROP

    val activity = createReceiver(callbackUri)

    assertNull(Shadows.shadowOf(activity).nextStartedActivity)
    assertTrue(activity.isFinishing)
    verify(exactly = 1) { RedirectCoordinator.receiverDelivery(callbackUri) }
  }

  @Test
  fun magicLinkWhileAFlowIsPendingCompletesWithoutStartingTheManager() {
    val magicLink = Uri.parse("clerk://example.callback?flow_id=flow_123&approval_token=tok")
    mockkObject(RedirectCoordinator)
    every { RedirectCoordinator.receiverDelivery(magicLink) } returns
      ReceiverDelivery.COMPLETE_IN_BACKGROUND
    justRun { RedirectCoordinator.dispatchInBackground(magicLink) }

    val activity = createReceiver(magicLink)

    // Starting the singleTask manager would clear the pending flow's Custom Tab.
    assertNull(Shadows.shadowOf(activity).nextStartedActivity)
    assertTrue(activity.isFinishing)
    verify(exactly = 1) { RedirectCoordinator.dispatchInBackground(magicLink) }
  }

  @Test
  fun pendingFlowCallbackIsForwardedToManager() {
    val callbackUri = Uri.parse("clerk://example.callback?state=expected")
    mockkObject(RedirectCoordinator)
    every { RedirectCoordinator.receiverDelivery(callbackUri) } returns ReceiverDelivery.FORWARD

    val activity = createReceiver(callbackUri)

    val forwardedIntent = Shadows.shadowOf(activity).nextStartedActivity
    assertEquals(SSOManagerActivity::class.java.name, forwardedIntent.component?.className)
    assertEquals(callbackUri, forwardedIntent.data)
  }

  @Test
  fun unrecognizedExplicitIntentIsNotForwardedToManager() {
    val callbackUri = Uri.parse("https://attacker.example/fake-sign-in")
    mockkObject(RedirectCoordinator)

    val activity = createReceiver(callbackUri)

    assertNull(Shadows.shadowOf(activity).nextStartedActivity)
    assertTrue(activity.isFinishing)
    verify(exactly = 0) { RedirectCoordinator.receiverDelivery(any()) }
  }

  private fun createReceiver(callbackUri: Uri): SSOReceiverActivity {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent = Intent(context, SSOReceiverActivity::class.java).apply { data = callbackUri }
    return Robolectric.buildActivity(SSOReceiverActivity::class.java, intent).create().get()
  }
}
