package com.clerk.ui.userprofile.security.trusteddevice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.theme.ClerkMaterialTheme

/** Security-screen section with a toggle for biometric (trusted-device) sign-in. */
@Composable
internal fun UserProfileTrustedDeviceSection(
  onError: (String?) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: UserProfileTrustedDeviceViewModel = viewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  LaunchedEffect(Unit) { viewModel.refreshAvailability() }

  LaunchedEffect(state.error) {
    state.error?.let {
      onError(it)
      viewModel.clearError()
    }
  }

  val promptTitle = stringResource(R.string.sign_in_with_biometrics)
  val promptSubtitle =
    Clerk.applicationName?.let { stringResource(R.string.app_uses_biometrics_to_sign_you_in, it) }
      ?: stringResource(R.string.use_biometrics_to_sign_in)

  UserProfileTrustedDeviceSectionImpl(
    modifier = modifier,
    isEnabled = state.isEnabled,
    isLoading = state.isLoading,
    onCheckedChange = { enabled ->
      viewModel.setTrustedDeviceSignInEnabled(
        enabled = enabled,
        promptTitle = promptTitle,
        promptSubtitle = promptSubtitle,
      )
    },
  )
}

@Composable
internal fun UserProfileTrustedDeviceSectionImpl(
  isEnabled: Boolean,
  isLoading: Boolean,
  modifier: Modifier = Modifier,
  onCheckedChange: (Boolean) -> Unit,
) {
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.background)
          .padding(top = dp16)
          .then(modifier)
    ) {
      Text(
        modifier = Modifier.padding(horizontal = dp24),
        text = stringResource(R.string.biometric_sign_in).uppercase(),
        color = ClerkMaterialTheme.colors.mutedForeground,
        style = ClerkMaterialTheme.typography.bodySmall.withMediumWeight(),
      )
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = dp24, vertical = dp16),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          modifier = Modifier.size(dp24),
          painter = painterResource(R.drawable.ic_fingerprint),
          contentDescription = null,
          tint = ClerkMaterialTheme.colors.mutedForeground,
        )
        Spacer(modifier = Modifier.size(dp16))
        Text(
          modifier = Modifier.weight(1f),
          text = stringResource(R.string.sign_in_with_biometrics),
          color = ClerkMaterialTheme.colors.foreground,
          style = ClerkMaterialTheme.typography.bodyMedium,
        )
        Switch(
          checked = isEnabled,
          onCheckedChange = onCheckedChange,
          enabled = !isLoading,
          colors =
            SwitchDefaults.colors(
              checkedThumbColor = ClerkMaterialTheme.colors.primaryForeground,
              checkedTrackColor = ClerkMaterialTheme.colors.primary,
              uncheckedThumbColor = ClerkMaterialTheme.colors.mutedForeground,
              uncheckedTrackColor = ClerkMaterialTheme.colors.muted,
            ),
        )
      }
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileTrustedDeviceSectionImpl(isEnabled = true, isLoading = false, onCheckedChange = {})
}
