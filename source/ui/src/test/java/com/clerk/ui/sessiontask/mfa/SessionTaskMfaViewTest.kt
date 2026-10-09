package com.clerk.ui.sessiontask.mfa

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.clerk.api.Clerk
import com.clerk.api.network.model.totp.TOTPResource
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.api.phonenumber.setReservedForSecondFactor
import com.clerk.api.session.Session
import com.clerk.api.session.SessionTask
import com.clerk.api.user.User
import com.clerk.api.user.attemptTotpVerification
import com.clerk.api.user.createTotp
import com.clerk.api.user.phoneNumbersAvailableForMfa
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class SessionTaskMfaViewTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val session = MutableStateFlow<Session?>(session(awaitingMfaSetup = true))
  private val user = mockk<User>()
  private val authCompletions = AtomicInteger()

  @Before
  fun setUp() {
    mockkObject(Clerk)
    mockkStatic("com.clerk.api.user.UserKt")
    mockkStatic("com.clerk.api.phonenumber.PhoneNumberKt")
    every { Clerk.sessionFlow } returns session
    every { Clerk.user } returns user
    every { Clerk.mfaPhoneCodeIsEnabled } returns true
    every { Clerk.mfaAuthenticatorAppIsEnabled } returns true
    coEvery { user.createTotp() } returns ClerkResult.success(totp())
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `backup codes from an authenticator code stay up until the user closes them`() {
    coEvery { user.attemptTotpVerification(any()) } coAnswers
      {
        session.value = session(awaitingMfaSetup = false)
        ClerkResult.success(totp(backupCodes = BACKUP_CODES))
      }
    showTask()

    enterAuthenticatorCode()
    composeTestRule.waitUntilExactlyOneExists(hasText(BACKUP_CODES.first()), TIMEOUT_MILLIS)
    composeTestRule.waitForIdle()

    assertEquals(0, authCompletions.get())

    composeTestRule.onNodeWithContentDescription("Close").performClick()
    composeTestRule.waitForIdle()

    assertEquals(1, authCompletions.get())
    composeTestRule.onNodeWithText("Set up two-step verification").assertDoesNotExist()
  }

  @Test
  fun `closing the backup codes twice completes the task once`() {
    coEvery { user.attemptTotpVerification(any()) } coAnswers
      {
        session.value = session(awaitingMfaSetup = false)
        ClerkResult.success(totp(backupCodes = BACKUP_CODES))
      }
    showTask()

    enterAuthenticatorCode()
    composeTestRule.waitUntilExactlyOneExists(hasText(BACKUP_CODES.first()), TIMEOUT_MILLIS)
    composeTestRule.onNodeWithContentDescription("Close").performClick()
    composeTestRule.onNodeWithContentDescription("Close").performClick()
    composeTestRule.waitForIdle()

    assertEquals(1, authCompletions.get())
  }

  @Test
  fun `the task completes when MFA is set up while the method list is showing`() {
    showTask()
    composeTestRule.waitForIdle()

    assertEquals(0, authCompletions.get())

    session.value = session(awaitingMfaSetup = false)
    composeTestRule.waitForIdle()

    assertEquals(1, authCompletions.get())
  }

  @Test
  fun `system back on the backup codes completes the task`() {
    coEvery { user.attemptTotpVerification(any()) } coAnswers
      {
        session.value = session(awaitingMfaSetup = false)
        ClerkResult.success(totp(backupCodes = BACKUP_CODES))
      }
    showTask()

    enterAuthenticatorCode()
    composeTestRule.waitUntilExactlyOneExists(hasText(BACKUP_CODES.first()), TIMEOUT_MILLIS)
    composeTestRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
    composeTestRule.waitForIdle()

    assertEquals(1, authCompletions.get())
  }

  @Test
  fun `backup codes from reserving a phone number stay up until the user closes them`() {
    every { user.phoneNumbersAvailableForMfa() } returns listOf(PHONE_NUMBER)
    coEvery { PHONE_NUMBER.setReservedForSecondFactor(true) } coAnswers
      {
        session.value = session(awaitingMfaSetup = false)
        ClerkResult.success(
          PHONE_NUMBER.copy(reservedForSecondFactor = true, backupCodes = BACKUP_CODES)
        )
      }
    showTask()

    composeTestRule.onNodeWithText("SMS code").performClick()
    composeTestRule
      .onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
      .performClick()
    composeTestRule.onNodeWithText("Continue").performClick()
    composeTestRule.waitUntilExactlyOneExists(hasText(BACKUP_CODES.first()), TIMEOUT_MILLIS)
    composeTestRule.waitForIdle()

    assertEquals(0, authCompletions.get())

    composeTestRule.onNodeWithContentDescription("Close").performClick()
    composeTestRule.waitForIdle()

    assertEquals(1, authCompletions.get())
    composeTestRule.onNodeWithText("Set up two-step verification").assertDoesNotExist()
  }

  @Test
  fun `an authenticator code that returns no backup codes completes the task`() {
    coEvery { user.attemptTotpVerification(any()) } coAnswers
      {
        session.value = session(awaitingMfaSetup = false)
        ClerkResult.success(totp())
      }
    showTask()

    enterAuthenticatorCode()
    composeTestRule.waitUntil(TIMEOUT_MILLIS) { authCompletions.get() > 0 }
    composeTestRule.waitForIdle()

    assertEquals(1, authCompletions.get())
  }

  private fun showTask() {
    composeTestRule.setContent {
      SessionTaskMfaView(onAuthComplete = { authCompletions.incrementAndGet() })
    }
  }

  private fun enterAuthenticatorCode() {
    composeTestRule.onNodeWithText("Authenticator application").performClick()
    composeTestRule.waitUntilExactlyOneExists(hasText("Continue"), TIMEOUT_MILLIS)
    composeTestRule.onNodeWithText("Continue").performClick()
    composeTestRule.onNode(hasSetTextAction()).performTextInput("424242")
  }

  private companion object {
    const val TIMEOUT_MILLIS = 5_000L

    val BACKUP_CODES = listOf("backup-code-1", "backup-code-2")

    val PHONE_NUMBER =
      PhoneNumber(
        id = "idn_123",
        phoneNumber = "+12015550100",
        verification = Verification(status = Verification.Status.VERIFIED),
      )

    fun session(awaitingMfaSetup: Boolean) =
      Session(
        id = "sess_123",
        status =
          if (awaitingMfaSetup) Session.SessionStatus.PENDING else Session.SessionStatus.ACTIVE,
        expireAt = 0L,
        lastActiveAt = 0L,
        createdAt = 0L,
        updatedAt = 0L,
        currentTask = SessionTask("setup-mfa").takeIf { awaitingMfaSetup },
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
