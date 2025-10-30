package com.clerk.ui.userprofile.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.api.user.User
import com.clerk.ui.R
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme

@Composable
fun UserProfileDetailView(modifier: Modifier = Modifier) {
  val scrollState = rememberScrollState()
  val user by Clerk.userFlow.collectAsStateWithLifecycle()
  ClerkMaterialTheme {
    Scaffold(
      modifier = modifier,
      topBar = { ClerkTopAppBar(onBackPressed = {}, hasLogo = false, title = "Profile ") },
    ) { innerPadding ->
      Column(
        modifier =
          Modifier.fillMaxSize()
            .background(ClerkMaterialTheme.colors.background)
            .padding(innerPadding)
            .verticalScroll(scrollState)
      ) {
        Spacers.Vertical.Spacer32()
        Text(
          modifier = Modifier.padding(horizontal = dp24),
          text = stringResource(R.string.email_addresses).uppercase(),
          style = ClerkMaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
          color = ClerkMaterialTheme.colors.mutedForeground,
        )
      }
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileDetailView()
}

fun User.sortedEmailAddress(): List<EmailAddress> {
  return emailAddresses.sortedWith(
    compareByDescending<EmailAddress> { it == primaryEmailAddress }.thenBy { it.createdAt ?: 0L }
  )
}

fun User.sortedPhoneNumbers(): List<PhoneNumber> {
  return phoneNumbers.sortedWith(
    compareByDescending<PhoneNumber> { it == primaryPhoneNumber }.thenBy { it.createdAt ?: 0L }
  )
}

fun User.sortedExternalAccounts(): List<ExternalAccount> {
  return this.externalAccounts
    ?.filter { account ->
      val verification = account.verification
      verification?.status == Verification.Status.VERIFIED || verification?.error != null
    }
    ?.sortedBy { it.createdAt } ?: emptyList()
}
