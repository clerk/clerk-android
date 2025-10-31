package com.clerk.ui.userprofile.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.LocalUserProfileState
import com.clerk.ui.userprofile.PreviewUserProfileStateProvider
import com.clerk.ui.userprofile.connectedaccount.UserProfileExternalAccountSection
import com.clerk.ui.userprofile.email.UserProfileEmailSection
import com.clerk.ui.userprofile.phone.UserProfilePhoneSection
import com.clerk.ui.userprofile.security.UserProfileSecurityFooter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
fun UserProfileDetailView(modifier: Modifier = Modifier) {
  val user by Clerk.userFlow.collectAsStateWithLifecycle()
  UserProfileDetailViewImpl(
    emailAddresses = user.sortedEmailAddresses(),
    phoneNumbers = user.sortedPhoneNumbers(),
    externalAccounts = user.sortedExternalAccounts(),
    modifier = modifier,
  )
}

@Composable
fun UserProfileDetailViewImpl(
  emailAddresses: ImmutableList<EmailAddress>,
  phoneNumbers: ImmutableList<PhoneNumber>,
  externalAccounts: ImmutableList<ExternalAccount>,
  modifier: Modifier = Modifier,
) {
  val userProfileState = LocalUserProfileState.current
  val scrollState = rememberScrollState()
  ClerkMaterialTheme {
    Scaffold(
      modifier = modifier,
      topBar = {
        ClerkTopAppBar(
          onBackPressed = { userProfileState.navigateBack() },
          title = stringResource(R.string.profile),
          hasLogo = false,
        )
      },
      bottomBar = { UserProfileSecurityFooter() },
    ) { innerPadding ->
      Column(
        modifier =
          Modifier.fillMaxSize()
            .background(ClerkMaterialTheme.colors.background)
            .padding(innerPadding)
            .verticalScroll(scrollState)
      ) {
        HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
        Spacers.Vertical.Spacer32()
        UserProfileEmailSection(emailAddresses = emailAddresses)
        HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
        Spacers.Vertical.Spacer16()
        UserProfilePhoneSection(phoneNumbers = phoneNumbers)
        Spacers.Vertical.Spacer16()
        UserProfileExternalAccountSection(externalAccounts)
      }
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  PreviewUserProfileStateProvider {
    ClerkMaterialTheme {
      UserProfileDetailViewImpl(
        emailAddresses =
          persistentListOf(
            EmailAddress(
              id = "email_1",
              emailAddress = "sam@clerk.dev",
              verification = Verification(Verification.Status.UNVERIFIED),
            ),
            EmailAddress(
              id = "email_2",
              emailAddress = "sam+ext@clerk.com",
              verification = Verification(Verification.Status.VERIFIED),
              linkedTo = listOf(EmailAddress.LinkedEntity("email_1", type = "OAUTH")),
            ),
          ),
        persistentListOf(
          PhoneNumber(id = "phone_1", phoneNumber = "15555550100", reservedForSecondFactor = true),
          PhoneNumber(id = "phone_2", phoneNumber = "15555550101"),
        ),
        persistentListOf(
          ExternalAccount(
            id = "eac_34o5pCBEhohJtr1Ni14YiX8aQ0K",
            identificationId = "idn_34o5pAvdtMtjAAdeFBfTkRfs77e",
            provider = "oauth_google",
            providerUserId = "102662613248529322762",
            emailAddress = "sam@clerk.dev",
            approvedScopes =
              "email https://www.googleapis.com/auth/userinfo.email" +
                " https://www.googleapis.com/auth/userinfo.profile openid profile",
            createdAt = 1L,
          ),
          ExternalAccount(
            id = "eac_34o5pCBEhohJtr1Ni14YiX8aQ0K",
            identificationId = "idn_34o5pAvdtMtjAAdeFBfTkRfs77e",
            provider = "oauth_linear",
            providerUserId = "102662613248529322762",
            emailAddress = "sam@clerk.dev",
            approvedScopes =
              "email https://www.googleapis.com/auth/userinfo.email" +
                " https://www.googleapis.com/auth/userinfo.profile openid profile",
            createdAt = 1L,
          ),
        ),
      )
    }
  }
}

fun User?.sortedEmailAddresses(): ImmutableList<EmailAddress> {
  return this?.emailAddresses
    ?.sortedWith(
      compareByDescending<EmailAddress> { it == primaryEmailAddress }.thenBy { it.createdAt ?: 0L }
    )
    ?.toImmutableList() ?: persistentListOf()
}

fun User?.sortedPhoneNumbers(): ImmutableList<PhoneNumber> {
  return this?.phoneNumbers
    ?.sortedWith(
      compareByDescending<PhoneNumber> { it == primaryPhoneNumber }.thenBy { it.createdAt ?: 0L }
    )
    ?.toImmutableList() ?: persistentListOf()
}

fun User?.sortedExternalAccounts(): ImmutableList<ExternalAccount> {

  return this?.externalAccounts
    ?.filter { account ->
      val verification = account.verification
      verification?.status == Verification.Status.VERIFIED || verification?.error != null
    }
    ?.sortedBy { it.createdAt }
    ?.toImmutableList() ?: persistentListOf()
}
