package com.clerk.ui.reverification

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.auth.types.Strategy
import com.clerk.api.session.SessionVerification
import com.clerk.ui.R
import com.clerk.ui.auth.VerificationUiState
import com.clerk.ui.clerkTestTagsAsResourceIds
import com.clerk.ui.core.footer.DevelopmentModeWarningBackground
import com.clerk.ui.core.footer.DevelopmentModeWarningBox
import com.clerk.ui.navigation.clerkNavigationForwardTransition
import com.clerk.ui.navigation.clerkNavigationPopTransition
import com.clerk.ui.signin.help.rememberEmailSupport
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkThemeOverrideProvider
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun ReverificationView(
  level: SessionVerification.Level,
  sessionId: String?,
  onVerified: (sessionId: String) -> Unit,
  onClose: () -> Unit,
) {
  val viewModel = viewModel { ReverificationViewModel(level, sessionId) }
  val session by Clerk.sessionFlow.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(session?.id, sessionId) { if (session?.id != sessionId) onClose() }
  LaunchedEffect(viewModel) { viewModel.errors.collect { snackbarHostState.showSnackbar(it) } }
  val phase = viewModel.phase
  LaunchedEffect(phase) { if (phase is ReverificationPhase.Complete) onVerified(phase.sessionId) }
  BackHandler(enabled = viewModel.backStack.size > 1) { viewModel.navigateBack() }

  ClerkThemeOverrideProvider(clerkTheme = null) {
    ClerkMaterialTheme {
      DevelopmentModeWarningBox(
        modifier =
          Modifier.fillMaxSize()
            .clerkTestTagsAsResourceIds()
            .background(ClerkMaterialTheme.colors.background),
        background = DevelopmentModeWarningBackground.White,
      ) {
        val chrome =
          ReverificationChrome(
            snackbarHostState = snackbarHostState,
            canGoBack = viewModel.backStack.size > 1,
            onBack = { if (!viewModel.navigateBack()) onClose() },
            onClose = onClose,
          )
        when (phase) {
          ReverificationPhase.Starting,
          is ReverificationPhase.Complete -> Spinner()
          is ReverificationPhase.Failed -> ReverificationFailedScreen(chrome, phase.message)
          ReverificationPhase.Ready -> ReverificationNavigation(viewModel, chrome)
        }
      }
    }
  }
}

@Composable
private fun Spinner() {
  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(color = ClerkMaterialTheme.colors.primary)
  }
}

@Composable
private fun ReverificationNavigation(
  viewModel: ReverificationViewModel,
  chrome: ReverificationChrome,
) {
  val destination = viewModel.backStack.lastOrNull() ?: return
  var depth by remember { mutableIntStateOf(viewModel.backStack.size) }
  var isPop by remember { mutableStateOf(false) }
  if (viewModel.backStack.size != depth) {
    isPop = viewModel.backStack.size < depth
    depth = viewModel.backStack.size
  }
  AnimatedContent(
    targetState = destination,
    transitionSpec = {
      if (isPop) clerkNavigationPopTransition() else clerkNavigationForwardTransition()
    },
    label = "reverification",
  ) {
    ReverificationDestinationView(it, viewModel, chrome)
  }
}

@Composable
private fun ReverificationDestinationView(
  destination: ReverificationDestination,
  viewModel: ReverificationViewModel,
  chrome: ReverificationChrome,
) {
  when (destination) {
    is ReverificationDestination.FactorEntry -> FactorView(destination, viewModel, chrome)
    is ReverificationDestination.AlternativeMethods ->
      ReverificationAlternativeMethodsScreen(
        chrome = chrome,
        factors =
          viewModel
            .alternativeFactors(destination.currentFactor, destination.stage)
            .toImmutableList(),
        onSelect = {
          viewModel.navigate(ReverificationDestination.FactorEntry(it, destination.stage))
        },
      )
    is ReverificationDestination.NewPassword ->
      ReverificationNewPasswordScreen(
        chrome = chrome,
        reason = destination.reason,
        newPassword = viewModel.newPassword,
        confirmNewPassword = viewModel.confirmNewPassword,
        signOutOfOtherSessions = viewModel.signOutOfOtherSessions,
        error = viewModel.newPasswordError,
        onNewPasswordChange = viewModel::updateNewPassword,
        onConfirmNewPasswordChange = { viewModel.confirmNewPassword = it },
        onSignOutOfOtherSessionsChange = { viewModel.signOutOfOtherSessions = it },
        onContinue = {
          viewModel.navigate(
            ReverificationDestination.FactorEntry(
              destination.resetFactor,
              ReverificationStage.FIRST_FACTOR,
            )
          )
        },
      )
    ReverificationDestination.GetHelp -> {
      val emailSupport = rememberEmailSupport(chrome.snackbarHostState)
      val supportEmail = Clerk.supportEmail ?: stringResource(R.string.support_clerk_com)
      ReverificationGetHelpScreen(chrome, onEmailSupport = { emailSupport(supportEmail) })
    }
  }
}

@Composable
private fun FactorView(
  destination: ReverificationDestination.FactorEntry,
  viewModel: ReverificationViewModel,
  chrome: ReverificationChrome,
) {
  val (factor, stage) = destination
  val onUseAnotherMethod = {
    viewModel.navigate(ReverificationDestination.AlternativeMethods(factor, stage))
  }
    .takeIf { !factor.isResetPasswordFactor && viewModel.alternativeFactors(factor, stage).any() }
  val state = viewModel.screen
  when (factor.strategyType) {
    Strategy.Password -> PasswordFactorView(viewModel, chrome, onUseAnotherMethod)
    Strategy.Passkey -> {
      LaunchedEffect(destination) { viewModel.attemptAutomatically(factor, stage) }
      ReverificationPasskeyScreen(
        chrome = chrome,
        isLoading = state.isLoading,
        onContinue = { viewModel.verifyWithCredential(factor, stage) },
        onUseAnotherMethod = onUseAnotherMethod,
      )
    }
    Strategy.TrustedDevice -> {
      LaunchedEffect(destination) { viewModel.attemptAutomatically(factor, stage) }
      ReverificationBiometricScreen(
        chrome = chrome,
        isLoading = state.isLoading,
        onContinue = { viewModel.verifyWithCredential(factor, stage) },
        onUseAnotherMethod = onUseAnotherMethod,
      )
    }
    Strategy.BackupCode -> {
      var code by rememberSaveable { mutableStateOf("") }
      ReverificationBackupCodeScreen(
        chrome = chrome,
        code = code,
        state = state,
        onCodeChange = {
          code = it
          viewModel.clearFieldError()
        },
        onSubmit = { viewModel.verifyBackupCode(code) },
        onUseAnotherMethod = onUseAnotherMethod,
      )
    }
    else -> CodeFactorView(destination, viewModel, chrome, onUseAnotherMethod)
  }
}

@Composable
private fun PasswordFactorView(
  viewModel: ReverificationViewModel,
  chrome: ReverificationChrome,
  onUseAnotherMethod: (() -> Unit)?,
) {
  var password by rememberSaveable { mutableStateOf("") }
  val resetFactor = viewModel.resetPasswordFactor
  val reason = viewModel.screen.passwordResetReason
  ReverificationPasswordScreen(
    chrome = chrome,
    password = password,
    state = viewModel.screen,
    onPasswordChange = {
      password = it
      viewModel.clearFieldError()
    },
    onSubmit = { viewModel.verifyPassword(password) },
    onResetPassword =
      if (resetFactor != null && reason != null) {
        { viewModel.navigate(ReverificationDestination.NewPassword(resetFactor, reason)) }
      } else {
        null
      },
    onUseAnotherMethod = onUseAnotherMethod,
  )
}

@Composable
private fun CodeFactorView(
  destination: ReverificationDestination.FactorEntry,
  viewModel: ReverificationViewModel,
  chrome: ReverificationChrome,
  onUseAnotherMethod: (() -> Unit)?,
) {
  val (factor, stage) = destination
  val codeState = viewModel.screen.codeState
  LaunchedEffect(destination) { viewModel.sendCodeIfNeeded(factor, stage) }
  ReverificationCodeScreen(
    chrome = chrome,
    factor = factor,
    codeState = codeState,
    onCodeChange = { code ->
      if (codeState == VerificationUiState.IncorrectCode) viewModel.resetCodeState()
      if (code.length == CODE_LENGTH) viewModel.verifyCode(factor, stage, code)
    },
    onResend = { viewModel.sendCode(factor, stage) },
    onUseAnotherMethod = onUseAnotherMethod,
  )
}

private const val CODE_LENGTH = 6
