package com.clerk.ui.userprofile.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.ui.R
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.footer.SecuredByClerkView
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.security.delete.UserProfileDeleteAccountSection
import com.clerk.ui.userprofile.security.device.UserProfileDevicesSection
import com.clerk.ui.userprofile.security.mfa.UserProfileMfaSection
import com.clerk.ui.userprofile.security.passkey.UserProfilePasskeySection
import com.clerk.ui.userprofile.security.password.UserProfilePasswordSection
import kotlinx.collections.immutable.toImmutableList

@Composable
fun UserProfileSecurityView(
  modifier: Modifier = Modifier,
  viewModel: UserProfileSecurityViewModel = viewModel(),
) {
  UserProfileSecurityViewImpl(
    modifier = modifier,
    isPasswordEnabled = Clerk.passwordIsEnabled,
    isPasskeyEnabled = Clerk.passkeyIsEnabled,
    isMfaEnabled = Clerk.mfaIsEnabled,
    isDeleteSelfEnabled = Clerk.deleteSelfIsEnabled,
  )
}

@Composable
private fun UserProfileSecurityViewImpl(
  modifier: Modifier = Modifier,
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
    Scaffold(
      modifier = Modifier.then(modifier),
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
      when (state) {
        is UserProfileSecurityViewModel.State.Loading,
        UserProfileSecurityViewModel.State.Idle -> {
          Box(
            modifier =
              Modifier.fillMaxSize()
                .background(ClerkMaterialTheme.colors.muted)
                .padding(innerPadding)
          ) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
          }
        }
        else -> {
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .fillMaxSize()
                .background(ClerkMaterialTheme.colors.muted)
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            ClerkTopAppBar(
              hasLogo = false,
              hasBackButton = true,
              title = stringResource(R.string.security),
              backgroundColor = ClerkMaterialTheme.colors.muted,
              onBackPressed = {},
            )
            UserProfileSecurityContent(
              isPasswordEnabled,
              isPasskeyEnabled,
              isMfaEnabled,
              state,
              isDeleteSelfEnabled,
            )
            UserProfileSecurityFooter()
          }
        }
      }
    }
  }
}

@Composable
private fun UserProfileSecurityContent(
  isPasswordEnabled: Boolean,
  isPasskeyEnabled: Boolean,
  isMfaEnabled: Boolean,
  state: UserProfileSecurityViewModel.State,
  isDeleteSelfEnabled: Boolean,
) {
  if (isPasswordEnabled) {
    UserProfilePasswordSection(onAction = {})
    HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
  }
  if (isPasskeyEnabled) {
    UserProfilePasskeySection() {}
    HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
  }
  if (isMfaEnabled) {
    UserProfileMfaSection(onRemove = {}, onAdd = {})
    HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
  }
  if (
    (state as? UserProfileSecurityViewModel.State.Success)
      ?.sessions
      ?.mapNotNull { it.latestActivity }
      ?.isNotEmpty() == true
  ) {
    UserProfileDevicesSection(
      devices = (state as UserProfileSecurityViewModel.State.Success).sessions.toImmutableList()
    )
    HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
  }
  if (isDeleteSelfEnabled) {
    UserProfileDeleteAccountSection(onDeleteAccount = {})
  }
}

@Composable
private fun ColumnScope.UserProfileSecurityFooter() {
  Spacers.Vertical.Spacer16()
  Spacer(modifier = Modifier.weight(1f))
  Spacers.Vertical.Spacer24()
  SecuredByClerkView()
  Spacers.Vertical.Spacer24()
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
