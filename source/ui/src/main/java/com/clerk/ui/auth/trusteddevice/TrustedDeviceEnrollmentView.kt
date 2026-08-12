package com.clerk.ui.auth.trusteddevice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.ui.R
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.auth.handleSessionTaskCompletion
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.composition.LocalAuthState
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp64
import com.clerk.ui.core.scaffold.ClerkThemedAuthScaffold
import com.clerk.ui.theme.ClerkMaterialTheme

/**
 * Prompt offered after sign-in or sign-up asking the user to enable biometric (trusted-device)
 * sign-in for this device.
 */
@Composable
internal fun TrustedDeviceEnrollmentView(
  onAuthComplete: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: TrustedDeviceEnrollmentViewModel = viewModel(),
) {
  val authState = LocalAuthState.current
  val state by viewModel.state.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val generic = stringResource(R.string.something_went_wrong_please_try_again)

  fun continueAfterEnrollmentPrompt() {
    authState.handleSessionTaskCompletion(Clerk.session, onAuthComplete)
  }

  LaunchedEffect(state) {
    when (val s = state) {
      is TrustedDeviceEnrollmentViewModel.State.Enrolled -> {
        viewModel.resetState()
        continueAfterEnrollmentPrompt()
      }
      is TrustedDeviceEnrollmentViewModel.State.Error -> {
        snackbarHostState.showSnackbar(s.message ?: generic)
        viewModel.resetState()
      }
      else -> Unit
    }
  }

  val applicationName = Clerk.applicationName
  val promptSubtitle =
    applicationName?.let { stringResource(R.string.app_uses_biometrics_to_sign_you_in, it) }
      ?: stringResource(R.string.use_biometrics_to_sign_in)
  val enrollmentPromptTitle = stringResource(R.string.enroll_this_device)

  ClerkThemedAuthScaffold(
    modifier = modifier,
    title = stringResource(R.string.enable_biometric_sign_in),
    subtitle =
      applicationName?.let {
        stringResource(R.string.enable_biometric_sign_in_for_faster_access_to, it)
      } ?: stringResource(R.string.enable_biometric_sign_in_for_faster_access),
    hasBackButton = false,
    showSignedInUserButton = false,
    snackbarHostState = snackbarHostState,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(dp24, alignment = Alignment.CenterVertically),
    ) {
      Icon(
        modifier = Modifier.size(dp64).height(dp64),
        painter = painterResource(R.drawable.ic_fingerprint),
        contentDescription = null,
        tint = ClerkMaterialTheme.colors.primary,
      )

      ClerkButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.allow),
        isLoading = state is TrustedDeviceEnrollmentViewModel.State.Loading,
        onClick = { viewModel.enroll(enrollmentPromptTitle, promptSubtitle) },
      )

      ClerkTextButton(
        text = stringResource(R.string.not_now),
        onClick = { continueAfterEnrollmentPrompt() },
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  PreviewAuthStateProvider { TrustedDeviceEnrollmentView(onAuthComplete = {}) }
}
