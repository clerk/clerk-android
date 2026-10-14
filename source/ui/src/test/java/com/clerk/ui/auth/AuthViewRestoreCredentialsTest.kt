package com.clerk.ui.auth

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.restorecredentials.RestoreCredentials
import com.clerk.api.signin.SignIn
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
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
    coVerify(exactly = 0) { RestoreCredentials.signInWithoutReportingFailures() }
    coVerify(exactly = 0) { RestoreCredentials.signIn() }
  }

  @Test
  fun `falls through to the start screen when there is no restore credential`() {
    coEvery { RestoreCredentials.signInWithoutReportingFailures() } returns
      ClerkResult.unknownFailure(noSavedCredential())
    var completions = 0

    composeTestRule.setContent {
      AuthView(
        persistIdentifiers = false,
        enableRestoreCredentials = true,
        onAuthComplete = { completions++ },
      )
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Continue").assertExists()
    composeTestRule.onNodeWithText("Enter your email").assertExists()
    coVerify(exactly = 1) { RestoreCredentials.signInWithoutReportingFailures() }
    assertEquals(0, completions)
  }

  @Test
  fun `completes authentication when the restore credential signs in`() {
    coEvery { RestoreCredentials.signInWithoutReportingFailures() } returns
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

    coVerify(exactly = 1) { RestoreCredentials.signInWithoutReportingFailures() }
    coVerify(exactly = 1) { RestoreCredentials.createIfMissing() }
  }

  private fun noSavedCredential(): Throwable =
    Class.forName("com.clerk.api.credentials.CredentialFlowException\$NoSavedCredential")
      .getDeclaredConstructor()
      .newInstance() as Throwable
}
