package com.clerk.ui.userprofile.email

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.EmailAddress
import com.clerk.api.VerificationStatus
import com.clerk.ui.R
import com.clerk.ui.core.badge.Badge
import com.clerk.ui.core.badge.ClerkBadgeType
import com.clerk.ui.core.composition.LocalClerk
import com.clerk.ui.core.composition.clerkViewModel
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp4
import com.clerk.ui.core.dimens.dp48
import com.clerk.ui.core.menu.DropDownItem
import com.clerk.ui.core.menu.ItemMoreMenu
import com.clerk.ui.core.preview.ClerkPreview
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UserProfileEmailRow(
  emailAddress: EmailAddress,
  onError: (String) -> Unit,
  onVerify: (EmailAddress) -> Unit,
  modifier: Modifier = Modifier,
  isInteractive: Boolean = true,
  viewModel: EmailViewModel? = if (isInteractive) clerkViewModel { EmailViewModel(it) } else null,
) {

  if (isInteractive && viewModel != null) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReportEmailRowError(state, onError)
  }

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .background(ClerkMaterialTheme.colors.background)
        .padding(start = dp24)
        .padding(vertical = dp16)
        .then(modifier),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val isPrimary = (LocalClerk.current.user?.primaryEmailAddressId == emailAddress.id)

    EmailWithBadge(isPrimary, emailAddress)
    Spacer(modifier = Modifier.weight(1f))
    EmailRowActions(
      emailAddress = emailAddress,
      isInteractive = isInteractive,
      viewModel = viewModel,
      onVerify = onVerify,
    )
  }
}

@Composable
private fun EmailRowActions(
  emailAddress: EmailAddress,
  isInteractive: Boolean,
  viewModel: EmailViewModel?,
  onVerify: (EmailAddress) -> Unit,
) {
  val canRemove =
    (LocalClerk.current.environment.userSettings.attributes["email_address"]?.immutable != true)
  val canSetAsPrimary =
    (LocalClerk.current.environment.userSettings.attributes["email_address"]?.immutable != true)
  val isPrimary = (LocalClerk.current.user?.primaryEmailAddressId == emailAddress.id)
  val isVerified = emailAddress.verification.status == VerificationStatus.Verified
  val shouldShowMenu = canRemove || (canSetAsPrimary && !isPrimary && isVerified) || !isVerified

  if (LocalInspectionMode.current || !shouldShowMenu) return
  if (!isInteractive || viewModel == null) {
    Spacer(modifier = Modifier.size(dp48))
    return
  }
  ItemMoreMenu(
    dropDownItems =
      persistentListOf(
        DropDownItem(
          id = EmailAction.SetAsPrimary,
          text = stringResource(R.string.set_as_primary),
          isHidden = !canSetAsPrimary || isPrimary || !isVerified,
        ),
        DropDownItem(
          id = EmailAction.Verify,
          text = stringResource(R.string.verify),
          isHidden = isVerified,
        ),
        DropDownItem(
          id = EmailAction.Remove,
          text = stringResource(R.string.remove_email),
          danger = true,
          isHidden = !canRemove,
        ),
      ),
    onClick = {
      when (it) {
        EmailAction.SetAsPrimary -> viewModel.setAsPrimary(emailAddress)
        EmailAction.Verify -> onVerify(emailAddress)
        EmailAction.Remove -> viewModel.remove(emailAddress)
      }
    },
  )
}

@Composable
internal fun ReportEmailRowError(state: EmailViewModel.State, onError: (String) -> Unit) {
  val errorMessage = (state as? EmailViewModel.State.Failure)?.message
  LaunchedEffect(errorMessage) {
    if (errorMessage != null) {
      onError(errorMessage)
    }
  }
}

@Composable
private fun EmailWithBadge(isPrimary: Boolean, emailAddress: EmailAddress) {
  Column {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(dp4),
    ) {
      if (isPrimary) {
        Badge(text = stringResource(R.string.primary), badgeType = ClerkBadgeType.Secondary)
        Spacers.Vertical.Spacer4()
      }
      if (emailAddress.verification.status != VerificationStatus.Verified) {
        Badge(text = stringResource(R.string.unverified), badgeType = ClerkBadgeType.Warning)
        Spacers.Vertical.Spacer4()
      }
      if (emailAddress.linkedTo.isNotEmpty()) {
        Badge(text = stringResource(R.string.linked), badgeType = ClerkBadgeType.Secondary)
        Spacers.Vertical.Spacer4()
      }
    }
    Spacers.Vertical.Spacer4()
    Text(
      text = emailAddress.emailAddress,
      style = ClerkMaterialTheme.typography.bodyLarge,
      color = ClerkMaterialTheme.colors.foreground,
    )
  }
}

internal enum class EmailAction {
  SetAsPrimary,
  Verify,
  Remove,
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkPreview { clerk ->
    ClerkMaterialTheme {
      Column(
        modifier =
          Modifier.background(color = ClerkMaterialTheme.colors.muted).padding(vertical = dp24)
      ) {
        UserProfileEmailRow(
          onError = {},
          onVerify = {},
          emailAddress = clerk.user!!.emailAddresses[0],
        )
        UserProfileEmailRow(
          onError = {},
          onVerify = {},
          emailAddress = clerk.user!!.emailAddresses[1],
        )
        UserProfileEmailRow(
          onError = {},
          onVerify = {},
          emailAddress = clerk.user!!.emailAddresses[0],
        )
      }
    }
  }
}
