package com.clerk.ui.sessiontask.mfa

import com.clerk.ui.userprofile.mfa.ViewType
import com.clerk.ui.userprofile.security.MfaType
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
}
