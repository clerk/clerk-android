package com.clerk.ui.core.input

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp14
import com.clerk.ui.core.dimens.dp16
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.dimens.dp4
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import com.clerk.ui.theme.LocalClerkDesign
import com.clerk.ui.theme.LocalClerkThemeColors
import com.clerk.ui.theme.LocalComputedColors
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClerkPhoneNumberField(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  value: String? = null,
  onValueChange: (String) -> Unit = {},
  countryCode: String = "US",
) {
  var rawInput by remember { mutableStateOf("") }
  var displayValue by remember { mutableStateOf(value.orEmpty()) }

  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()

  val phoneUtil = PhoneNumberUtil.getInstance()

  fun formatPhoneNumber(input: String, regionCode: String): String {
    if (input.isEmpty()) return ""

    val formatter = phoneUtil.getAsYouTypeFormatter(regionCode)
    formatter.clear()

    // Extract only digits from input
    val digits = input.filter { it.isDigit() }

    var result = ""
    for (digit in digits) {
      result = formatter.inputDigit(digit)
    }
    return result
  }

  fun isValidPhoneNumber(number: String, regionCode: String): Boolean {
    if (number.isEmpty()) return true // Empty is valid (not required to be filled)

    return try {
      val phoneNumber = phoneUtil.parse(number, regionCode)
      phoneUtil.isValidNumber(phoneNumber)
    } catch (e: NumberParseException) {
      false
    }
  }

  ClerkMaterialTheme {
    val colors = LocalClerkThemeColors.current
    val computedColors = LocalComputedColors.current
    val design = LocalClerkDesign.current

    val isValid = isValidPhoneNumber(rawInput, countryCode)

    Row(
      modifier = Modifier.fillMaxWidth().then(modifier),
      horizontalArrangement = Arrangement.spacedBy(dp12),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      CountrySelector(onClick)
      OutlinedTextField(
        colors =
          OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor =
              if (isValid) computedColors.inputBorder else MaterialTheme.colorScheme.error,
            focusedBorderColor =
              if (isFocused || isValid) colors.primary else MaterialTheme.colorScheme.error,
            focusedContainerColor = colors.background,
            unfocusedContainerColor = colors.background,
            errorBorderColor = MaterialTheme.colorScheme.error,
            focusedLabelColor =
              if (isFocused || isValid) colors.primary else MaterialTheme.colorScheme.error,
            unfocusedLabelColor = colors.mutedForeground,
            errorLabelColor = MaterialTheme.colorScheme.error,
          ),
        shape = RoundedCornerShape(design.borderRadius),
        interactionSource = interactionSource,
        modifier = Modifier.weight(1f),
        value = displayValue,
        onValueChange = { newValue ->
          // Remove all non-digit characters to get raw input
          val newRawInput = newValue.filter { it.isDigit() }

          // Only update if the raw digits actually changed
          if (newRawInput != rawInput) {
            rawInput = newRawInput
            displayValue = formatPhoneNumber(newRawInput, countryCode)
            onValueChange(rawInput)
          }
        },
        label = { Text("Enter your phone number", style = MaterialTheme.typography.bodyMedium) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        isError = !isValid,
        supportingText =
          if (!isValid && rawInput.isNotEmpty()) {
            {
              Text(
                "Please enter a valid phone number",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
              )
            }
          } else null,
      )
    }
  }
}

@Composable
private fun CountrySelector(onClick: () -> Unit, modifier: Modifier = Modifier) {
  val colors = LocalClerkThemeColors.current
  val computedColors = LocalComputedColors.current
  val design = LocalClerkDesign.current

  Box(modifier = Modifier.padding(top = dp8).heightIn(min = 56.dp).clickable { onClick() }) {
    Row(
      modifier =
        Modifier.background(color = colors.background)
          .border(
            width = dp1,
            color = computedColors.inputBorder,
            shape = RoundedCornerShape(design.borderRadius),
          )
          .padding(dp16)
          .then(modifier),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Text("\uD83C\uDDFA\uD83C\uDDF8")
      Spacer(modifier = Modifier.width(dp8))
      Text("US", style = MaterialTheme.typography.bodyLarge, color = colors.foreground)
      Spacer(modifier = Modifier.width(dp14))
      Icon(
        modifier = Modifier.size(dp24),
        painter = painterResource(R.drawable.ic_chevron_down),
        contentDescription = null,
        tint = colors.mutedForeground,
      )
    }
    Text(
      modifier =
        Modifier.align(Alignment.TopStart)
          .padding(horizontal = dp12)
          .offset(y = (-7).dp)
          .background(color = colors.background)
          .padding(horizontal = dp4)
          .zIndex(1f),
      text = stringResource(R.string.country),
      color = colors.mutedForeground,
      style = MaterialTheme.typography.bodySmall,
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
      ClerkPhoneNumberField(
        value = "3012370655",
        onValueChange = { number -> println("Phone number digits: $number") },
        onClick = {},
      )
      ClerkPhoneNumberField(
        onValueChange = { number -> println("Phone number digits: $number") },
        onClick = {},
      )
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
      ClerkPhoneNumberField(
        value = "3012370655",
        onValueChange = { number -> println("Phone number digits: $number") },
        onClick = {},
      )
      ClerkPhoneNumberField(
        onValueChange = { number -> println("Phone number digits: $number") },
        onClick = {},
      )
    }
  }
}
