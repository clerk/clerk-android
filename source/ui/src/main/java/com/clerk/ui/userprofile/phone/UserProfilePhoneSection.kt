package com.clerk.ui.userprofile.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.api.PhoneNumber
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.core.preview.ClerkPreview
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.common.UserProfileButtonRow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal fun LazyListScope.userProfilePhoneSection(
  clerk: Clerk,
  phoneNumbers: ImmutableList<PhoneNumber>,
  onError: (String) -> Unit,
  onAddPhoneNumberClick: () -> Unit,
  onVerify: (PhoneNumber) -> Unit,
  isInteractive: Boolean = true,
) {
  item(key = "user_profile_phone_header") {
    Text(
      modifier = Modifier.padding(horizontal = dp24),
      text = stringResource(R.string.phone_number).uppercase(),
      style = ClerkMaterialTheme.typography.bodySmall.withMediumWeight(),
      color = ClerkMaterialTheme.colors.mutedForeground,
    )
  }
  item(key = "user_profile_phone_header_spacing") { Spacers.Vertical.Spacer16() }
  items(
    items = phoneNumbers,
    key = { phoneNumber -> "user_profile_phone_${phoneNumber.id}" },
    contentType = { "user_profile_phone" },
  ) { phoneNumber ->
    UserProfilePhoneRow(
      phoneNumber = phoneNumber,
      onError = onError,
      onVerify = onVerify,
      isInteractive = isInteractive,
    )
  }
  if (clerk.environment.userSettings.attributes["phone_number"]?.immutable != true) {
    item(key = "user_profile_phone_add") {
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
  ClerkPreview { clerk ->
    ClerkMaterialTheme {
      Box(modifier = Modifier.background(color = ClerkMaterialTheme.colors.background)) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
          userProfilePhoneSection(
            clerk = clerk,
            onError = {},
            onAddPhoneNumberClick = {},
            onVerify = {},
            phoneNumbers = clerk.user!!.phoneNumbers.toImmutableList(),
          )
        }
      }
    }
  }
}
