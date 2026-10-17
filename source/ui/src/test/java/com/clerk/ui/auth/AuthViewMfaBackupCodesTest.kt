package com.clerk.ui.auth

import androidx.activity.ComponentActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.totp.TOTPResource
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.session.SessionTask
import com.clerk.api.user.User
import com.clerk.api.user.attemptTotpVerification
import com.clerk.api.user.createTotp
import com.clerk.api.user.getOrganizationInvitations
import com.clerk.api.user.getOrganizationMemberships
import com.clerk.api.user.getOrganizationSuggestions
import com.clerk.base.OwnJvm
import com.clerk.ui.navigation.clerkNavigationForwardTransition
import com.clerk.ui.navigation.clerkNavigationPopTransition
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Category(OwnJvm::class)
@Config(qualifiers = "w411dp-h891dp")
class AuthViewMfaBackupCodesTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val session = MutableStateFlow<Session?>(session(task = "setup-mfa"))
  private val user = mockk<User>(relaxed = true)
  private val authCompletions = AtomicInteger()

  @Before
  fun setUp() {
    mockkObject(Clerk)
    mockkStatic("com.clerk.api.user.UserKt")
    mockkStatic("com.clerk.ui.navigation.ClerkNavigationTransitionKt")
    every { clerkNavigationForwardTransition() } returns NO_TRANSITION
    every { clerkNavigationPopTransition() } returns NO_TRANSITION
    every { Clerk.sessionFlow } returns session
    every { Clerk.session } answers { session.value }
    every { Clerk.userFlow } returns MutableStateFlow(user)
    every { Clerk.user } returns user
    every { Clerk.mfaPhoneCodeIsEnabled } returns true
    every { Clerk.mfaAuthenticatorAppIsEnabled } returns true
    coEvery { user.createTotp() } returns ClerkResult.success(totp())
    coEvery { user.attemptTotpVerification(any()) } coAnswers
      {
        session.value = session(task = "choose-organization")
        ClerkResult.success(totp(backupCodes = BACKUP_CODES))
      }
    coEvery { user.getOrganizationMemberships(any(), any()) } returns LOAD_FAILURE
    coEvery { user.getOrganizationInvitations(any(), any(), any()) } returns LOAD_FAILURE
    coEvery { user.getOrganizationSuggestions(any(), any(), any()) } returns LOAD_FAILURE
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `closing the MFA backup codes continues to the pending organization task`() {
    composeTestRule.setContent {
      AuthView(
        persistIdentifiers = false,
        preferGoogleOneTap = false,
        onAuthComplete = { authCompletions.incrementAndGet() },
      )
    }
    composeTestRule.waitUntilExactlyOneExists(hasText("Authenticator application"), TIMEOUT_MILLIS)
    composeTestRule.onNodeWithText("Authenticator application").performClick()
    composeTestRule.waitUntilExactlyOneExists(hasText("Continue"), TIMEOUT_MILLIS)
    composeTestRule.onNodeWithText("Continue").performClick()
    composeTestRule.onNode(hasSetTextAction()).performTextInput("424242")

    composeTestRule.waitUntilExactlyOneExists(hasText(BACKUP_CODES.first()), TIMEOUT_MILLIS)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Choose an organization").assertDoesNotExist()

    composeTestRule.onNodeWithContentDescription("Close").performClick()

    composeTestRule.waitUntilExactlyOneExists(hasText("Choose an organization"), TIMEOUT_MILLIS)
    composeTestRule.onNodeWithText(BACKUP_CODES.first()).assertDoesNotExist()
    assertEquals(0, authCompletions.get())
  }

  private companion object {
    const val TIMEOUT_MILLIS = 5_000L

    val BACKUP_CODES = listOf("backup-code-1", "backup-code-2")

    val NO_TRANSITION = EnterTransition.None togetherWith ExitTransition.None

    val LOAD_FAILURE =
      ClerkResult.Failure(ClerkErrorResponse(errors = listOf(Error(longMessage = "offline"))))

    fun session(task: String) =
      Session(
        id = "sess_123",
        status = Session.SessionStatus.PENDING,
        expireAt = 0L,
        lastActiveAt = 0L,
        createdAt = 0L,
        updatedAt = 0L,
        currentTask = SessionTask(task),
      )

    fun totp(backupCodes: List<String>? = null) =
      TOTPResource(
        id = "totp_123",
        secret = "JBSWY3DPEHPK3PXP",
        uri = "otpauth://totp/Example?secret=JBSWY3DPEHPK3PXP",
        verified = true,
        backupCodes = backupCodes,
        createdAt = 0L,
        updatedAt = 0L,
      )
  }
}
