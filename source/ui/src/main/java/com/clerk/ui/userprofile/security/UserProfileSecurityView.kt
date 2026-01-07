package com.clerk.ui.userprofile.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.session.Session
import com.clerk.ui.R
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp10
import com.clerk.ui.core.error.ClerkErrorSnackbar
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.LocalUserProfileState
import com.clerk.ui.userprofile.security.delete.UserProfileDeleteAccountSection
import com.clerk.ui.userprofile.security.device.UserProfileDevicesSection
import com.clerk.ui.userprofile.security.mfa.UserProfileMfaSection
import com.clerk.ui.userprofile.security.passkey.UserProfilePasskeySection
import com.clerk.ui.userprofile.security.password.PasswordAction
import com.clerk.ui.userprofile.security.password.UserProfilePasswordSection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@Composable
internal fun UserProfileSecurityView() {
  UserProfileSecurityViewImpl(
    isPasswordEnabled = Clerk.passwordIsEnabled,
    isPasskeyEnabled = Clerk.passkeyIsEnabled,
    isMfaEnabled = Clerk.mfaIsEnabled,
    isDeleteSelfEnabled = Clerk.deleteSelfIsEnabled,
  )
}

@Composable
private fun UserProfileSecurityViewImpl(
  viewModel: UserProfileSecurityViewModel = viewModel(),
  isPasswordEnabled: Boolean = false,
  isPasskeyEnabled: Boolean = false,
  isMfaEnabled: Boolean = false,
  isDeleteSelfEnabled: Boolean = false,
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val errorMessage = (state as? UserProfileSecurityViewModel.State.Error)?.message
  LaunchedEffect(errorMessage) {
    if (errorMessage != null) {
      snackbarHostState.showSnackbar(errorMessage)
    }
  }
  LaunchedEffect(Unit) { viewModel.loadSessions() }
  ClerkMaterialTheme {
    when (state) {
      is UserProfileSecurityViewModel.State.Loading,
      UserProfileSecurityViewModel.State.Idle -> {
        Box(modifier = Modifier.fillMaxSize().background(ClerkMaterialTheme.colors.muted)) {
          CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
      }
      else -> {
        UserProfileSecurityMainContent(
          isPasswordEnabled = isPasswordEnabled,
          isPasskeyEnabled = isPasskeyEnabled,
          isMfaEnabled = isMfaEnabled,
          isDeleteSelfEnabled = isDeleteSelfEnabled,
          snackbarHostState = snackbarHostState,
          sessions =
            (state as? UserProfileSecurityViewModel.State.Success)
              ?.sessions
              .orEmpty()
              .toImmutableList(),
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileSecurityMainContent(
  isPasswordEnabled: Boolean,
  isPasskeyEnabled: Boolean,
  isMfaEnabled: Boolean,
  isDeleteSelfEnabled: Boolean,
  snackbarHostState: SnackbarHostState,
  sessions: ImmutableList<Session>,
) {
  val userProfileState = LocalUserProfileState.current
  val coroutineScope = rememberCoroutineScope()
  var showBottomSheet by remember { mutableStateOf(false) }
  var currentSheetType by remember {
    mutableStateOf<BottomSheetType>(BottomSheetType.DeleteAccount)
  }
  val context = LocalContext.current
  Scaffold(
    containerColor = ClerkMaterialTheme.colors.muted,
    snackbarHost = { ClerkErrorSnackbar(snackbarHostState) },
    topBar = {
      ClerkTopAppBar(
        hasLogo = false,
        hasBackButton = true,
        title = stringResource(R.string.security),
        backgroundColor = ClerkMaterialTheme.colors.muted,
        onBackPressed = { userProfileState.navigateBack() },
      )
    },
  ) { innerPadding ->
    UserProfileSecurityContent(
      innerPadding = innerPadding,
      configuration =
        SecurityContentConfiguration(
          isPasswordEnabled = isPasswordEnabled,
          isPasskeyEnabled = isPasskeyEnabled,
          isMfaEnabled = isMfaEnabled,
          sessions = sessions,
          isDeleteSelfEnabled = isDeleteSelfEnabled,
        ),
      onAdd = {
        showBottomSheet = true
        currentSheetType = BottomSheetType.ChooseMfa
      },
      onClickDeleteAccount = {
        showBottomSheet = true
        currentSheetType = BottomSheetType.DeleteAccount
      },
      onClickAddPassword = {
        showBottomSheet = true
        currentSheetType =
          when (it) {
            PasswordAction.Add ->
              BottomSheetType.NewPassword(currentPassword = null, PasswordAction.Add)

            PasswordAction.Reset -> {
              BottomSheetType.CurrentPassword(PasswordAction.Reset)
            }
          }
      },
      onError = { message ->
        coroutineScope.launch {
          snackbarHostState.showSnackbar(
            message ?: context.getString(R.string.something_went_wrong_please_try_again)
          )
        }
      },
    )
    BottomSheetContent(
      showBottomSheet = showBottomSheet,
      currentSheetType = currentSheetType,
      callbacks =
        BottomSheetCallbacks(
          onDismiss = { showBottomSheet = false },
          onClickMfaType = {
            showBottomSheet = false
            currentSheetType = BottomSheetType.AddMfa(it)
            showBottomSheet = true
          },
          onCurrentPasswordEntered = { password, action ->
            showBottomSheet = true
            currentSheetType =
              BottomSheetType.NewPassword(currentPassword = password, passwordAction = action)
          },
          onAddPhoneNumber = {
            showBottomSheet = false
            currentSheetType = BottomSheetType.AddPhoneNumber
            showBottomSheet = true
          },
          onNavigateToBackupCodes = {
            showBottomSheet = false
            currentSheetType = BottomSheetType.BackupCodes(it)
            showBottomSheet = true
          },
          onVerify = {
            showBottomSheet = false
            currentSheetType = BottomSheetType.Verify(it)
            showBottomSheet = true
          },
          onError = { message ->
            coroutineScope.launch {
              snackbarHostState.showSnackbar(
                message ?: context.getString(R.string.something_went_wrong_please_try_again)
              )
            }
          },
        ),
    )
  }
}

@Composable
private fun UserProfileSecurityContent(
  innerPadding: PaddingValues,
  configuration: SecurityContentConfiguration,
  onError: (String?) -> Unit,
  onClickDeleteAccount: () -> Unit,
  onClickAddPassword: (PasswordAction) -> Unit,
  onAdd: () -> Unit,
) {
  val scrollState = rememberScrollState()
  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(ClerkMaterialTheme.colors.muted)
        .padding(innerPadding)
        .verticalScroll(scrollState),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (configuration.isPasswordEnabled) {
      UserProfilePasswordSection(onClick = onClickAddPassword)
      HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
    }
    if (configuration.isPasskeyEnabled) {
      UserProfilePasskeySection(onError = onError)
      HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
    }
    if (configuration.isMfaEnabled) {
      UserProfileMfaSection(onAdd = onAdd)
      HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
    }
    if ((configuration.sessions.mapNotNull { it.latestActivity }.isNotEmpty())) {
      UserProfileDevicesSection(devices = configuration.sessions)
      HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
    }
    if (configuration.isDeleteSelfEnabled) {
      UserProfileDeleteAccountSection(onDeleteAccount = onClickDeleteAccount)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetContent(
  showBottomSheet: Boolean,
  currentSheetType: BottomSheetType,
  callbacks: BottomSheetCallbacks,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()
  if (showBottomSheet) {
    ModalBottomSheet(
      scrimColor = ClerkMaterialTheme.colors.neutral.copy(alpha = 0.5f),
      shape = RoundedCornerShape(topEnd = dp10, topStart = dp10),
      containerColor = ClerkMaterialTheme.colors.background,
      sheetState = sheetState,
      onDismissRequest = callbacks.onDismiss,
    ) {
      val programmaticDismissCallbacks =
        callbacks.copy(
          onDismiss = {
            scope.launch {
              sheetState.hide()
            }.invokeOnCompletion {
              callbacks.onDismiss()
            }
          }
        )

      BottomSheetBody(currentSheetType = currentSheetType, callbacks = programmaticDismissCallbacks)
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    UserProfileSecurityViewImpl(
      isPasskeyEnabled = true,
      isPasswordEnabled = true,
      isMfaEnabled = true,
      isDeleteSelfEnabled = true,
    )
  }
}

internal data class SecurityContentConfiguration(
  val isPasswordEnabled: Boolean = true,
  val isPasskeyEnabled: Boolean = true,
  val isMfaEnabled: Boolean = true,
  val isDeleteSelfEnabled: Boolean = true,
  val sessions: ImmutableList<Session> = persistentListOf(),
)
