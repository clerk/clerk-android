package com.clerk.api.sso

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class SSOManagerActivityTest {

  @Before
  fun setup() {
    // Ensure AppCompat theme for AppCompatActivity
    ApplicationProvider.getApplicationContext<Application>()
      .setTheme(androidx.appcompat.R.style.Theme_AppCompat)
  }

  @Test
  fun authorizationComplete_setsResultOk_whenDataPresent() {
    mockkObject(SSOService)
    every { SSOService.hasPendingExternalAccountConnection() } returns false
    coJustRun { SSOService.completeAuthenticateWithRedirect(any()) }

    val app = ApplicationProvider.getApplicationContext<Application>()
    val responseUri = Uri.parse("clerk://callback?rotating_token_nonce=abc")
    val intent =
      SSOManagerActivity.createResponseHandlingIntent(app, responseUri).apply {
        putExtra(com.clerk.api.Constants.Storage.KEY_AUTHORIZATION_STARTED, true)
      }

    val controller = Robolectric.buildActivity(SSOManagerActivity::class.java, intent)
    val activity = controller.create().resume().get()

    val shadow = Shadows.shadowOf(activity)
    assertEquals(Activity.RESULT_OK, shadow.resultCode)
  }

  @Test
  fun authorizationCanceled_setsResultCanceled_whenNoData() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val intent =
      SSOManagerActivity.createBaseIntent(app).apply {
        putExtra(com.clerk.api.Constants.Storage.KEY_AUTHORIZATION_STARTED, true)
      }

    val controller = Robolectric.buildActivity(SSOManagerActivity::class.java, intent)
    val activity = controller.create().resume().get()

    val shadow = Shadows.shadowOf(activity)
    assertEquals(Activity.RESULT_CANCELED, shadow.resultCode)
  }

  @Test
  fun authorizationComplete_isCalledOnlyOnce_whenResumedTwice() {
    mockkObject(SSOService)
    every { SSOService.hasPendingExternalAccountConnection() } returns false
    coJustRun { SSOService.completeAuthenticateWithRedirect(any()) }

    val app = ApplicationProvider.getApplicationContext<Application>()
    val responseUri = Uri.parse("clerk://callback?rotating_token_nonce=abc")
    val intent =
      SSOManagerActivity.createResponseHandlingIntent(app, responseUri).apply {
        putExtra(com.clerk.api.Constants.Storage.KEY_AUTHORIZATION_STARTED, true)
      }

    val controller = Robolectric.buildActivity(SSOManagerActivity::class.java, intent)
    controller.create().resume()
    // Simulate another onResume before finish()
    controller.pause().resume()

    coVerify(exactly = 1) { SSOService.completeAuthenticateWithRedirect(any()) }
  }

  @Test
  fun authorizationComplete_setsResultOk_evenWhenServiceThrows() {
    mockkObject(SSOService)
    every { SSOService.hasPendingExternalAccountConnection() } returns false
    coEvery { SSOService.completeAuthenticateWithRedirect(any()) } throws RuntimeException("boom")

    val app = ApplicationProvider.getApplicationContext<Application>()
    val responseUri = Uri.parse("clerk://callback?rotating_token_nonce=abc")
    val intent =
      SSOManagerActivity.createResponseHandlingIntent(app, responseUri).apply {
        putExtra(com.clerk.api.Constants.Storage.KEY_AUTHORIZATION_STARTED, true)
      }

    val controller = Robolectric.buildActivity(SSOManagerActivity::class.java, intent)
    val activity = controller.create().resume().get()

    val shadow = Shadows.shadowOf(activity)
    // Result is set to OK before the service call
    assertEquals(Activity.RESULT_OK, shadow.resultCode)
  }

  @Test
  fun externalConnectionFlow_callsOnlyCompleteExternalConnection() {
    mockkObject(SSOService)
    every { SSOService.hasPendingExternalAccountConnection() } returns true
    coJustRun { SSOService.completeExternalConnection() }
    coJustRun { SSOService.completeAuthenticateWithRedirect(any()) }

    val app = ApplicationProvider.getApplicationContext<Application>()
    val responseUri = Uri.parse("clerk://callback?rotating_token_nonce=abc")
    val intent =
      SSOManagerActivity.createResponseHandlingIntent(app, responseUri).apply {
        putExtra(com.clerk.api.Constants.Storage.KEY_AUTHORIZATION_STARTED, true)
      }

    val controller = Robolectric.buildActivity(SSOManagerActivity::class.java, intent)
    val activity = controller.create().resume().get()

    coVerify(exactly = 1) { SSOService.completeExternalConnection() }
    coVerify(exactly = 0) { SSOService.completeAuthenticateWithRedirect(any()) }

    val shadow = Shadows.shadowOf(activity)
    assertEquals(Activity.RESULT_OK, shadow.resultCode)
  }

  @Test
  fun completionStarted_persistsAcrossConfigurationChange() {
    mockkObject(SSOService)
    every { SSOService.hasPendingExternalAccountConnection() } returns false
    val gate = CompletableDeferred<Unit>()
    coEvery { SSOService.completeAuthenticateWithRedirect(any()) } coAnswers
      {
        gate.await() // suspend until we release to simulate long-running work
      }

    val app = ApplicationProvider.getApplicationContext<Application>()
    val responseUri = Uri.parse("clerk://callback?rotating_token_nonce=abc")
    val intent =
      SSOManagerActivity.createResponseHandlingIntent(app, responseUri).apply {
        putExtra(com.clerk.api.Constants.Storage.KEY_AUTHORIZATION_STARTED, true)
      }

    val controller = Robolectric.buildActivity(SSOManagerActivity::class.java, intent)
    // First resume: only launches CustomTabs and sets authorizationStarted=true
    controller.create().resume()
    // Simulate configuration change before completion begins
    controller.configurationChange()
    // Next resume: should start completion exactly once
    controller.resume()

    coVerify(exactly = 1) { SSOService.completeAuthenticateWithRedirect(any()) }

    // Release the gate so activity can finish
    gate.complete(Unit)
  }
}
