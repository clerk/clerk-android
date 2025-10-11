package com.clerk.ui.userprofile.security.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.session.Session
import com.clerk.api.session.SessionActivity
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp32
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun UserProfileDevicesSection(
  modifier: Modifier = Modifier,
  viewModel: AllDevicesViewModel = viewModel(),
  errorFetchingDevices: () -> Unit,
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  UserProfileDevicesSectionImpl(
    modifier = modifier,
    state = state,
    errorFetchingDevices = errorFetchingDevices,
  )
}

@Composable
private fun UserProfileDevicesSectionImpl(
  state: AllDevicesViewModel.State,
  modifier: Modifier = Modifier,
  errorFetchingDevices: () -> Unit,
) {
  ClerkMaterialTheme {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.background)
          .then(modifier),
      contentAlignment = Alignment.Center,
    ) {
      when (state) {
        is AllDevicesViewModel.State.Error -> errorFetchingDevices()
        AllDevicesViewModel.State.Idle -> {}
        AllDevicesViewModel.State.Loading -> CircularProgressIndicator()
        is AllDevicesViewModel.State.Success -> {
          Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = dp24).padding(top = dp32)
          ) {
            Text(
              stringResource(R.string.active_devices).uppercase(),
              style = ClerkMaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
              color = ClerkMaterialTheme.colors.mutedForeground,
            )
            Spacers.Vertical.Spacer16()
            LazyColumn { items(state.devices) { UserProfileDeviceRow(session = it, onError = {}) } }
          }
        }
      }
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileDevicesSectionImpl(
    state =
      AllDevicesViewModel.State.Success(
        listOf(
          Session(
            id = "123456",
            expireAt = 1759976778801,
            lastActiveAt = 1759976778801,
            createdAt = 1759976778801,
            updatedAt = 1759976778801,
            latestActivity =
              SessionActivity(
                id = "activity_123",
                ipAddress = "196.172.122.88",
                isMobile = true,
                browserName = "Chrome",
                browserVersion = "139.0.0.0",
                city = "San Francisco",
                country = "CA",
              ),
          ),
          Session(
            id = "123457",
            expireAt = 1759976778801,
            lastActiveAt = 1759976778801,
            createdAt = 1759976778801,
            updatedAt = 1759976778801,
            latestActivity =
              SessionActivity(
                id = "activity_123",
                ipAddress = "196.172.122.88",
                isMobile = true,
                browserName = "Chrome",
                browserVersion = "139.0.0.0",
                city = "San Francisco",
                country = "CA",
              ),
          ),
        )
      ),
    errorFetchingDevices = {},
  )
}
