package com.clerk.ui.sessiontask.mfa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.api.session.requiresForcedMfa
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfiguration
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.scaffold.ClerkThemedAuthScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.signin.help.SignInGetHelpView
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.mfa.AddMfaCallbacks
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

  ClerkMaterialTheme {
    val contentModifier =
      Modifier.fillMaxSize().background(ClerkMaterialTheme.colors.background).then(modifier)
    SessionTaskMfaFlowContent(
      modifier = contentModifier,
      flowStep = flowStep,
      onFlowStepChange = { flowStep = it },
    )
  }
}

@Composable
private fun SessionTaskMfaFlowContent(
  flowStep: FlowStep,
  modifier: Modifier = Modifier,
  onFlowStepChange: (FlowStep) -> Unit,
) {
  when (flowStep) {
    FlowStep.ChooseMethod ->
      ChooseMethodStep(modifier = modifier, onFlowStepChange = onFlowStepChange)
    is FlowStep.AddMfa ->
      SessionTaskMfaStepContainer(modifier = modifier) {
        AddMfaStep(viewType = flowStep.viewType, onFlowStepChange = onFlowStepChange)
      }
    FlowStep.AddPhoneNumber ->
      SessionTaskMfaStepContainer(modifier = modifier) {
        AddPhoneNumberStep(onFlowStepChange = onFlowStepChange)
      }
    is FlowStep.Verify ->
      SessionTaskMfaStepContainer(modifier = modifier) {
        VerifyStep(mode = flowStep.mode, onFlowStepChange = onFlowStepChange)
      }
    is FlowStep.BackupCodes ->
      SessionTaskMfaStepContainer(modifier = modifier) {
        BackupCodesStep(step = flowStep, onFlowStepChange = onFlowStepChange)
      }
  }
}

@Composable
private fun SessionTaskMfaStepContainer(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Box(modifier = modifier.fillMaxSize().statusBarsPadding().padding(top = dp8)) { content() }
}

@Composable
private fun ChooseMethodStep(modifier: Modifier = Modifier, onFlowStepChange: (FlowStep) -> Unit) {
  ClerkThemedAuthScaffold(
    modifier = modifier,
    title = stringResource(R.string.set_up_two_step_verification),
    subtitle =
      stringResource(
        R.string
          .choose_which_method_you_prefer_to_protect_your_account_with_an_extra_layer_of_security
      ),
    hasLogo = false,
  ) {
    SessionTaskMfaMethodButtons(
      mfaPhoneCodeIsEnabled = Clerk.mfaPhoneCodeIsEnabled,
      mfaAuthenticatorAppIsEnabled = Clerk.mfaAuthenticatorAppIsEnabled,
      onClick = { onFlowStepChange(FlowStep.AddMfa(it)) },
    )
  }
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
private fun SessionTaskMfaMethodButtons(
  mfaPhoneCodeIsEnabled: Boolean,
  mfaAuthenticatorAppIsEnabled: Boolean,
  onClick: (ViewType) -> Unit,
) {
  if (mfaPhoneCodeIsEnabled) {
    SessionTaskMfaMethodButton(
      text = stringResource(R.string.sms_code),
      iconRes = R.drawable.ic_phone,
      onClick = { onClick(ViewType.Sms) },
    )
  }
  if (mfaAuthenticatorAppIsEnabled) {
    Spacers.Vertical.Spacer12()
    SessionTaskMfaMethodButton(
      text = stringResource(R.string.authenticator_application),
      iconRes = R.drawable.ic_key,
      onClick = { onClick(ViewType.AuthenticatorApp) },
    )
  }
}

@Composable
private fun SessionTaskMfaMethodButton(text: String, iconRes: Int, onClick: () -> Unit) {
  ClerkButton(
    modifier = Modifier.fillMaxWidth(),
    text = text,
    onClick = onClick,
    configuration =
      ClerkButtonDefaults.configuration(
        style = ClerkButtonConfiguration.ButtonStyle.Secondary,
        emphasis = ClerkButtonConfiguration.Emphasis.High,
      ),
    icons =
      ClerkButtonDefaults.icons(
        leadingIcon = iconRes,
        leadingIconColor = ClerkMaterialTheme.colors.mutedForeground,
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
      val mfaType =
        when (mode) {
          is VerifyBottomSheetMode.Email,
          is VerifyBottomSheetMode.Phone -> MfaType.PhoneCode
          VerifyBottomSheetMode.Totp -> MfaType.AuthenticatorApp
        }
      val nextStep =
        if (codes.isNullOrEmpty()) {
          FlowStep.ChooseMethod
        } else {
          FlowStep.BackupCodes(codes = codes, mfaType = mfaType)
        }
      onFlowStepChange(nextStep)
    },
  )
}

@Composable
private fun BackupCodesStep(step: FlowStep.BackupCodes, onFlowStepChange: (FlowStep) -> Unit) {
  BackupCodesView(
    codes = step.codes.toImmutableList(),
    mfaType = step.mfaType,
    onDismiss = { onFlowStepChange(FlowStep.ChooseMethod) },
  )
}

private sealed interface FlowStep {
  data object ChooseMethod : FlowStep

  data class AddMfa(val viewType: ViewType) : FlowStep

  data object AddPhoneNumber : FlowStep

  data class Verify(val mode: VerifyBottomSheetMode) : FlowStep

  data class BackupCodes(val codes: List<String>, val mfaType: MfaType) : FlowStep
}
