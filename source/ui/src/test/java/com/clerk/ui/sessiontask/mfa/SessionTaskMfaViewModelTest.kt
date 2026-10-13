package com.clerk.ui.sessiontask.mfa

import com.clerk.ui.userprofile.mfa.ViewType
import com.clerk.ui.userprofile.security.MfaType
import com.clerk.ui.userprofile.verify.VerifyBottomSheetMode
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionTaskMfaViewModelTest {

  @Test
  fun flowStep_startsOnChooseMethod() {
    val viewModel = SessionTaskMfaViewModel()

    assertEquals(FlowStep.ChooseMethod, viewModel.flowStep.value)
  }

  @Test
  fun setFlowStep_updatesFlowStep() {
    val viewModel = SessionTaskMfaViewModel()

    viewModel.setFlowStep(FlowStep.AddMfa(ViewType.AuthenticatorApp))

    assertEquals(FlowStep.AddMfa(ViewType.AuthenticatorApp), viewModel.flowStep.value)
  }

  @Test
  fun backupCodesStep_isKeptUntilFlowStepChanges() {
    val viewModel = SessionTaskMfaViewModel()
    val backupCodes =
      FlowStep.BackupCodes(codes = listOf("code-1", "code-2"), mfaType = MfaType.PhoneCode)

    viewModel.setFlowStep(backupCodes)

    assertEquals(backupCodes, viewModel.flowStep.value)

    viewModel.setFlowStep(FlowStep.ChooseMethod)

    assertEquals(FlowStep.ChooseMethod, viewModel.flowStep.value)
  }

  @Test
  fun startSession_keepsFlowStepForTheSameSession() {
    val viewModel = SessionTaskMfaViewModel()
    viewModel.startSession("sess_123")
    viewModel.setFlowStep(FlowStep.AddPhoneNumber)

    viewModel.startSession("sess_123")

    assertEquals(FlowStep.AddPhoneNumber, viewModel.flowStep.value)
  }

  @Test
  fun startSession_resetsFlowStepForAnotherSession() {
    val viewModel = SessionTaskMfaViewModel()
    viewModel.startSession("sess_123")
    viewModel.setFlowStep(
      FlowStep.BackupCodes(codes = listOf("code-1"), mfaType = MfaType.PhoneCode)
    )

    viewModel.startSession("sess_456")

    assertEquals(FlowStep.ChooseMethod, viewModel.flowStep.value)
  }

  @Test
  fun endsDoneTask_keepsBackupCodesUp() {
    val backupCodes = FlowStep.BackupCodes(codes = listOf("code-1"), mfaType = MfaType.PhoneCode)

    assertEquals(false, backupCodes.endsDoneTask(enrollmentInFlight = false))
  }

  @Test
  fun endsDoneTask_waitsForAnEnrollmentInFlight() {
    val verify = FlowStep.Verify(VerifyBottomSheetMode.Totp)

    assertEquals(false, verify.endsDoneTask(enrollmentInFlight = true))
  }

  @Test
  fun endsDoneTask_leavesAnIdleSetupStep() {
    assertEquals(true, FlowStep.AddMfa(ViewType.Sms).endsDoneTask(enrollmentInFlight = false))
    assertEquals(true, FlowStep.AddPhoneNumber.endsDoneTask(enrollmentInFlight = false))
    assertEquals(true, FlowStep.ChooseMethod.endsDoneTask(enrollmentInFlight = false))
  }
}
