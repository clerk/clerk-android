package com.clerk.ui.userprofile.security

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.clerk.api.Clerk
import com.clerk.api.biometriccredential.BiometricCredentialAvailability
import com.clerk.api.biometriccredential.BiometricCredentials
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.user.User
import com.clerk.api.user.activeSessions
import com.clerk.ui.userprofile.LocalUserProfileState
import com.clerk.ui.userprofile.security.biometriccredential.UserProfileBiometricCredentialViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserProfileSecurityViewTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val hasCredential = AtomicBoolean(false)

  @Before
  fun setUp() {
    mockkObject(Clerk, BiometricCredentials)
    mockkStatic("com.clerk.api.user.UserKt")
    val user = mockk<User>()
    every { Clerk.user } returns user
    every { Clerk.passwordIsEnabled } returns false
    every { Clerk.passkeyIsEnabled } returns false
    every { Clerk.mfaIsEnabled } returns false
    every { Clerk.deleteSelfIsEnabled } returns false
    every { Clerk.biometricSignInIsEnabled } returns true
    coEvery { Clerk.refreshClient() } returns ClerkResult.success(Client())
    coEvery { user.activeSessions() } returns ClerkResult.success(emptyList())
    every { BiometricCredentials.deviceSupportsBiometricAuthentication } returns false
    every { BiometricCredentials.currentUserLocalAvailability() } answers { availability() }
    coEvery { BiometricCredentials.currentUserAvailability() } coAnswers { availability() }
    coEvery { BiometricCredentials.revokeCurrentBiometricCredential() } coAnswers
      {
        hasCredential.set(false)
        ClerkResult.success(Unit)
      }
    composeTestRule.runOnIdle {
      // Keep mocked availability work on the main thread so Compose can wait for it to finish.
      val factory = viewModelFactory {
        initializer {
          UserProfileBiometricCredentialViewModel(workDispatcher = Dispatchers.Main.immediate)
        }
      }
      ViewModelProvider(composeTestRule.activity, factory)
        .get(UserProfileBiometricCredentialViewModel::class.java)
    }
  }

  @After
  fun tearDown() {
    // Dispose the composition and cancel ViewModel work before removing the singleton mocks.
    composeTestRule.activityRule.scenario.close()
    unmockkAll()
  }

  @Test
  fun `existing PIN capable credential can be disabled without strong biometrics`() {
    hasCredential.set(true)
    showSecurityScreen()

    composeTestRule.onNode(isToggleable()).assertIsOn().assertIsEnabled().performClick()

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodes(isToggleable()).fetchSemanticsNodes().isEmpty()
    }
    coVerify(exactly = 1) { BiometricCredentials.revokeCurrentBiometricCredential() }
    coVerify(exactly = 0) { BiometricCredentials.enroll(any(), any(), any(), any(), any()) }
  }

  @Test
  fun `enrollment is hidden without strong biometrics or an existing credential`() {
    showSecurityScreen()

    composeTestRule.onNode(isToggleable()).assertDoesNotExist()
  }

  @Test
  fun `strong biometrics allow enrollment without an existing credential`() {
    every { BiometricCredentials.deviceSupportsBiometricAuthentication } returns true
    showSecurityScreen()

    composeTestRule.onNode(isToggleable()).assertIsOff().assertIsEnabled()
  }

  private fun showSecurityScreen() {
    composeTestRule.setContent {
      CompositionLocalProvider(LocalUserProfileState provides mockk(relaxed = true)) {
        UserProfileSecurityView()
      }
    }
    composeTestRule.onNodeWithText("Security").assertExists()
  }

  private fun availability(): BiometricCredentialAvailability =
    if (hasCredential.get()) BiometricCredentialAvailability.Available
    else
      BiometricCredentialAvailability.Unavailable(
        BiometricCredentialAvailability.UnavailableReason.NO_LOCAL_CREDENTIAL
      )
}
