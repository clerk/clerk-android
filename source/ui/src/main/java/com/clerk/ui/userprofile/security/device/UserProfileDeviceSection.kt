package com.clerk.ui.userprofile.security.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.SessionWithActivities
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.core.preview.ClerkPreview
import com.clerk.ui.core.preview.previewResource
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun UserProfileDevicesSection(
  devices: ImmutableList<SessionWithActivities>,
  modifier: Modifier = Modifier,
) {
  UserProfileDevicesSectionImpl(modifier = modifier, devices = devices)
}

@Composable
private fun UserProfileDevicesSectionImpl(
  devices: ImmutableList<SessionWithActivities>,
  modifier: Modifier = Modifier,
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
        text = stringResource(R.string.active_devices).uppercase(),
        color = ClerkMaterialTheme.colors.mutedForeground,
        style = ClerkMaterialTheme.typography.bodySmall.withMediumWeight(),
      )
      Column(modifier = Modifier.fillMaxWidth()) {
        devices.forEach { session ->
          UserProfileDeviceRow(session = session, onError = {})
        }
      }
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkPreview { clerk ->
    UserProfileDevicesSectionImpl(
      persistentListOf(
        (previewResource("SessionWithActivities") as SessionWithActivities),
        (previewResource("SessionWithActivities") as SessionWithActivities),
      )
    )
  }
}
