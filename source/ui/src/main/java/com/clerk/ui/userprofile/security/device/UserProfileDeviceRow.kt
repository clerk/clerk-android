package com.clerk.ui.userprofile.security.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.clerk.api.Clerk
import com.clerk.api.session.Session
import com.clerk.api.session.SessionActivity
import com.clerk.api.session.isThisDevice
import com.clerk.ui.R
import com.clerk.ui.core.badge.Badge
import com.clerk.ui.core.badge.ClerkBadgeType
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp4
import com.clerk.ui.core.extensions.browserFormatted
import com.clerk.ui.core.extensions.deviceImage
import com.clerk.ui.core.extensions.deviceText
import com.clerk.ui.core.extensions.ipAndLocationFormatted
import com.clerk.ui.core.extensions.lastActiveRelativeTime
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun UserProfileDeviceRow(modifier: Modifier = Modifier, onError: (String?) -> Unit) {
  val session by Clerk.sessionFlow.collectAsStateWithLifecycle()
  UserProfileDeviceRowImpl(session = session, modifier = modifier, onError = onError)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileDeviceRowImpl(
  session: Session?,
  modifier: Modifier = Modifier,
  forceIsThisDevice: Boolean = false,
  viewModel: DeviceViewModel = viewModel(),
  onError: (String?) -> Unit,
) {
  var showDropdown by remember { mutableStateOf(false) }
  val state by viewModel.state.collectAsStateWithLifecycle()
  LaunchedEffect(state) {
    if (state is DeviceViewModel.State.Error) {
      onError((state as DeviceViewModel.State.Error).message)
    }
  }

  session?.latestActivity?.let { activity ->
    ClerkMaterialTheme {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(ClerkMaterialTheme.colors.background)
            .padding(vertical = dp16, horizontal = dp24)
            .then(modifier)
      ) {
        DeviceInfoWithIcon(activity, session, forceIsThisDevice)
        Spacer(modifier = Modifier.weight(1f))
        ExposedDropdownMenuBox(expanded = showDropdown, onExpandedChange = { showDropdown = it }) {
          IconButton(
            modifier =
              Modifier.menuAnchor(
                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                enabled = true,
              ),
            onClick = { showDropdown = true },
          ) {
            Icon(
              imageVector = Icons.Outlined.MoreVert,
              contentDescription = stringResource(R.string.more_options),
              tint = ClerkMaterialTheme.colors.mutedForeground,
            )
          }

          DropdownMenu(
            modifier = Modifier.width(IntrinsicSize.Min),
            expanded = showDropdown,
            shape = ClerkMaterialTheme.shape,
            onDismissRequest = { showDropdown = false },
          ) {
            DropdownMenuItem(
              text = {
                Text(
                  text = "Sign out of this device",
                  style = ClerkMaterialTheme.typography.bodyLarge,
                  color = ClerkMaterialTheme.colors.danger,
                )
              },
              onClick = {
                viewModel.signOut(session)
                showDropdown = false
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DeviceInfoWithIcon(
  activity: SessionActivity,
  session: Session,
  forceIsThisDevice: Boolean,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(dp16, alignment = Alignment.Start),
    verticalAlignment = Alignment.Top,
  ) {
    AsyncImage(
      modifier = Modifier.size(dp24),
      model = activity.deviceImage(),
      contentDescription = null,
      fallback = painterResource(R.drawable.ic_mobile),
    )
    Column {
      Row(
        horizontalArrangement = Arrangement.spacedBy(dp4, alignment = Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(activity.deviceText()),
          color = ClerkMaterialTheme.colors.foreground,
          style = ClerkMaterialTheme.typography.bodyLarge,
        )
        if (session.isThisDevice || forceIsThisDevice) {
          Badge(text = stringResource(R.string.this_device), badgeType = ClerkBadgeType.Secondary)
        }
      }
      Text(
        text = activity.browserFormatted,
        style = ClerkMaterialTheme.typography.bodyMedium,
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
      Text(
        text = activity.ipAndLocationFormatted,
        style = ClerkMaterialTheme.typography.bodyMedium,
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
      Text(
        text = session.lastActiveRelativeTime,
        style = ClerkMaterialTheme.typography.bodyMedium,
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileDeviceRowImpl(
    forceIsThisDevice = true,
    onError = {},
    session =
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
  )
}
