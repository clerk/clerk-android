package com.clerk.ui.auth

import android.os.Looper
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.clerk.api.Clerk
import com.clerk.api.auth.Auth
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.restorecredentials.RestoreCredentials
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AuthViewRestoreCredentialsTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val creationDispatcher = StandardTestDispatcher()
  private lateinit var defaultCreationScope: CoroutineScope

  @Before
  fun setUp() {
    mockkObject(Clerk)
    every { Clerk.isInitialized } returns MutableStateFlow(true)
    every { Clerk.applicationName } returns "Acme Co"
    every { Clerk.enabledFirstFactorAttributes } returns listOf("email_address")
    every { Clerk.socialProviders } returns emptyMap()
    every { Clerk.passkeyFirstFactorIsEnabled } returns true
    every { Clerk.passkeyAutofillIsEnabled } returns false

    mockkObject(RestoreCredentials)
    coEvery { RestoreCredentials.createIfMissing() } returns ClerkResult.success(Unit)
    defaultCreationScope = RestoreCredentialCreation.scope
    RestoreCredentialCreation.scope = TestScope(creationDispatcher)
  }

  @After
  fun tearDown() {
    unmockkAll()
    RestoreCredentialCreation.scope = defaultCreationScope
  }

  @Test
  fun `does not try a restore credential when the flag is off`() {
    composeTestRule.setContent { AuthView(persistIdentifiers = false) }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Continue").assertExists()
    coVerify(exactly = 0) { RestoreCredentials.signInOnFirstLaunch() }
    coVerify(exactly = 0) { RestoreCredentials.signIn() }
  }

  @Test
  fun `falls through to the start screen when there is no restore credential`() {
    val attempts = AtomicInteger()
    coEvery { RestoreCredentials.signInOnFirstLaunch() } answers
      {
        attempts.incrementAndGet()
        ClerkResult.unknownFailure(noSavedCredential())
      }
    var completions = 0

    composeTestRule.setContent {
      AuthView(
        persistIdentifiers = false,
        enableRestoreCredentials = true,
        onAuthComplete = { completions++ },
      )
    }
    composeTestRule.waitUntil { attempts.get() == 1 }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Continue").assertExists()
    composeTestRule.onNodeWithText("Enter your email").assertExists()
    assertEquals(1, attempts.get())
    assertEquals(0, completions)
  }

  @Test
  fun `creates a restore credential after a sign-up completes`() {
    val auth = mockk<Auth>()
    every { Clerk.auth } returns auth
    coEvery { auth.signUp(any()) } returns ClerkResult.success(completedSignUp())
    var completions = 0

    composeTestRule.setContent {
      AuthView(
        mode = AuthMode.SignUp,
        persistIdentifiers = false,
        enableRestoreCredentials = true,
        onAuthComplete = { completions++ },
      )
    }
    composeTestRule.onNodeWithText("Enter your email").performTextInput("new@example.com")
    composeTestRule.onNodeWithText("Continue").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      shadowOf(Looper.getMainLooper()).idle()
      completions == 1
    }
    creationDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 1) { auth.signUp(any()) }
    coVerify(exactly = 0) { RestoreCredentials.signInOnFirstLaunch() }
    coVerify(exactly = 1) { RestoreCredentials.createIfMissing() }
  }

  @Test
  fun `completes authentication when the restore credential signs in`() {
    coEvery { RestoreCredentials.signInOnFirstLaunch() } returns
      ClerkResult.success(SignIn(id = "sign_in_restored", status = SignIn.Status.COMPLETE))
    var completions = 0

    composeTestRule.setContent {
      AuthView(
        persistIdentifiers = false,
        enableRestoreCredentials = true,
        onAuthComplete = { completions++ },
      )
    }
    composeTestRule.waitUntil { completions == 1 }
    creationDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 1) { RestoreCredentials.signInOnFirstLaunch() }
    coVerify(exactly = 1) { RestoreCredentials.createIfMissing() }
  }

  private fun completedSignUp(): SignUp =
    SignUp(
      id = "sign_up_1",
      status = SignUp.Status.COMPLETE,
      requiredFields = emptyList(),
      optionalFields = emptyList(),
      missingFields = emptyList(),
      unverifiedFields = emptyList(),
      verifications = emptyMap(),
      emailAddress = "new@example.com",
      passwordEnabled = false,
    )

  private fun noSavedCredential(): Throwable =
    Class.forName("com.clerk.api.credentials.CredentialFlowException\$NoSavedCredential")
      .getDeclaredConstructor()
      .newInstance() as Throwable
}
