package com.clerk.ui.userprofile.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.ui.R
import com.clerk.ui.core.badge.Badge
import com.clerk.ui.core.badge.ClerkBadgeType
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.menu.DropDownItem
import com.clerk.ui.core.menu.ItemMoreMenu
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.util.formattedAsPhoneNumberIfPossible
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UserProfilePhoneRow(
  phoneNumber: PhoneNumber,
  onVerify: () -> Unit,
  onError: (String) -> Unit,
  modifier: Modifier = Modifier,
  isPrimary: Boolean = false,
) {

  val isPreview = LocalInspectionMode.current

  ClerkMaterialTheme {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(ClerkMaterialTheme.colors.background)
          .padding(horizontal = dp24, vertical = dp16)
          .then(modifier),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      PhoneWithBadge(isPrimary, phoneNumber)
      Spacer(modifier = Modifier.weight(1f))
      if (!isPreview) {
        ItemMoreMenu(
          dropDownItems =
            persistentListOf(
              DropDownItem(
                id = PhoneAction.SetAsPrimary,
                text = stringResource(R.string.set_as_primary),
              ),
              DropDownItem(id = PhoneAction.Verify, text = stringResource(R.string.verify)),
              DropDownItem(
                id = PhoneAction.Remove,
                text = stringResource(R.string.remove_email),
                danger = true,
              ),
            ),
          onClick = {
            when (it) {
              PhoneAction.SetAsPrimary -> TODO()
              PhoneAction.Verify -> TODO()
              PhoneAction.Remove -> TODO()
            }
          },
        )
      }
    }
  }
}

@Composable
private fun PhoneWithBadge(isPrimary: Boolean, phoneNumber: PhoneNumber) {
  Column {
    if (isPrimary) {
      Badge(text = stringResource(R.string.primary), badgeType = ClerkBadgeType.Secondary)
      Spacers.Vertical.Spacer4()
    }
    if (phoneNumber.verification?.status != Verification.Status.VERIFIED) {
      Badge(text = stringResource(R.string.unverified), badgeType = ClerkBadgeType.Warning)
      Spacers.Vertical.Spacer4()
    }
    if (phoneNumber.reservedForSecondFactor) {
      Badge(text = stringResource(R.string.mfa_reserved), badgeType = ClerkBadgeType.Secondary)
      Spacers.Vertical.Spacer4()
    }
    Text(
      text = phoneNumber.phoneNumber.formattedAsPhoneNumberIfPossible,
      style = ClerkMaterialTheme.typography.bodyLarge,
      color = ClerkMaterialTheme.colors.foreground,
    )
  }
}

internal enum class PhoneAction {
  SetAsPrimary,
  Verify,
  Remove,
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.background(color = ClerkMaterialTheme.colors.muted).padding(vertical = dp24)
    ) {
      UserProfilePhoneRow(
        isPrimary = true,
        onError = {},
        onVerify = {},
        phoneNumber =
          PhoneNumber(
            id = "phone_1",
            phoneNumber = "15555550100",
            verification = Verification(Verification.Status.VERIFIED),
          ),
      )
      UserProfilePhoneRow(
        isPrimary = false,
        onError = {},
        onVerify = {},
        phoneNumber =
          PhoneNumber(
            id = "phone_1",
            phoneNumber = "15555550100",
            reservedForSecondFactor = true,
            verification = Verification(Verification.Status.VERIFIED),
          ),
      )
      UserProfilePhoneRow(
        isPrimary = false,
        onError = {},
        onVerify = {},
        phoneNumber =
          PhoneNumber(
            id = "phone_1",
            phoneNumber = "15555550100",
            verification = Verification(Verification.Status.UNVERIFIED),
          ),
      )
    }
  }
}
