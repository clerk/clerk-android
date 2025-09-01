package com.clerk.ui.core.input

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.colors.ComputedColors
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp14
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp4
import com.clerk.ui.core.dimens.dp56
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import com.clerk.ui.theme.LocalComputedColors

@Composable
fun ClerkPhoneNumberField(modifier: Modifier = Modifier) {
  val defaultCountry = PhoneInputUtils.getDefaultCountry()
  var selectedCountry: CountryInfo by remember { mutableStateOf(defaultCountry) }
  var phoneNumber: String by remember { mutableStateOf(selectedCountry.getPhonePrefix) }
  val context = LocalContext.current

  LaunchedEffect(Unit) {
    val detectedCountry = PhoneInputUtils.detectCountry(context)
    if (detectedCountry != null) {
      selectedCountry = detectedCountry
      phoneNumber = selectedCountry.getPhonePrefix
    }
  }

  ClerkMaterialTheme {
    val computedColors = LocalComputedColors.current

    Row(
      modifier =
        Modifier.background(ClerkMaterialTheme.colors.background, shape = ClerkMaterialTheme.shape)
          .padding(horizontal = dp4)
          .padding(bottom = dp4)
          .fillMaxWidth()
          .then(modifier),
      horizontalArrangement = Arrangement.spacedBy(dp12),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      CountrySelector(
        selectedCountry = selectedCountry,
        onSelect = { country ->
          selectedCountry = country
          // Reset phone number when country changes, keeping only the country code
          phoneNumber = country.getPhonePrefix
        },
      )

      PhoneNumberInput(
        computedColors = computedColors,
        value = phoneNumber,
        onValueChange = { phoneNumber = it },
        countryCode = selectedCountry.countryShortName,
      )
    }
  }
}

@Composable
private fun PhoneNumberInput(
  computedColors: ComputedColors,
  value: String,
  onValueChange: (String) -> Unit,
  countryCode: String,
) {

  val interactionSource = remember { MutableInteractionSource() }

  OutlinedTextField(
    colors =
      OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = computedColors.inputBorder,
        focusedBorderColor = ClerkMaterialTheme.colors.primary,
        focusedContainerColor = ClerkMaterialTheme.colors.background,
        unfocusedContainerColor = ClerkMaterialTheme.colors.background,
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedLabelColor = ClerkMaterialTheme.colors.primary,
        unfocusedLabelColor = ClerkMaterialTheme.colors.mutedForeground,
        errorLabelColor = MaterialTheme.colorScheme.error,
      ),
    interactionSource = interactionSource,
    value = value,
    onValueChange = { onValueChange(PhoneInputUtils().keepDialableCapped(it)) },
    visualTransformation = phoneVisualTransformation(countryCode),
    label = {
      Text(
        stringResource(R.string.enter_your_phone_number),
        style = MaterialTheme.typography.bodyMedium,
      )
    },
    shape = ClerkMaterialTheme.shape,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
    singleLine = true,
  )
}

@Composable
private fun CountrySelector(
  selectedCountry: CountryInfo,
  modifier: Modifier = Modifier,
  onSelect: (CountryInfo) -> Unit,
) {
  var isExpanded by remember { mutableStateOf(false) }
  val context = LocalContext.current

  Box(
    modifier =
      Modifier.padding(top = dp8).heightIn(min = dp56).clickable { isExpanded = !isExpanded }
  ) {
    TextWithIcon(modifier = modifier, selectedCountry = selectedCountry)
    Text(
      modifier =
        Modifier.align(Alignment.TopStart)
          .padding(horizontal = dp12)
          .offset(y = (-7).dp)
          .background(color = ClerkMaterialTheme.colors.background)
          .padding(horizontal = dp4)
          .zIndex(1f),
      text = stringResource(R.string.country),
      color = ClerkMaterialTheme.colors.mutedForeground,
      style = MaterialTheme.typography.bodySmall,
    )
    DropdownMenu(isExpanded, onDismissRequest = { isExpanded = false }) {
      val allCountries = PhoneInputUtils.getAllCountries()

      // Get detected country from system
      val detectedCountry = PhoneInputUtils.detectCountry(context)

      // Show detected country first if it exists and is different from current selection
      if (detectedCountry != null) {
        DropdownMenuItem(
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(text = detectedCountry.getSelectorText)
            }
          },
          onClick = {
            isExpanded = false
            onSelect(detectedCountry)
          },
        )

        HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.inputBorder)

        // Show all countries
        allCountries.forEach { country ->
          DropdownMenuItem(
            text = { Text(text = country.getSelectorText) },
            onClick = {
              isExpanded = false
              onSelect(country)
            },
          )
        }
      }
    }
  }
}

@Composable
private fun TextWithIcon(selectedCountry: CountryInfo, modifier: Modifier = Modifier) {
  Row(
    modifier =
      Modifier.background(color = ClerkMaterialTheme.colors.background)
        .border(
          width = dp1,
          color = ClerkMaterialTheme.computedColors.inputBorder,
          shape = ClerkMaterialTheme.shape,
        )
        .padding(dp16)
        .then(modifier),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    Text(selectedCountry.flag)
    Spacer(modifier = Modifier.width(dp8))
    Text(
      selectedCountry.countryShortName,
      style = MaterialTheme.typography.bodyLarge,
      color = ClerkMaterialTheme.colors.foreground,
    )
    Spacer(modifier = Modifier.width(dp14))
    Icon(
      modifier = Modifier.size(dp24),
      painter = painterResource(R.drawable.ic_chevron_down),
      contentDescription = null,
      tint = ClerkMaterialTheme.colors.mutedForeground,
    )
  }
}

@PreviewLightDark
@Composable
private fun PreviewPhoneInput() {
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.background(color = MaterialTheme.colorScheme.background)
          .fillMaxWidth()
          .padding(dp12),
      verticalArrangement = Arrangement.spacedBy(dp12),
    ) {
      ClerkPhoneNumberField()
      ClerkPhoneNumberField()
    }
  }
}

@Preview
@Composable
private fun PreviewPhoneInputClerkTheme() {
  Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
  ClerkMaterialTheme {
    Column(
      modifier =
        Modifier.background(color = MaterialTheme.colorScheme.background)
          .fillMaxWidth()
          .padding(dp12),
      verticalArrangement = Arrangement.spacedBy(dp12),
    ) {
      ClerkPhoneNumberField()
      ClerkPhoneNumberField()
    }
  }
}
