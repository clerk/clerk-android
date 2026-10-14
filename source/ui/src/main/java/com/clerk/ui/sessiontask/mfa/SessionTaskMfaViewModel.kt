package com.clerk.ui.sessiontask.mfa

import androidx.lifecycle.ViewModel
import com.clerk.ui.userprofile.mfa.ViewType
import com.clerk.ui.userprofile.security.MfaType
import com.clerk.ui.userprofile.verify.VerifyBottomSheetMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class SessionTaskMfaViewModel : ViewModel() {

  private val _progress =
    MutableStateFlow(MfaSetupProgress(sessionId = null, FlowStep.ChooseMethod))
  val progress: StateFlow<MfaSetupProgress> = _progress.asStateFlow()

  fun setFlowStep(sessionId: String?, step: FlowStep) {
    _progress.value = MfaSetupProgress(sessionId, step)
  }
}

internal data class MfaSetupProgress(val sessionId: String?, val step: FlowStep) {
  fun stepFor(sessionId: String?): FlowStep =
    if (sessionId == this.sessionId) step else FlowStep.ChooseMethod
}

internal sealed interface FlowStep {
  data object ChooseMethod : FlowStep

  data class AddMfa(val viewType: ViewType) : FlowStep

  data object AddPhoneNumber : FlowStep

  data class Verify(val mode: VerifyBottomSheetMode) : FlowStep

  data class BackupCodes(val codes: List<String>, val mfaType: MfaType) : FlowStep
}
