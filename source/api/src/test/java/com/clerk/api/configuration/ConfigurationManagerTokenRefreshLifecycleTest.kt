package com.clerk.api.configuration

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.configuration.connectivity.NetworkConnectivityMonitor
import com.clerk.api.configuration.lifecycle.AppLifecycleListener
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.AuthConfig
import com.clerk.api.network.model.environment.DisplayConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.UserSettings
import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.session.fetchToken
import com.clerk.api.storage.StorageHelper
import com.clerk.api.user.User
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConfigurationManagerTokenRefreshLifecycleTest {
  private lateinit var context: Context
  private lateinit var manager: ConfigurationManager
  private var tokenFetches = 0

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    StorageHelper.initialize(context)
    StorageHelper.reset(context)
    Clerk.reset()
    mockkObject(Client.Companion, Environment.Companion)
    every { Client.serializer() } answers { callOriginal() }
    every { Environment.serializer() } answers { callOriginal() }
    mockkObject(NetworkConnectivityMonitor)
    every { NetworkConnectivityMonitor.configure(any(), any()) } returns Unit
    // Drive the real AppLifecycleListener through the real process lifecycle so the order in
    // which it flips its background flag and invokes the foreground callback is under test.
    processLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
    mockkStatic("com.clerk.api.session.SessionKt")
    coEvery { any<Session>().fetchToken(any()) } coAnswers
      {
        tokenFetches++
        ClerkResult.success(TokenResource(jwt = "jwt"))
      }
    coEvery { Client.get() } returns ClerkResult.success(signedInClient())
    coEvery { Environment.get() } returns ClerkResult.success(testEnvironment())
  }

  @After
  fun tearDown() {
    if (processLifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
      processLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }
    if (::manager.isInitialized) manager.reset()
    AppLifecycleListener.stop()
    Clerk.reset()
    unmockkAll()
    StorageHelper.reset(context)
    NetworkConnectivityMonitor.resetForTesting()
  }

  @Test
  fun `token refresh pauses in the background and resumes immediately on foreground`() = runTest {
    initialize()
    runCurrent()
    val fetchesAtStart = tokenFetches
    assertTrue(fetchesAtStart >= 1)

    advanceTimeBy(5_001)
    assertEquals(fetchesAtStart + 1, tokenFetches)

    processLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    assertTrue(AppLifecycleListener.isInBackground)
    advanceTimeBy(60_000)
    val fetchesWhileBackgrounded = tokenFetches - (fetchesAtStart + 1)
    assertEquals(0, fetchesWhileBackgrounded)

    processLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
    runCurrent()
    val fetchesAfterForeground = tokenFetches
    assertTrue(fetchesAfterForeground > fetchesAtStart + 1)

    advanceTimeBy(5_001)
    assertTrue(tokenFetches > fetchesAfterForeground)
  }

  @Test
  fun `foreground callback runs after the background flag is cleared`() {
    val backgroundFlagSeenByCallback = mutableListOf<Boolean>()
    AppLifecycleListener.configure {
      backgroundFlagSeenByCallback += AppLifecycleListener.isInBackground
    }

    processLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    assertTrue(AppLifecycleListener.isInBackground)
    processLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)

    // The callback restarts the token refresh loop, which exits immediately if it still sees
    // isInBackground == true.
    assertEquals(listOf(false), backgroundFlagSeenByCallback)
    assertFalse(AppLifecycleListener.isInBackground)
  }

  private val processLifecycle: LifecycleRegistry
    get() = ProcessLifecycleOwner.get().lifecycle as LifecycleRegistry

  private fun TestScope.initialize() {
    manager = ConfigurationManager(backgroundScope)
    manager.configure(
      context = context,
      publishableKey = "pk_test_token_refresh_lifecycle",
      options = ClerkConfigurationOptions(proxyUrl = "https://proxy.example.com"),
    )
  }

  private fun signedInClient(): Client {
    val session = mockk<Session>(relaxed = true)
    every { session.id } returns "sess_123"
    every { session.status } returns Session.SessionStatus.ACTIVE
    every { session.user } returns mockk<User>(relaxed = true)
    return Client(id = "client_123", sessions = listOf(session), lastActiveSessionId = "sess_123")
  }

  private fun testEnvironment(): Environment =
    Environment(
      authConfig = AuthConfig(singleSessionMode = false),
      displayConfig =
        DisplayConfig(
          applicationName = "Token Refresh App",
          branded = true,
          logoImageUrl = "https://example.com/logo.png",
          homeUrl = "/",
          privacyPolicyUrl = null,
          termsUrl = null,
          googleOneTapClientId = null,
        ),
      userSettings =
        UserSettings(
          attributes = emptyMap(),
          signUp =
            UserSettings.SignUpUserSettings(
              customActionRequired = false,
              progressive = false,
              mode = "public",
              legalConsentEnabled = false,
            ),
          social = emptyMap(),
          actions = UserSettings.Actions(),
          passkeySettings = null,
        ),
    )
}
