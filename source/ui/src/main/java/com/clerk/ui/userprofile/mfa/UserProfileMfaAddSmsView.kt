package com.clerk.ui.userprofile.mfa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.api.user.phoneNumbersAvailableForMfa
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp10
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp6
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.input.CountryCodeUtils
import com.clerk.ui.core.scaffold.ClerkThemedProfileScaffold
import com.clerk.ui.core.spacers.Spacers
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.util.formattedAsPhoneNumberIfPossible
import com.google.i18n.phonenumbers.PhoneNumberUtil
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
fun UserProfileMfaAddSmsView(
  onClickUsePhoneNumber: () -> Unit,
  modifier: Modifier = Modifier,
  onReserveForSecondFactorSuccess: () -> Unit,
) {
  val availablePhoneNumbers =
    remember(Clerk.user) { Clerk.user?.phoneNumbersAvailableForMfa() ?: emptyList() }
      .filter { it.verification?.status == Verification.Status.VERIFIED }
      .sortedBy { it.createdAt }

  UserProfileMfaAddSmsViewImpl(
    modifier = modifier,
    availablePhoneNumbers = availablePhoneNumbers.toImmutableList(),
    onClickUsePhoneNumber = onClickUsePhoneNumber,
    onReserveForSecondFactorSuccess = onReserveForSecondFactorSuccess,
  )
}

@Composable
private fun UserProfileMfaAddSmsViewImpl(
  availablePhoneNumbers: ImmutableList<PhoneNumber>,
  onReserveForSecondFactorSuccess: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: MfaAddSmsViewModel = viewModel(),
  onClickUsePhoneNumber: () -> Unit,
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val phoneUtil = PhoneNumberUtil.getInstance()
  var selectedNumber by remember { mutableStateOf<PhoneNumber?>(null) }
  val context = LocalContext.current
  val errorMessage: String? =
    when (val s = state) {
      is MfaAddSmsViewModel.State.Error ->
        s.message ?: context.getString(R.string.something_went_wrong_please_try_again)
      else -> null
    }

  if (state is MfaAddSmsViewModel.State.Success) {
    onReserveForSecondFactorSuccess()
  }

  ClerkThemedProfileScaffold(
    errorMessage = errorMessage,
    modifier = modifier,
    title = stringResource(R.string.add_sms_code_verification),
  ) {
    Text(
      stringResource(R.string.select_an_existing_phone_number),
      style = ClerkMaterialTheme.typography.bodyMedium,
    )
    Spacers.Vertical.Spacer24()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(dp12)) {
      items(availablePhoneNumbers) { phoneNumber ->
        val proto = phoneUtil.parse(phoneNumber.phoneNumber, null)
        val region = phoneUtil.getRegionCodeForNumber(proto)
        AddMfaSmsRow(
          regionCode = region,
          flag = CountryCodeUtils.regionToFlagEmoji(region),
          phoneNumber = phoneNumber.phoneNumber,
          selected = selectedNumber == phoneNumber,
          onSelected = { selectedNumber = phoneNumber },
        )
      }
    }
    Spacers.Vertical.Spacer24()
    ClerkButton(
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.continue_text),
      isLoading = state is MfaAddSmsViewModel.State.Loading,
      isEnabled = selectedNumber != null && state !is MfaAddSmsViewModel.State.Loading,
      onClick = { viewModel.reserveForSecondFactor(selectedNumber!!) },
    )
    Spacers.Vertical.Spacer24()
    ClerkTextButton(
      text = stringResource(R.string.use_phone_number),
      onClick = onClickUsePhoneNumber,
    )
  }
}

@Composable
fun AddMfaSmsRow(
  flag: String,
  regionCode: String,
  phoneNumber: String,
  selected: Boolean,
  onSelected: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val borderColor =
    if (selected) ClerkMaterialTheme.colors.primary
    else ClerkMaterialTheme.computedColors.inputBorder
  ClerkMaterialTheme {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.input, shape = ClerkMaterialTheme.shape)
          .border(dp1, color = borderColor, shape = ClerkMaterialTheme.shape)
          .padding(horizontal = dp6)
          .padding(vertical = dp8)
          .then(modifier),
      horizontalArrangement = Arrangement.spacedBy(dp8, Alignment.Start),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      CountryIndicator(region = regionCode, flagEmoji = flag)
      Text(
        text = phoneNumber.formattedAsPhoneNumberIfPossible,
        color = ClerkMaterialTheme.colors.foreground,
        style = ClerkMaterialTheme.typography.bodyMedium,
      )
      Spacer(modifier = Modifier.weight(1f))
      RadioButton(selected = selected, onClick = onSelected)
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
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(color = ClerkMaterialTheme.colors.background)
          .padding(dp8),
      verticalArrangement = Arrangement.spacedBy(dp12),
    ) {
      AddMfaSmsRow(
        flag = "🇺🇸",
        regionCode = "US",
        "+13012370655",
        selected = true,
        onSelected = {},
      )
      AddMfaSmsRow(
        flag = "🇺🇸",
        regionCode = "US",
        "+13012370655",
        selected = false,
        onSelected = {},
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileMfaAddSmsViewImpl(
    onClickUsePhoneNumber = {},
    onReserveForSecondFactorSuccess = {},
    availablePhoneNumbers =
      listOf(
          PhoneNumber(id = "1", phoneNumber = "+13012370655"),
          PhoneNumber(id = "2", "+15246462566"),
          PhoneNumber(id = "3", "+306912345678"),
        )
        .toImmutableList(),
  )
}
