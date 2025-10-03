package com.clerk.ui.userprofile.mfa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.clerk.api.Clerk
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.user.phoneNumbersAvailableForMfa
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp10
import com.clerk.ui.core.dimens.dp6
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.input.CountryCodeUtils
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.google.i18n.phonenumbers.PhoneNumberUtil

@Composable
fun UserProfileMfaAddSmsView(modifier: Modifier = Modifier) {
  UserProfileMfaAddSmsViewImpl(modifier = modifier)
}

@Composable
fun UserProfileMfaAddSmsViewImpl(modifier: Modifier = Modifier) {
  val phoneUtil = PhoneNumberUtil.getInstance()

  val availablePhoneNumbers =
    remember(Clerk.user) { Clerk.user?.phoneNumbersAvailableForMfa() ?: emptyList() }
      .filter { it.verification?.status == Verification.Status.VERIFIED }
      .sortedBy { it.createdAt }

  ClerkThemedProfileScaffold(
    modifier = modifier,
    title = stringResource(R.string.add_sms_code_verification),
  ) {
    Text(
      stringResource(R.string.select_an_existing_phone_number),
      style = ClerkMaterialTheme.typography.bodyMedium,
    )
    Spacers.Vertical.Spacer24()
    LazyColumn {
      items(availablePhoneNumbers) { phoneNumber ->
        val proto = phoneUtil.parse(phoneNumber.phoneNumber, null)
        val region = phoneUtil.getRegionCodeForNumber(proto)
        AddMfaSmsRow(regionCode = region, flag = CountryCodeUtils.regionToFlagEmoji(region))
      }
    }
  }
}

@Composable
fun AddMfaSmsRow(flag: String, regionCode: String, modifier: Modifier = Modifier) {
  ClerkMaterialTheme {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.input, shape = ClerkMaterialTheme.shape)
          .border(
            dp1,
            color = ClerkMaterialTheme.computedColors.inputBorder,
            shape = ClerkMaterialTheme.shape,
          )
          .padding(horizontal = dp6)
          .padding(vertical = dp8)
          .then(modifier),
      horizontalArrangement = Arrangement.Start,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      CountryIndicator(region = regionCode, flagEmoji = flag)
    }
  }
}

@Composable
fun CountryIndicator(region: String, flagEmoji: String, modifier: Modifier = Modifier) {
  Box(
    modifier =
      Modifier.height(44.dp)
        .wrapContentWidth()
        .background(color = ClerkMaterialTheme.colors.muted, shape = ClerkMaterialTheme.shape)
        .then(modifier)
  ) {
    Row(
      modifier = Modifier.padding(dp10).align(Alignment.Center),
      horizontalArrangement = Arrangement.spacedBy(dp6),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(text = flagEmoji)
      Text(region, color = ClerkMaterialTheme.colors.foreground)
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewMfaRow() {
  AddMfaSmsRow(flag = "🇺🇸", regionCode = "US")
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileMfaAddSmsView()
}
