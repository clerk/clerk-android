package com.clerk.ui.core.input

import com.google.i18n.phonenumbers.AsYouTypeFormatter
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

data class Country(
  val region: String, // "US"
  val dialCode: String, // "+1"
  val name: String, // "United States"
  val emoji: String, // 🇺🇸
)

internal val phoneUtil by lazy { PhoneNumberUtil.getInstance() }

fun loadCountries(locale: Locale = Locale.getDefault()): ImmutableList<Country> {
  return phoneUtil.supportedRegions
    .sorted()
    .map { region ->
      val cc = phoneUtil.getCountryCodeForRegion(region)
      val name = Locale("", region).getDisplayCountry(locale)
      Country(region, "+$cc", name, regionToFlag(region))
    }
    .toImmutableList()
}

private fun regionToFlag(region: String): String {
  // “A” → 🇦 (127462), “Z” → 🇿 (127487)
  return region
    .uppercase(Locale.US)
    .map { 0x1F1E6 - 'A'.code + it.code }
    .map { Character.toChars(it) }
    .joinToString(separator = "") { String(it) }
}

internal fun formatNational(region: String, digits: String): String {
  val fmt: AsYouTypeFormatter = phoneUtil.getAsYouTypeFormatter(region)
  var out = ""
  digits.forEach { out = fmt.inputDigit(it) }
  return out
}

internal fun onlyDigits(s: String) = s.filter { it.isDigit() }
