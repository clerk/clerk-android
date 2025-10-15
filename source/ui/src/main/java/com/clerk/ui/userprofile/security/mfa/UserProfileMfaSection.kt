package com.clerk.ui.userprofile.security.mfa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp32
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.common.UserProfileSectionHeader
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
fun UserProfileMfaSection(modifier: Modifier = Modifier) {
  UserProfileMfaSectionImpl(modifier = modifier, mfaItems = buildMfaItemList())
}

@Composable
private fun UserProfileMfaSectionImpl(
  mfaItems: ImmutableList<MfaItem>,
  modifier: Modifier = Modifier,
) {
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.background)
          .padding(top = dp32)
          .then(modifier)
    ) {
      UserProfileSectionHeader(stringResource(R.string.two_step_verification))
      Spacers.Vertical.Spacer16()
      LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(mfaItems) { mfaItem ->
          UserProfileMfaRow(style = mfaItem.style, isDefault = mfaItem.isDefault, onRemove = {})
        }
      }
    }
  }
}

private fun buildMfaItemList(): ImmutableList<MfaItem> {
  val mfaPhoneNumbers =
    Clerk.user
      ?.phoneNumbers
      .orEmpty()
      .filter { it.reservedForSecondFactor }
      .sortedWith(
        compareByDescending<PhoneNumber> { it.defaultSecondFactor }.thenBy { it.createdAt }
      )

  val items: ImmutableList<MfaItem> =
    buildList {
        if (Clerk.mfaPhoneCodeIsEnabled) {
          mfaPhoneNumbers.forEach {
            add(
              MfaItem(
                style = Style.Sms(it),
                isDefault = it.defaultSecondFactor && Clerk.user?.totpEnabled == false,
              )
            )
          }
        }
        if (Clerk.user?.totpEnabled == true) {
          add(MfaItem(style = Style.AuthenticatorApp, isDefault = true))
        }
        if (Clerk.mfaBackupCodeIsEnabled) {
          if (Clerk.user?.backupCodeEnabled == true) {
            add(MfaItem(style = Style.BackupCodes))
          }
        }
      }
      .toImmutableList()
  return items
}

internal data class MfaItem(val style: Style, val isDefault: Boolean = false)

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileMfaSectionImpl(
    mfaItems =
      persistentListOf(
        MfaItem(style = Style.AuthenticatorApp, isDefault = true),
        MfaItem(style = Style.Sms(PhoneNumber(id = "1", "+15555550100")), isDefault = false),
        MfaItem(style = Style.BackupCodes),
      )
  )
}
