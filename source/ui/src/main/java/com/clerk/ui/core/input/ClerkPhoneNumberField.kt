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
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
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
import com.clerk.ui.theme.colors.ComputedColors

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
 * The component is fully controlled - the `value` parameter is the single source of truth, and
 * `onValueChange` is called whenever the user modifies the input. The parent component is
 * responsible for managing the phone number state.
 *
 * Example usage:
 * ```kotlin
 * @Composable
 * fun MyPhoneForm() {
 *   var phoneNumber by remember { mutableStateOf("") }
 *   var errorMessage by remember { mutableStateOf<String?>(null) }
 *
 *   ClerkPhoneNumberField(
 *     value = phoneNumber,
 *     onValueChange = { newValue ->
 *       phoneNumber = newValue
 *       // Validate and set error if needed
 *       errorMessage = if (newValue.length < 10) "Invalid phone number" else null
 *     },
 *     errorText = errorMessage
 *   )
 * }
 * ```
 *
 * @param value The complete phone number including country code (e.g., "+1 5551234567")
 * @param onValueChange Callback when the complete phone number changes
 * @param modifier [Modifier] to be applied to the component
 * @param errorText Optional error message to display below the input field
 * @param imeAction IME action to show on the keyboard
 * @param keyboardActions Keyboard action callbacks for IME events
 * @param enabled Whether the phone input and country selector accept user input
 * @param clerkTheme Optional Clerk theme override for this component
 */
@Composable
fun ClerkPhoneNumberField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  errorText: String? = null,
  imeAction: ImeAction = ImeAction.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  enabled: Boolean = true,
  clerkTheme: ClerkTheme? = null,
) {
  ClerkPhoneNumberFieldImpl(
    modifier = modifier,
    errorText = errorText,
    value = value,
    onValueChange = onValueChange,
    imeAction = imeAction,
    keyboardActions = keyboardActions,
    enabled = enabled,
    clerkTheme = clerkTheme,
  )
}

private fun findMatchingCountry(value: String, defaultCountry: CountryInfo): CountryInfo {
  if (value.isEmpty()) return defaultCountry

  val matchingCountries =
    PhoneInputUtils.getAllCountries().filter { country -> value.startsWith(country.getPhonePrefix) }

  return when {
    matchingCountries.isEmpty() -> defaultCountry
    matchingCountries.size == 1 -> matchingCountries.first()
    else -> {
      matchingCountries.find { it.countryShortName == "US" } ?: matchingCountries.first()
    }
  }
}

@Composable
private fun CountryAutoDetectionEffect(
  value: String,
  onCountrySelected: (CountryInfo) -> Unit,
  onValueChange: (String) -> Unit,
) {
  val context = LocalContext.current
  val defaultCountry = PhoneInputUtils.getDefaultCountry()

  LaunchedEffect(value) {
    if (value.isNotEmpty()) {
      val matchingCountry = findMatchingCountry(value, defaultCountry)
      if (matchingCountry != defaultCountry || value.startsWith(matchingCountry.getPhonePrefix)) {
        onCountrySelected(matchingCountry)
      }
    }
  }

  LaunchedEffect(Unit) {
    val detectedCountry = PhoneInputUtils.detectCountry(context)
    if (detectedCountry != null && value.isEmpty()) {
      onCountrySelected(detectedCountry)
      onValueChange(detectedCountry.getPhonePrefix)
    } else if (value.isEmpty()) {
      onCountrySelected(defaultCountry)
      onValueChange(defaultCountry.getPhonePrefix)
    }
  }
}

/**
 * Internal implementation of the Clerk phone number field.
 *
 * This composable handles the state management for country selection and phone number input,
 * including automatic country detection and formatting.
 *
 * @param onValueChange Callback when the complete phone number changes
 * @param value The complete phone number (including country prefix) - this is the source of truth
 * @param modifier [Modifier] to be applied to the component
 * @param errorText Optional error message to display below the input field
 * @param imeAction IME action to show on the keyboard
 * @param keyboardActions Keyboard action callbacks for IME events
 * @param enabled Whether the phone input and country selector accept user input
 * @param clerkTheme Optional Clerk theme override for this component
 */
@Composable
internal fun ClerkPhoneNumberFieldImpl(
  onValueChange: (String) -> Unit,
  value: String,
  modifier: Modifier = Modifier,
  errorText: String? = null,
  imeAction: ImeAction = ImeAction.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  enabled: Boolean = true,
  clerkTheme: ClerkTheme? = null,
) {
  val defaultCountry = PhoneInputUtils.getDefaultCountry()
  val currentCountry = remember(value) { findMatchingCountry(value, defaultCountry) }
  var selectedCountry: CountryInfo by remember { mutableStateOf(currentCountry) }

  CountryAutoDetectionEffect(
    value = value,
    onCountrySelected = { selectedCountry = it },
    onValueChange = onValueChange,
  )

  ClerkMaterialTheme(clerkTheme = clerkTheme) {
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
          enabled = enabled,
          onSelect = { country ->
            val prefix = selectedCountry.getPhonePrefix
            val numberWithoutPrefix =
              when {
                value == prefix -> ""
                value.startsWith("$prefix ") -> value.drop(prefix.length + 1).trim()
                value.startsWith(prefix) -> value.drop(prefix.length).trim()
                else -> value.trim()
              }
            selectedCountry = country
            onValueChange(
              listOf(country.getPhonePrefix, numberWithoutPrefix)
                .filter(String::isNotEmpty)
                .joinToString(" ")
            )
          },
        )
      }

      Column(modifier = Modifier.weight(1f)) {
        PhoneNumberInput(
          computedColors = computedColors,
          value = value,
          onValueChange = onValueChange,
          errorText = errorText,
          countryCode = selectedCountry.countryShortName,
          imeAction = imeAction,
          keyboardActions = keyboardActions,
          enabled = enabled,
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
 * @param value Current complete phone number value (including country prefix)
 * @param onValueChange Callback when phone number changes (receives complete phone number)
 * @param countryCode Selected country code for formatting
 * @param errorText Optional error message to display
 * @param imeAction IME action to show on the keyboard
 * @param keyboardActions Keyboard action callbacks for IME events
 * @param enabled Whether the phone text field accepts user input
 */
@Composable
private fun PhoneNumberInput(
  computedColors: ComputedColors,
  value: String,
  onValueChange: (String) -> Unit,
  countryCode: String,
  errorText: String? = null,
  imeAction: ImeAction = ImeAction.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  enabled: Boolean = true,
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
      onValueChange = { newValue ->
        val filtered = PhoneInputUtils().keepDialableCapped(newValue)
        onValueChange(filtered)
      },
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
          text = stringResource(R.string.enter_your_phone_number),
          style = labelStyle,
          color = labelColor,
        )
      },
      shape = ClerkMaterialTheme.shape,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = imeAction),
      keyboardActions = keyboardActions,
      singleLine = true,
      enabled = enabled,
    )
    PhoneNumberInputError(errorText = errorText)
  }
}

@Composable
private fun PhoneNumberInputError(errorText: String?) {
  errorText?.let {
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
        text = it,
        color = ClerkMaterialTheme.colors.danger,
        style = ClerkMaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
      )
    }
  }
}

@Composable
private fun CountrySelector(
  selectedCountry: CountryInfo,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  onSelect: (CountryInfo) -> Unit,
) {
  var isExpanded by remember { mutableStateOf(false) }

  Box(
    modifier =
      Modifier.padding(top = dp8).heightIn(min = dp56).clickable(enabled = enabled) {
        isExpanded = !isExpanded
      }
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

    if (detectedCountry != null) {
      Text(
        modifier = Modifier.padding(start = dp12),
        text = stringResource(R.string.default_text),
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
      ClerkPhoneNumberField(value = "", onValueChange = {})

      ClerkPhoneNumberField(value = "+1 5551234567", onValueChange = {})

      ClerkPhoneNumberField(value = "+44 20 1234 5678", onValueChange = {})

      ClerkPhoneNumberField(
        value = "+1 555",
        onValueChange = {},
        errorText = "The value entered is in an invalid format. Please check and correct it.",
      )
    }
  }
}
