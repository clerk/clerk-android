package com.clerk.ui.userprofile.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.clerk.api.EmailAddress
import com.clerk.api.ExternalAccount
import com.clerk.api.PhoneNumber
import com.clerk.api.User
import com.clerk.api.VerificationStatus
import com.clerk.ui.R
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.common.runUiOperation
import com.clerk.ui.core.composition.LocalClerk
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.error.ClerkErrorSnackbar
import com.clerk.ui.core.preview.ClerkPreview
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.LocalUserProfileState
import com.clerk.ui.userprofile.PreviewUserProfileStateProvider
import com.clerk.ui.userprofile.connectedaccount.userProfileExternalAccountSection
import com.clerk.ui.userprofile.email.userProfileEmailSection
import com.clerk.ui.userprofile.phone.userProfilePhoneSection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

private const val EMAIL_IMMUTABLE_SNACKBAR_MESSAGE =
  "Email addresses cannot be changed for this application."

@Composable
fun UserProfileDetailView(modifier: Modifier = Modifier) {
  UserProfileDetailViewContent(modifier = modifier)
}

@Composable
internal fun UserProfileDetailViewWithBackHandler(
  modifier: Modifier = Modifier,
  onBackPressed: () -> Unit,
) {
  UserProfileDetailViewContent(modifier = modifier, onBackPressed = onBackPressed)
}

@Composable
private fun UserProfileDetailViewContent(modifier: Modifier, onBackPressed: (() -> Unit)? = null) {
  val clerk = LocalClerk.current
  val user = clerk.user
  val destinationLifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
  val isNavigationSettled = destinationLifecycleState == Lifecycle.State.RESUMED
  LaunchedEffect(isNavigationSettled) {
    if (isNavigationSettled) {
      runUiOperation { clerk.user?.reload() }
    }
  }
  UserProfileDetailViewImpl(
    emailAddresses = user.sortedEmailAddresses(),
    phoneNumbers = user.sortedPhoneNumbers(),
    externalAccounts = user.sortedExternalAccounts(),
    isNavigationSettled = isNavigationSettled,
    onBackPressed = onBackPressed,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileDetailViewImpl(
  emailAddresses: ImmutableList<EmailAddress>,
  phoneNumbers: ImmutableList<PhoneNumber>,
  externalAccounts: ImmutableList<ExternalAccount>,
  isNavigationSettled: Boolean = true,
  onBackPressed: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val clerk = LocalClerk.current
  val snackbarHostState = remember { SnackbarHostState() }
  val userProfileState = LocalUserProfileState.current
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()
  var showBottomSheet by remember { mutableStateOf(false) }
  var bottomSheetType by remember { mutableStateOf<BottomSheetMode>(BottomSheetMode.EmailAddress) }

  ClerkMaterialTheme {
    Scaffold(
      modifier = modifier,
      topBar = {
        ClerkTopAppBar(
          onBackPressed = onBackPressed ?: { userProfileState.navigateBack() },
          title = stringResource(R.string.manage_account),
          hasLogo = false,
        )
      },
      snackbarHost = { ClerkErrorSnackbar(snackbarHostState) },
    ) { innerPadding ->
      ProfileContent(
        innerPadding = innerPadding,
        listState = listState,
        data = ProfileContentData(emailAddresses, phoneNumbers, externalAccounts),
        isNavigationSettled = isNavigationSettled,
        actions =
          ProfileContentActions(
            onShowBottomSheet = { type ->
              if (
                type == BottomSheetMode.EmailAddress &&
                  (clerk.environment.userSettings.attributes["email_address"]?.immutable == true)
              ) {
                scope.launch { snackbarHostState.showSnackbar(EMAIL_IMMUTABLE_SNACKBAR_MESSAGE) }
              } else {
                bottomSheetType = type
                showBottomSheet = true
              }
            },
            onError = { errorMessage ->
              scope.launch { snackbarHostState.showSnackbar(errorMessage) }
            },
          ),
      )

      if (showBottomSheet) {
        UserProfileDetailBottomSheet(
          bottomSheetType = bottomSheetType,
          onDismissRequest = { showBottomSheet = false },
          onVerify = {
            bottomSheetType = it
            showBottomSheet = true
          },
          onShowBackupCodes = {
            bottomSheetType = BottomSheetMode.BackupCodes(it)
            showBottomSheet = true
          },
        )
      }
    }
  }
}

@Composable
private fun ProfileContent(
  innerPadding: PaddingValues,
  listState: LazyListState,
  data: ProfileContentData,
  isNavigationSettled: Boolean,
  actions: ProfileContentActions,
) {
  val clerk = LocalClerk.current
  LazyColumn(
    state = listState,
    modifier =
      Modifier.fillMaxSize().background(ClerkMaterialTheme.colors.background).padding(innerPadding),
  ) {
    val showEmailSection =
      (clerk.environment.userSettings.attributes["email_address"]?.enabled == true) &&
        !((clerk.environment.userSettings.attributes["email_address"]?.immutable == true) &&
          data.emailAddresses.isEmpty())
    val showPhoneSection =
      (clerk.environment.userSettings.attributes["phone_number"]?.enabled == true) &&
        !((clerk.environment.userSettings.attributes["phone_number"]?.immutable == true) &&
          data.phoneNumbers.isEmpty())

    item(key = "user_profile_detail_top_divider") {
      HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
    }
    if (showEmailSection) {
      item(key = "user_profile_detail_email_spacing") { Spacers.Vertical.Spacer32() }
      userProfileEmailSection(
        clerk = clerk,
        emailAddresses = data.emailAddresses,
        isInteractive = isNavigationSettled,
        onError = actions.onError,
        onAddEmailClick = { actions.onShowBottomSheet(BottomSheetMode.EmailAddress) },
        onVerify = { actions.onShowBottomSheet(BottomSheetMode.VerifyEmailAddress(it)) },
      )
      item(key = "user_profile_detail_email_divider") {
        HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
      }
    }
    if (showPhoneSection) {
      item(key = "user_profile_detail_phone_spacing") { Spacers.Vertical.Spacer16() }
      userProfilePhoneSection(
        clerk = clerk,
        phoneNumbers = data.phoneNumbers,
        isInteractive = isNavigationSettled,
        onError = actions.onError,
        onAddPhoneNumberClick = { actions.onShowBottomSheet(BottomSheetMode.PhoneNumber) },
        onVerify = { actions.onShowBottomSheet(BottomSheetMode.VerifyPhoneNumber(it)) },
      )
      item(key = "user_profile_detail_phone_divider") {
        HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
      }
    }
    item(key = "user_profile_detail_external_account_spacing") { Spacers.Vertical.Spacer16() }
    userProfileExternalAccountSection(
      externalAccounts = data.externalAccounts,
      isInteractive = isNavigationSettled,
      loadRemoteLogos = isNavigationSettled,
      onError = actions.onError,
      onClickAddAccount = { actions.onShowBottomSheet(BottomSheetMode.ExternalAccount) },
    )
  }
}

private data class ProfileContentData(
  val emailAddresses: ImmutableList<EmailAddress>,
  val phoneNumbers: ImmutableList<PhoneNumber>,
  val externalAccounts: ImmutableList<ExternalAccount>,
)

private data class ProfileContentActions(
  val onShowBottomSheet: (BottomSheetMode) -> Unit,
  val onError: (String) -> Unit,
)

internal sealed interface BottomSheetMode {
  data object ExternalAccount : BottomSheetMode

  data object PhoneNumber : BottomSheetMode

  data object EmailAddress : BottomSheetMode

  data class VerifyEmailAddress(val emailAddress: com.clerk.api.EmailAddress) : BottomSheetMode

  data class VerifyPhoneNumber(val phoneNumber: com.clerk.api.PhoneNumber) : BottomSheetMode

  data class BackupCodes(val backupCodes: List<String>) : BottomSheetMode
}

@PreviewLightDark
@Composable
private fun Preview() {
  ClerkPreview { clerk ->
    PreviewUserProfileStateProvider {
      ClerkMaterialTheme {
        UserProfileDetailViewImpl(
          emailAddresses = clerk.user!!.emailAddresses.toImmutableList(),
          clerk.user!!.phoneNumbers.toImmutableList(),
          clerk.user!!.externalAccounts.toImmutableList(),
        )
      }
    }
  }
}

internal fun User?.sortedEmailAddresses(): ImmutableList<EmailAddress> {
  return this?.emailAddresses
    ?.sortedWith(
      compareByDescending<EmailAddress> { it == primaryEmailAddress }
        .thenBy { it.createdAt ?: java.time.Instant.EPOCH }
    )
    ?.toImmutableList() ?: persistentListOf()
}

internal fun User?.sortedPhoneNumbers(): ImmutableList<PhoneNumber> {
  return this?.phoneNumbers
    ?.sortedWith(
      compareByDescending<PhoneNumber> { it == primaryPhoneNumber }
        .thenBy { it.createdAt ?: java.time.Instant.EPOCH }
    )
    ?.toImmutableList() ?: persistentListOf()
}

internal fun User?.sortedExternalAccounts(): ImmutableList<ExternalAccount> {

  return this?.externalAccounts
    ?.filter { account ->
      val verification = account.verification
      verification?.status == VerificationStatus.Verified || verification?.error != null
    }
    ?.sortedBy { it.createdAt }
    ?.toImmutableList() ?: persistentListOf()
}
