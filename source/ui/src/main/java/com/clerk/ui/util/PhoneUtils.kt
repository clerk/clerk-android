package com.clerk.ui.util

import com.google.i18n.phonenumbers.AsYouTypeFormatter
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

private fun String.nbsp(): String = replace(" ", "\u00A0")

private fun defaultRegion(): String = Locale.getDefault().country.takeIf { it.isNotBlank() } ?: "US"

val String.formattedAsPhoneNumberIfPossible: String
  get() {
    val util = PhoneNumberUtil.getInstance()
    val region = defaultRegion()

    // 1) Try a real parse -> canonical formatting (with '+' prefix)
    try {
      val parsed = util.parse(this, region)
      if (util.isValidNumber(parsed)) {
        val formatted = util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
        return formatted.nbsp()
      }
    } catch (_: NumberParseException) {
      // fall through to partial
    }

    // 2) Fallback: as-you-type (keeps user’s current input vibe)
    val formatter: AsYouTypeFormatter = util.getAsYouTypeFormatter(region)
    var out = ""
    for (c in this) {
      out = formatter.inputDigit(c)
    }
    return out.nbsp()
  }

val String.isPhoneNumber: Boolean
  get() {
    val util = PhoneNumberUtil.getInstance()
    val region = defaultRegion()
    return try {
      val parsed = util.parse(this, region)
      util.isValidNumber(parsed)
    } catch (_: NumberParseException) {
      false
    }
  }
