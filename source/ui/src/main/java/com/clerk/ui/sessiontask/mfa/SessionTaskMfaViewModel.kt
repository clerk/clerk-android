package com.clerk.ui.sessiontask.mfa

import androidx.lifecycle.ViewModel
import com.clerk.ui.userprofile.mfa.ViewType
import com.clerk.ui.userprofile.security.MfaType
import com.clerk.ui.userprofile.verify.VerifyBottomSheetMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class SessionTaskMfaViewModel : ViewModel() {

  private val _flowStep = MutableStateFlow<FlowStep>(FlowStep.ChooseMethod)
  val flowStep: StateFlow<FlowStep> = _flowStep.asStateFlow()

  fun setFlowStep(step: FlowStep) {
    _flowStep.value = step
  }
}

internal sealed interface FlowStep {
  data object ChooseMethod : FlowStep

  data class AddMfa(val viewType: ViewType) : FlowStep

  data object AddPhoneNumber : FlowStep

  data class Verify(val mode: VerifyBottomSheetMode) : FlowStep

  data class BackupCodes(val codes: List<String>, val mfaType: MfaType) : FlowStep
}
