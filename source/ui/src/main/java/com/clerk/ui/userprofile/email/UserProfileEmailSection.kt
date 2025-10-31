package com.clerk.ui.userprofile.email

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.emailaddress.isPrimary
import com.clerk.api.network.model.verification.Verification
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.common.UserProfileButtonRow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun UserProfileEmailSection(
  emailAddresses: ImmutableList<EmailAddress>,
  modifier: Modifier = Modifier,
) {
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.background)
          .then(modifier)
    ) {
      Text(
        modifier = Modifier.padding(horizontal = dp24),
        text = stringResource(R.string.email_addresses).uppercase(),
        style = ClerkMaterialTheme.typography.bodySmall.withMediumWeight(),
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
      emailAddresses.forEach {
        UserProfileEmailRow(
          isPrimary = it.isPrimary,
          emailAddress = it,
          onVerify = {},
          onError = {},
        )
      }
      UserProfileButtonRow(text = "Add email address", onClick = {})
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileEmailSection(
    persistentListOf(
      EmailAddress(
        id = "123",
        emailAddress = "user@example.com",
        verification = Verification(status = Verification.Status.VERIFIED),
      ),
      EmailAddress(
        id = "123",
        emailAddress = "user@example.com",
        verification = Verification(status = Verification.Status.UNVERIFIED),
      ),
      EmailAddress(
        id = "123",
        emailAddress = "user@example.com",
        linkedTo = listOf(EmailAddress.LinkedEntity(id = "1", type = "email")),
        verification = Verification(status = Verification.Status.VERIFIED),
      ),
    )
  )
}
