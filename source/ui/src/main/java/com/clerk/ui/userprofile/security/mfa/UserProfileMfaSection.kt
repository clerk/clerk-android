package com.clerk.ui.userprofile.security.mfa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.extensions.withMediumWeight
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.common.UserProfileButtonRow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun UserProfileMfaSection(modifier: Modifier = Modifier, onAdd: () -> Unit) {
  val user by Clerk.userFlow.collectAsStateWithLifecycle()
  UserProfileMfaSectionImpl(
    modifier = modifier,
    mfaItems = buildMfaItemList(user?.phoneNumbers.orEmpty()),
    onAdd = onAdd,
  )
}

@Composable
private fun UserProfileMfaSectionImpl(
  mfaItems: ImmutableList<MfaItem>,
  modifier: Modifier = Modifier,
  onAdd: () -> Unit,
) {
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.background)
          .padding(top = dp16)
          .then(modifier)
    ) {
      Text(
        modifier = Modifier.padding(horizontal = dp24),
        text = stringResource(R.string.two_step_verification).uppercase(),
        color = ClerkMaterialTheme.colors.mutedForeground,
        style = ClerkMaterialTheme.typography.bodySmall.withMediumWeight(),
      )
      if (mfaItems.isNotEmpty()) {
        Spacers.Vertical.Spacer24()
      }
      Column(modifier = Modifier.fillMaxWidth().padding(start = dp24)) {
        mfaItems.forEachIndexed { index, mfaItem ->
          UserProfileMfaRow(style = mfaItem.style, isDefault = mfaItem.isDefault)
          if (index < mfaItems.lastIndex) {
            Spacers.Vertical.Spacer16()
          }
        }
      }

      UserProfileButtonRow(
        text = stringResource(R.string.add_two_step_verification),
        onClick = onAdd,
      )
    }
  }
}

private fun buildMfaItemList(phoneNumbers: List<PhoneNumber>): ImmutableList<MfaItem> {
  val mfaPhoneNumbers =
    phoneNumbers
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

data class MfaItem(val style: Style, val isDefault: Boolean = false)

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileMfaSectionImpl(
    mfaItems =
      persistentListOf(
        MfaItem(style = Style.AuthenticatorApp, isDefault = true),
        MfaItem(style = Style.Sms(PhoneNumber(id = "1", "+15555550100")), isDefault = false),
        MfaItem(style = Style.BackupCodes),
      ),
    onAdd = {},
  )
}
