package com.clerk.ui.userprofile.email

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.network.model.verification.Verification
import com.clerk.ui.R
import com.clerk.ui.core.badge.Badge
import com.clerk.ui.core.badge.ClerkBadgeType
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileEmailRow(
  emailAddress: EmailAddress,
  onVerify: () -> Unit,
  onError: (String) -> Unit,
  modifier: Modifier = Modifier,
  isPrimary: Boolean = false,
  viewModel: EmailViewModel = viewModel(),
) {

  val state by viewModel.state.collectAsStateWithLifecycle()
  if (state is EmailViewModel.State.Failure) {
    onError((state as EmailViewModel.State.Failure).message)
  }
  ClerkMaterialTheme {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(ClerkMaterialTheme.colors.background)
          .padding(horizontal = dp24, vertical = dp16)
          .then(modifier),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        if (isPrimary) {
          Badge(text = stringResource(R.string.primary), badgeType = ClerkBadgeType.Secondary)
          Spacers.Vertical.Spacer4()
        }
        if (emailAddress.verification?.status != Verification.Status.VERIFIED) {
          Badge(text = stringResource(R.string.unverified), badgeType = ClerkBadgeType.Warning)
          Spacers.Vertical.Spacer4()
        }
        if (emailAddress.linkedTo?.isNotEmpty() == true) {
          Badge(text = stringResource(R.string.linked), badgeType = ClerkBadgeType.Secondary)
          Spacers.Vertical.Spacer4()
        }
        Text(
          text = emailAddress.emailAddress,
          style = ClerkMaterialTheme.typography.bodyLarge,
          color = ClerkMaterialTheme.colors.foreground,
        )
      }
      Spacer(modifier = Modifier.weight(1f))
      DropDownMenu(
        isPrimary,
        emailAddress,
        onSetAsPrimary = { viewModel.setAsPrimary(emailAddress) },
        onVerify = onVerify,
        onRemoveEmail = { viewModel.remove(emailAddress) },
      )
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DropDownMenu(
  isPrimary: Boolean,
  emailAddress: EmailAddress,
  onSetAsPrimary: () -> Unit,
  onVerify: () -> Unit,
  onRemoveEmail: () -> Unit,
) {
  var showDropdown by remember { mutableStateOf(false) }

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

    DropDownMenuContent(
      isPrimary = isPrimary,
      emailAddress = emailAddress,
      showDropdown = showDropdown,
      onSetAsPrimary = {
        onSetAsPrimary()
        showDropdown = false
      },
      onVerify = {
        onVerify()
        showDropdown = false
      },
      onRemoveEmail = {
        onRemoveEmail()
        showDropdown = false
      },
      onDismiss = { showDropdown = false },
    )
  }
}

@Composable
private fun DropDownMenuContent(
  isPrimary: Boolean,
  emailAddress: EmailAddress,
  showDropdown: Boolean,
  onSetAsPrimary: () -> Unit,
  onVerify: () -> Unit,
  onRemoveEmail: () -> Unit,
  onDismiss: () -> Unit,
) {
  DropdownMenu(
    modifier = Modifier.defaultMinSize(minWidth = 200.dp),
    expanded = showDropdown,
    shape = ClerkMaterialTheme.shape,
    onDismissRequest = onDismiss,
  ) {
    if (!isPrimary)
      DropDownItem(R.string.set_as_primary, ClerkMaterialTheme.colors.foreground, onSetAsPrimary)
    if (emailAddress.verification?.status != Verification.Status.VERIFIED)
      DropDownItem(R.string.verify, ClerkMaterialTheme.colors.foreground, onVerify)
    DropDownItem(R.string.remove_email, ClerkMaterialTheme.colors.danger, onRemoveEmail)
  }
}

@Composable
private fun DropDownItem(textRes: Int, color: Color, onClick: () -> Unit) {
  DropdownMenuItem(
    text = {
      Text(
        text = stringResource(textRes),
        style = ClerkMaterialTheme.typography.bodyLarge,
        color = color,
      )
    },
    onClick = onClick,
  )
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.background(color = ClerkMaterialTheme.colors.muted).padding(vertical = dp24)
    ) {
      UserProfileEmailRow(
        isPrimary = true,
        onError = {},
        onVerify = {},
        emailAddress =
          EmailAddress(
            id = "123",
            emailAddress = "user@example.com",
            verification = Verification(status = Verification.Status.VERIFIED),
          ),
      )
      UserProfileEmailRow(
        isPrimary = false,
        onError = {},
        onVerify = {},
        emailAddress =
          EmailAddress(
            id = "123",
            emailAddress = "user@example.com",
            verification = Verification(status = Verification.Status.UNVERIFIED),
          ),
      )
      UserProfileEmailRow(
        isPrimary = false,
        onError = {},
        onVerify = {},
        emailAddress =
          EmailAddress(
            id = "123",
            emailAddress = "user@example.com",
            linkedTo = listOf(EmailAddress.LinkedEntity(id = "1", type = "email")),
            verification = Verification(status = Verification.Status.VERIFIED),
          ),
      )
    }
  }
}
