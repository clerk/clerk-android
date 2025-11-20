package com.clerk.ui.userprofile.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.common.UserProfileButtonRow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun UserProfilePhoneSection(
  phoneNumbers: ImmutableList<PhoneNumber>,
  onError: (String) -> Unit,
  modifier: Modifier = Modifier,
  onAddPhoneNumberClick: () -> Unit,
  onVerify: (PhoneNumber) -> Unit,
) {

  ClerkMaterialTheme {
    Column(modifier = modifier.fillMaxWidth()) {
      Text(
        modifier = Modifier.padding(horizontal = dp24),
        text = stringResource(R.string.phone_number).uppercase(),
        style = ClerkMaterialTheme.typography.bodySmall.withMediumWeight(),
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
      Spacers.Vertical.Spacer16()
      phoneNumbers.forEach {
        UserProfilePhoneRow(phoneNumber = it, onError = onError, onVerify = onVerify)
      }

      UserProfileButtonRow(
        text = stringResource(R.string.add_phone_number),
        onClick = onAddPhoneNumberClick,
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    Box(modifier = Modifier.background(color = ClerkMaterialTheme.colors.background)) {
      UserProfilePhoneSection(
        onError = {},
        onAddPhoneNumberClick = {},
        onVerify = {},
        phoneNumbers =
          persistentListOf(
            PhoneNumber(
              id = "phone_1",
              phoneNumber = "15555550101",
              reservedForSecondFactor = true,
            ),
            PhoneNumber(id = "phone_2", phoneNumber = "447911123456"),
          ),
      )
    }
  }
}
