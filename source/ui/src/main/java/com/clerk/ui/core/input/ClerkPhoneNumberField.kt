package com.clerk.ui.core.input

import androidx.annotation.VisibleForTesting
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
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
import com.clerk.ui.theme.LocalComputedColors

/**
 * Divisor used to calculate the maximum height of the country dropdown menu as a fraction of screen
 * height
 */
private const val DROPDOWN_HEIGHT_DIVISOR = 3

/**
 * A Clerk-styled phone number input field with country selection.
 *
 * This composable provides a complete phone number input experience with:
 * - Country selection dropdown with flag icons
 * - Automatic country detection based on locale
 * - Phone number formatting based on selected country
 * - Error state display
 * - Accessibility support
 *
 * @param modifier [Modifier] to be applied to the component
 * @param errorText Optional error message to display below the input field
 * @param inputText Optional initial phone number text (without country code)
 */
@Composable
fun ClerkPhoneNumberField(
  modifier: Modifier = Modifier,
  errorText: String? = null,
  @VisibleForTesting inputText: String? = null,
) {
  ClerkPhoneNumberFieldImpl(modifier, errorText = errorText, inputText = inputText)
}

/**
 * Creates the initial phone number string by combining country prefix with input text.
 *
 * @param inputText The raw phone number input (may be null or empty)
 * @param country The selected country information
 * @return A formatted phone number string with country prefix
 */
private fun getInitialPhoneNumber(inputText: String?, country: CountryInfo): String {
  return if (inputText.isNullOrEmpty()) {
    country.getPhonePrefix
  } else {
    "${country.getPhonePrefix} $inputText"
  }
}

/**
 * Internal implementation of the Clerk phone number field.
 *
 * This composable handles the state management for country selection and phone number input,
 * including automatic country detection and formatting.
 *
 * @param modifier [Modifier] to be applied to the component
 * @param inputText Optional initial phone number text (without country code)
 * @param errorText Optional error message to display below the input field
 */
@Composable
internal fun ClerkPhoneNumberFieldImpl(
  modifier: Modifier = Modifier,
  inputText: String? = null,
  errorText: String? = null,
) {
  val defaultCountry = PhoneInputUtils.getDefaultCountry()
  var selectedCountry: CountryInfo by remember { mutableStateOf(defaultCountry) }

  val initialPhoneNumber = remember(inputText) { getInitialPhoneNumber(inputText, defaultCountry) }
  var phoneNumber: String by remember { mutableStateOf(initialPhoneNumber) }
  val context = LocalContext.current

  LaunchedEffect(Unit) {
    val detectedCountry = PhoneInputUtils.detectCountry(context)
    if (detectedCountry != null) {
      selectedCountry = detectedCountry
      phoneNumber = getInitialPhoneNumber(inputText, detectedCountry)
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
      verticalAlignment = Alignment.Top,
    ) {
      Column {
        CountrySelector(
          selectedCountry = selectedCountry,
          onSelect = { country ->
            selectedCountry = country
            phoneNumber = getInitialPhoneNumber(inputText, country)
          },
        )
      }

      Column(modifier = Modifier.weight(1f)) {
        PhoneNumberInput(
          computedColors = computedColors,
          value = phoneNumber,
          onValueChange = { phoneNumber = it },
          errorText = errorText,
          countryCode = selectedCountry.countryShortName,
        )
      }
    }
  }
}

/**
 * Phone number input text field with formatting and validation.
 *
 * This composable renders the actual text input field for phone numbers with:
 * - Country-specific formatting via visual transformation
 * - Error state styling
 * - Proper keyboard type and input filtering
 * - Accessibility semantics
 *
 * @param computedColors Theme colors for styling
 * @param value Current phone number value
 * @param onValueChange Callback when phone number changes
 * @param countryCode Selected country code for formatting
 * @param errorText Optional error message to display
 */
@Composable
private fun PhoneNumberInput(
  computedColors: ComputedColors,
  value: String,
  onValueChange: (String) -> Unit,
  countryCode: String,
  errorText: String? = null,
) {

  val interactionSource = remember { MutableInteractionSource() }

  Column {
    OutlinedTextField(
      modifier = Modifier.semantics { contentType = ContentType.PhoneNumber },
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
      isError = errorText != null,
      label = {
        val labelStyle =
          if (value.isNotEmpty()) ClerkMaterialTheme.typography.bodySmall
          else MaterialTheme.typography.bodyMedium
        val labelColor =
          when {
            errorText != null -> MaterialTheme.colorScheme.error
            else -> ClerkMaterialTheme.colors.mutedForeground
          }
        Text(
          stringResource(R.string.enter_your_phone_number),
          style = labelStyle,
          color = labelColor,
        )
      },
      shape = ClerkMaterialTheme.shape,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
      singleLine = true,
    )
    errorText?.let { errorText ->
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = dp8),
        horizontalArrangement = Arrangement.spacedBy(dp4, alignment = Alignment.Start),
        verticalAlignment = Alignment.Top,
      ) {
        Icon(
          painter = painterResource(R.drawable.ic_warning),
          contentDescription = null,
          tint = ClerkMaterialTheme.colors.danger,
        )
        Text(
          text = errorText,
          color = ClerkMaterialTheme.colors.danger,
          style = ClerkMaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
        )
      }
    }
  }
}

/**
 * Country selection component with dropdown menu.
 *
 * This composable provides a clickable country selector that displays:
 * - Selected country flag and code
 * - Expandable dropdown with all available countries
 * - Auto-detected default country at the top
 * - Proper theming and accessibility
 *
 * @param selectedCountry Currently selected country
 * @param modifier [Modifier] to be applied to the component
 * @param onSelect Callback when a country is selected
 */
@Composable
private fun CountrySelector(
  selectedCountry: CountryInfo,
  modifier: Modifier = Modifier,
  onSelect: (CountryInfo) -> Unit,
) {
  var isExpanded by remember { mutableStateOf(false) }

  Box(
    modifier =
      Modifier.padding(top = dp8).heightIn(min = dp56).clickable { isExpanded = !isExpanded }
  ) {
    TextWithIcon(modifier = modifier, selectedCountry = selectedCountry, isExpanded = isExpanded)
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

    CountryDropdownContent(
      isExpanded = isExpanded,
      onDismissRequest = { isExpanded = false },
      onSelect = onSelect,
    )
  }
}

/**
 * Dropdown menu content for country selection.
 *
 * This composable renders the expandable dropdown menu containing:
 * - Auto-detected default country (if available) at the top
 * - Horizontal divider separating default from full list
 * - Complete list of all supported countries
 * - Proper height constraints based on screen size
 *
 * @param isExpanded Whether the dropdown menu is currently expanded
 * @param onDismissRequest Callback to dismiss the dropdown menu
 * @param onSelect Callback when a country is selected from the dropdown
 */
@Composable
private fun CountryDropdownContent(
  isExpanded: Boolean,
  onDismissRequest: () -> Unit,
  onSelect: (CountryInfo) -> Unit,
) {
  val windowInfo = LocalWindowInfo.current
  val density = LocalDensity.current
  val thirdScreenHeightDp =
    with(density) {
      ((windowInfo.containerSize.height / density.density) / DROPDOWN_HEIGHT_DIVISOR).dp
    }
  val context = LocalContext.current

  DropdownMenu(
    isExpanded,
    onDismissRequest = onDismissRequest,
    modifier = Modifier.heightIn(max = thirdScreenHeightDp),
    shape = ClerkMaterialTheme.shape,
  ) {
    val allCountries = PhoneInputUtils.getAllCountries()
    val detectedCountry = PhoneInputUtils.detectCountry(context)

    // Show detected country first if it exists
    if (detectedCountry != null) {
      Text(
        modifier = Modifier.padding(start = dp12),
        text = "Default",
        style = ClerkMaterialTheme.typography.labelSmall,
        color = ClerkMaterialTheme.colors.mutedForeground,
      )
      DropdownMenuItem(
        text = { Text(text = detectedCountry.getSelectorText) },
        onClick = {
          onDismissRequest()
          onSelect(detectedCountry)
        },
      )

      HorizontalDivider(
        modifier = Modifier.padding(horizontal = dp4),
        thickness = dp1,
        color = ClerkMaterialTheme.computedColors.inputBorder,
      )
    }

    // Show all countries
    allCountries.forEach { country ->
      DropdownMenuItem(
        text = { Text(text = country.getSelectorText) },
        onClick = {
          onDismissRequest()
          onSelect(country)
        },
      )
    }
  }
}

/**
 * Country selector display with flag, code, and dropdown icon.
 *
 * This composable renders the clickable country selection button showing:
 * - Country flag emoji
 * - Country code (e.g., "US", "GB")
 * - Chevron down icon
 * - Proper border styling based on focus state
 *
 * @param selectedCountry Currently selected country information
 * @param isExpanded Whether the dropdown is currently expanded (affects border styling)
 * @param modifier [Modifier] to be applied to the component
 */
@Composable
private fun TextWithIcon(
  selectedCountry: CountryInfo,
  isExpanded: Boolean,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      Modifier.background(color = ClerkMaterialTheme.colors.background)
        .border(
          width = dp1,
          color =
            if (isExpanded) ClerkMaterialTheme.colors.primary
            else ClerkMaterialTheme.computedColors.inputBorder,
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
      ClerkPhoneNumberField(
        errorText = "The value entered is in an invalid format. Please check and correct it."
      )
    }
  }
}
