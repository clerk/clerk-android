package com.clerk.ui.sessiontask.mfa

import com.clerk.ui.userprofile.mfa.ViewType
import com.clerk.ui.userprofile.security.MfaType
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionTaskMfaViewModelTest {

  @Test
  fun progress_startsOnChooseMethod() {
    val viewModel = SessionTaskMfaViewModel()

    assertEquals(FlowStep.ChooseMethod, viewModel.progress.value.stepFor("sess_123"))
  }

  @Test
  fun setFlowStep_updatesTheStepForItsSession() {
    val viewModel = SessionTaskMfaViewModel()

    viewModel.setFlowStep("sess_123", FlowStep.AddMfa(ViewType.AuthenticatorApp))

    assertEquals(
      FlowStep.AddMfa(ViewType.AuthenticatorApp),
      viewModel.progress.value.stepFor("sess_123"),
    )
  }

  @Test
  fun backupCodesStep_isKeptUntilFlowStepChanges() {
    val viewModel = SessionTaskMfaViewModel()
    val backupCodes =
      FlowStep.BackupCodes(codes = listOf("code-1", "code-2"), mfaType = MfaType.PhoneCode)

    viewModel.setFlowStep("sess_123", backupCodes)

    assertEquals(backupCodes, viewModel.progress.value.stepFor("sess_123"))

    viewModel.setFlowStep("sess_123", FlowStep.ChooseMethod)

    assertEquals(FlowStep.ChooseMethod, viewModel.progress.value.stepFor("sess_123"))
  }

  @Test
  fun stepFor_isChooseMethodForAnotherSession() {
    val viewModel = SessionTaskMfaViewModel()
    viewModel.setFlowStep(
      "sess_123",
      FlowStep.BackupCodes(codes = listOf("code-1"), mfaType = MfaType.PhoneCode),
    )

    assertEquals(FlowStep.ChooseMethod, viewModel.progress.value.stepFor("sess_456"))
  }

  @Test
  fun stepFor_isChooseMethodAfterSignOut() {
    val viewModel = SessionTaskMfaViewModel()
    viewModel.setFlowStep(
      "sess_123",
      FlowStep.BackupCodes(codes = listOf("code-1"), mfaType = MfaType.PhoneCode),
    )

    assertEquals(FlowStep.ChooseMethod, viewModel.progress.value.stepFor(null))
  }
}
