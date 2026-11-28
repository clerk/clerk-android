package com.clerk.ui.reverification

import android.app.Activity
import androidx.test.core.app.ApplicationProvider
import com.clerk.api.Clerk
import com.clerk.api.biometriccredential.BiometricCredentialAvailability
import com.clerk.api.biometriccredential.BiometricCredentials
import com.clerk.api.session.Session
import com.clerk.api.session.SessionVerification
import com.clerk.api.session.startVerification
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class ReverificationActivityTest {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private val host = Robolectric.buildActivity(Activity::class.java).setup().get()
  private val sessions = MutableStateFlow<Session?>(null)

  @Before
  fun setUp() {
    mockkObject(Clerk)
    every { Clerk.foregroundActivity } returns host
    every { Clerk.sessionFlow } returns sessions
    every { Clerk.session } answers { sessions.value }
    every { Clerk.isInitialized } returns MutableStateFlow(true)
    mockkObject(BiometricCredentials)
    every { BiometricCredentials.currentUserLocalAvailability() } returns
      BiometricCredentialAvailability.Unavailable(
        BiometricCredentialAvailability.UnavailableReason.NO_LOCAL_CREDENTIAL
      )
    mockkStatic("com.clerk.api.session.SessionVerificationExtensionsKt")
    coEvery { any<Session>().startVerification(any()) } coAnswers { awaitCancellation() }
  }

  @After
  fun tearDown() {
    scope.cancel()
    ShadowLooper.idleMainLooper()
    unmockkAll()
  }

  private fun present(sessionId: String = "sess_1"): Deferred<ReverificationOutcome> =
    scope
      .async {
        ReverificationPresenter.shared.present(
          SessionVerification.Level.SECOND_FACTOR,
          SignedInIdentity(clientId = "client_1", sessionId = sessionId),
        )
      }
      .also { ShadowLooper.idleMainLooper() }

  private fun launchStartedScreen(): ReverificationActivity {
    val intent = shadowOf(host).nextStartedActivity
    assertEquals(ReverificationActivity::class.java.name, intent.component?.className)
    return Robolectric.buildActivity(ReverificationActivity::class.java, intent).setup().get()
  }

  @Test
  fun aScreenWithoutAPendingRequestCloses() {
    val intent =
      ReverificationActivity.intent(ApplicationProvider.getApplicationContext(), "missing")
    val activity = Robolectric.buildActivity(ReverificationActivity::class.java, intent).setup()

    assertTrue(activity.get().isFinishing)
  }

  @Test
  fun aScreenForASessionThatIsNoLongerSignedInClosesAsCancelled() {
    val outcome = present()
    val activity = launchStartedScreen()
    ShadowLooper.idleMainLooper()

    assertTrue(activity.isFinishing)
    assertEquals(ReverificationOutcome.CANCELLED, outcome.getCompleted())
  }

  @Test
  fun closingTheScreenEndsTheRequestAsCancelled() {
    sessions.value = mockk(relaxed = true) { every { id } returns "sess_1" }
    val outcome = present()
    val controller =
      Robolectric.buildActivity(
          ReverificationActivity::class.java,
          shadowOf(host).nextStartedActivity,
        )
        .setup()
    assertFalse(controller.get().isFinishing)

    controller.get().onBackPressedDispatcher.onBackPressed()
    ShadowLooper.idleMainLooper()

    assertTrue(controller.get().isFinishing)
    controller.pause().stop().destroy()
    ShadowLooper.idleMainLooper()

    assertEquals(ReverificationOutcome.CANCELLED, outcome.getCompleted())
  }

  @Test
  fun aRequestThatEndsElsewhereClosesTheScreen() {
    sessions.value = mockk(relaxed = true) { every { id } returns "sess_1" }
    val outcome = present()
    val activity = launchStartedScreen()
    assertFalse(activity.isFinishing)

    outcome.cancel()
    ShadowLooper.idleMainLooper()

    assertTrue(activity.isFinishing)
  }
}
