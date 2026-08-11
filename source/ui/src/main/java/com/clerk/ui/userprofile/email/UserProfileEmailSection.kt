package com.clerk.ui.userprofile.email

import androidx.compose.foundation.background
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
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.network.model.verification.Verification
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.common.UserProfileButtonRow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal fun LazyListScope.userProfileEmailSection(
  emailAddresses: ImmutableList<EmailAddress>,
  onError: (String) -> Unit,
  onAddEmailClick: () -> Unit,
  onVerify: (EmailAddress) -> Unit,
  isInteractive: Boolean = true,
) {
  item(key = "user_profile_email_header") {
    Text(
      modifier = Modifier.padding(horizontal = dp24),
      text = stringResource(R.string.email_addresses).uppercase(),
      style = ClerkMaterialTheme.typography.bodySmall.withMediumWeight(),
      color = ClerkMaterialTheme.colors.mutedForeground,
    )
  }
  item(key = "user_profile_email_header_spacing") { Spacers.Vertical.Spacer16() }
  items(
    items = emailAddresses,
    key = { emailAddress -> "user_profile_email_${emailAddress.id}" },
    contentType = { "user_profile_email" },
  ) { emailAddress ->
    UserProfileEmailRow(
      emailAddress = emailAddress,
      onError = onError,
      onVerify = onVerify,
      isInteractive = isInteractive,
    )
  }
  if (!Clerk.isEmailImmutable) {
    item(key = "user_profile_email_add") {
      UserProfileButtonRow(
        text = stringResource(R.string.add_email_address),
        onClick = onAddEmailClick,
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkMaterialTheme {
    LazyColumn(
      modifier = Modifier.fillMaxWidth().background(ClerkMaterialTheme.colors.background)
    ) {
      userProfileEmailSection(
        onError = {},
        onAddEmailClick = {},
        onVerify = {},
        emailAddresses =
          persistentListOf(
            EmailAddress(
              id = "email_1",
              emailAddress = "user@example.com",
              verification = Verification(status = Verification.Status.VERIFIED),
            ),
            EmailAddress(
              id = "email_2",
              emailAddress = "user@example.com",
              verification = Verification(status = Verification.Status.UNVERIFIED),
            ),
            EmailAddress(
              id = "email_3",
              emailAddress = "user@example.com",
              linkedTo = listOf(EmailAddress.LinkedEntity(id = "1", type = "email")),
              verification = Verification(status = Verification.Status.VERIFIED),
            ),
          ),
      )
    }
  }
}
