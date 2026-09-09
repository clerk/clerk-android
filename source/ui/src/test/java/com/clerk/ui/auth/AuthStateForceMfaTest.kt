package com.clerk.ui.auth

import com.clerk.api.*
import com.clerk.testing.*
import io.mockk.*
import org.junit.Assert.*
import org.junit.Test

class AuthStateForceMfaTest {
  @Test
  fun pendingMfaTaskComesFromCoreSession() {
    assertEquals(
      SessionTaskKey.SetupMfa,
      mockSession(task = SessionTaskKey.SetupMfa).pendingSessionTaskKey(),
    )
  }

  @Test
  fun pendingResetPasswordTaskComesFromCoreSession() {
    assertEquals(
      SessionTaskKey.ResetPassword,
      mockSession(task = SessionTaskKey.ResetPassword).pendingSessionTaskKey(),
    )
  }

  @Test
  fun missingSessionHasNoInventedTask() {
    assertNull((null as Session?).pendingSessionTaskKey())
  }

  @Test
  fun pendingChooseOrganizationTaskComesFromCoreSession() {
    assertEquals(
      SessionTaskKey.ChooseOrganization,
      mockSession(task = SessionTaskKey.ChooseOrganization).pendingSessionTaskKey(),
    )
  }

  @Test
  fun activeSessionStillHonorsCurrentTask() {
    assertEquals(
      SessionTaskKey.SetupMfa,
      mockSession(status = SessionStatus.Active, task = SessionTaskKey.SetupMfa)
        .pendingSessionTaskKey(),
    )
  }

  @Test
  fun completedSignUpResolvesItsOwnSession() {
    val session = mockSession(status = SessionStatus.Pending, task = SessionTaskKey.SetupMfa)
    val clerk = mockClerk()
    every { clerk.sessions } returns listOf(session)
    val signUp = mockk<SignUp> { every { createdSessionId } returns "sess_123" }
    assertEquals(
      SessionTaskKey.SetupMfa,
      signUp.correspondingSession(clerk).pendingSessionTaskKey(),
    )
  }

  @Test
  fun unresolvedCreatedSessionRoutesToHelp() {
    assertEquals(PostAuthCompletionAction.ROUTE_TO_HELP, postAuthCompletionAction(null, true))
  }

  @Test
  fun resetPasswordRoutesToResetPassword() {
    assertEquals(
      PostAuthCompletionAction.ROUTE_TO_RESET_PASSWORD,
      postAuthCompletionAction(SessionTaskKey.ResetPassword, false),
    )
  }

  @Test
  fun chooseOrganizationRoutesToOrganizationSelection() {
    assertEquals(
      PostAuthCompletionAction.ROUTE_TO_CHOOSE_ORGANIZATION,
      postAuthCompletionAction(SessionTaskKey.ChooseOrganization, false),
    )
  }

  @Test
  fun unknownTaskDoesNotCompleteAuthentication() {
    assertEquals(
      PostAuthCompletionAction.ROUTE_TO_HELP,
      postAuthCompletionAction(SessionTaskKey.Unrecognized("new_task"), false),
    )
  }

  @Test
  fun pendingTaskTakesPriorityOverUnresolvedSession() {
    assertEquals(
      PostAuthCompletionAction.ROUTE_TO_RESET_PASSWORD,
      postAuthCompletionAction(SessionTaskKey.ResetPassword, true),
    )
  }

  @Test
  fun noTaskAndResolvedSessionAllowsCompletion() {
    assertEquals(PostAuthCompletionAction.COMPLETE_AUTH, postAuthCompletionAction(null, false))
  }

  @Test
  fun missingCreatedSessionNeverFallsBackToAnotherSession() {
    val fallback = mockSession(id = "sess_fallback")
    assertNull(
      resolveCorrespondingSession("sess_target", listOf(mockSession(id = "sess_other")), fallback)
    )
  }
}
