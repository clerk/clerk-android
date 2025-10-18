package com.clerk.ui.userprofile.security.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.session.Session
import com.clerk.api.session.SessionActivity
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun UserProfileDevicesSection(
  devices: ImmutableList<Session>,
  modifier: Modifier = Modifier,
) {
  UserProfileDevicesSectionImpl(modifier = modifier, devices = devices)
}

@Composable
private fun UserProfileDevicesSectionImpl(
  devices: ImmutableList<Session>,
  modifier: Modifier = Modifier,
) {
  ClerkMaterialTheme {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.background)
          .padding(dp16)
          .then(modifier)
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          modifier = Modifier.padding(horizontal = dp24).then(modifier),
          text = stringResource(R.string.active_devices).uppercase(),
          color = ClerkMaterialTheme.colors.mutedForeground,
          style = ClerkMaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
        )
        Spacers.Vertical.Spacer16()
        LazyColumn { items(devices) { UserProfileDeviceRow(session = it, onError = {}) } }
      }
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileDevicesSectionImpl(
    persistentListOf(
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
  )
}
