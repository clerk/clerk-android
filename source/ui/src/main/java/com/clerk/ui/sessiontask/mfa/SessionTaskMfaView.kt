package com.clerk.ui.sessiontask.mfa

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.api.session.requiresForcedMfa
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.signin.help.SignInGetHelpView
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.mfa.AddMfaCallbacks
import com.clerk.ui.userprofile.mfa.UserProfileAddMfaBottomSheetContent
import com.clerk.ui.userprofile.mfa.UserProfileAddMfaView
import com.clerk.ui.userprofile.mfa.ViewType
import com.clerk.ui.userprofile.phone.UserProfileAddPhoneView
import com.clerk.ui.userprofile.security.BackupCodesView
import com.clerk.ui.userprofile.security.MfaType
import com.clerk.ui.userprofile.security.toVerifyMode
import com.clerk.ui.userprofile.verify.UserProfileVerifyBottomSheetContent
import com.clerk.ui.userprofile.verify.VerifyBottomSheetMode
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun SessionTaskMfaView(modifier: Modifier = Modifier, onAuthComplete: () -> Unit) {
  val session by Clerk.sessionFlow.collectAsStateWithLifecycle()
  var flowStep by remember { mutableStateOf<FlowStep>(FlowStep.ChooseMethod) }

  LaunchedEffect(session?.requiresForcedMfa) {
    if (session?.requiresForcedMfa == false) {
      onAuthComplete()
    }
  }

  if (!Clerk.mfaPhoneCodeIsEnabled && !Clerk.mfaAuthenticatorAppIsEnabled) {
    SignInGetHelpView(modifier = modifier)
    return
  }

  SessionTaskMfaFlowContent(
    modifier = modifier,
    flowStep = flowStep,
    onFlowStepChange = { flowStep = it },
  )
}

@Composable
private fun SessionTaskMfaFlowContent(
  modifier: Modifier,
  flowStep: FlowStep,
  onFlowStepChange: (FlowStep) -> Unit,
) {
  when (val step = flowStep) {
    FlowStep.ChooseMethod ->
      ChooseMethodStep(modifier = modifier, onFlowStepChange = onFlowStepChange)
    is FlowStep.AddMfa -> AddMfaStep(viewType = step.viewType, onFlowStepChange = onFlowStepChange)
    FlowStep.AddPhoneNumber -> AddPhoneNumberStep(onFlowStepChange = onFlowStepChange)
    is FlowStep.Verify -> VerifyStep(mode = step.mode, onFlowStepChange = onFlowStepChange)
    is FlowStep.BackupCodes ->
      BackupCodesStep(modifier = modifier, step = step, onFlowStepChange = onFlowStepChange)
  }
}

@Composable
private fun ChooseMethodStep(modifier: Modifier, onFlowStepChange: (FlowStep) -> Unit) {
  UserProfileAddMfaBottomSheetContent(
    modifier = modifier,
    mfaPhoneCodeIsEnabled = Clerk.mfaPhoneCodeIsEnabled,
    mfaAuthenticatorAppIsEnabled = Clerk.mfaAuthenticatorAppIsEnabled,
    onClick = { onFlowStepChange(FlowStep.AddMfa(it)) },
  )
}

@Composable
private fun AddMfaStep(viewType: ViewType, onFlowStepChange: (FlowStep) -> Unit) {
  UserProfileAddMfaView(
    viewType = viewType,
    callbacks =
      AddMfaCallbacks(
        onDismiss = { onFlowStepChange(FlowStep.ChooseMethod) },
        onNavigateToBackupCodes = {
          onFlowStepChange(FlowStep.BackupCodes(codes = it, mfaType = MfaType.PhoneCode))
        },
        onError = { onFlowStepChange(FlowStep.ChooseMethod) },
        onAddPhoneNumber = { onFlowStepChange(FlowStep.AddPhoneNumber) },
        onVerify = { onFlowStepChange(FlowStep.Verify(it.toVerifyMode())) },
      ),
  )
}

@Composable
private fun AddPhoneNumberStep(onFlowStepChange: (FlowStep) -> Unit) {
  UserProfileAddPhoneView(
    onDismiss = { onFlowStepChange(FlowStep.ChooseMethod) },
    onVerify = { onFlowStepChange(FlowStep.Verify(it.toVerifyMode())) },
  )
}

@Composable
private fun VerifyStep(mode: VerifyBottomSheetMode, onFlowStepChange: (FlowStep) -> Unit) {
  UserProfileVerifyBottomSheetContent(
    mode = mode,
    onDismiss = { onFlowStepChange(FlowStep.ChooseMethod) },
    onVerified = { codes ->
      val nextStep =
        if (codes.isNullOrEmpty()) {
          FlowStep.ChooseMethod
        } else {
          FlowStep.BackupCodes(codes = codes, mfaType = mode.toMfaType())
        }
      onFlowStepChange(nextStep)
    },
  )
}

@Composable
private fun BackupCodesStep(
  modifier: Modifier,
  step: FlowStep.BackupCodes,
  onFlowStepChange: (FlowStep) -> Unit,
) {
  BackupCodesView(
    modifier = modifier,
    codes = step.codes.toImmutableList(),
    mfaType = step.mfaType,
    onDismiss = { onFlowStepChange(FlowStep.ChooseMethod) },
  )
}

private fun VerifyBottomSheetMode.toMfaType(): MfaType {
  return when (this) {
    is VerifyBottomSheetMode.Email,
    is VerifyBottomSheetMode.Phone -> MfaType.PhoneCode
    VerifyBottomSheetMode.Totp -> MfaType.AuthenticatorApp
  }
}

private sealed interface FlowStep {
  data object ChooseMethod : FlowStep

  data class AddMfa(val viewType: ViewType) : FlowStep

  data object AddPhoneNumber : FlowStep

  data class Verify(val mode: VerifyBottomSheetMode) : FlowStep

  data class BackupCodes(val codes: List<String>, val mfaType: MfaType) : FlowStep
}

@PreviewLightDark
@Composable
private fun PreviewSessionTaskMfaView() {
  PreviewAuthStateProvider { ClerkMaterialTheme { SessionTaskMfaView(onAuthComplete = {}) } }
}
