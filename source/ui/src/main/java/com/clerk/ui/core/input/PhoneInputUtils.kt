package com.clerk.ui.core.input

import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

internal data class CountryInfo(val flag: String, val code: Int, val countryShortName: String) {
  val getPhonePrefix: String
    get() = "+$code"

  val getSelectorText: String
    get() = "$flag $countryShortName"
}

internal object PhoneInputUtils {

  private const val REGIONAL_INDICATOR_SYMBOL_A = 0x1F1E6
  private const val FLAG_EMOJI_CODEPOINT_COUNT = 2

  internal fun detectCountry(context: Context): CountryInfo? {
    return try {
      val phoneNumberUtil = PhoneNumberUtil.getInstance()

      // Try system locale first, then telephony manager for SIM and network country
      detectFromLocale(phoneNumberUtil) ?: detectFromTelephony(context, phoneNumberUtil)
    } catch (e: Exception) {
      Log.w("PhoneInputUtils", "Failed to detect country", e)
      null
    }
  }

  private fun detectFromLocale(phoneNumberUtil: PhoneNumberUtil): CountryInfo? {
    val locale = Locale.getDefault()
    val countryCode = locale.country
    val regionCode = countryCode.uppercase()
    val phoneCode = phoneNumberUtil.getCountryCodeForRegion(regionCode)

    return if (countryCode.isNotEmpty() && regionCode.length == 2 && phoneCode > 0) {
      CountryInfo(
        flag = regionToFlagEmoji(regionCode),
        code = phoneCode,
        countryShortName = regionCode,
      )
    } else {
      null
    }
  }

  private fun detectFromTelephony(
    context: Context,
    phoneNumberUtil: PhoneNumberUtil,
  ): CountryInfo? {
    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    return telephonyManager?.let { tm ->
      // Try SIM country, then network country
      detectFromCountryCode(tm.simCountryIso, phoneNumberUtil)
        ?: detectFromCountryCode(tm.networkCountryIso, phoneNumberUtil)
    }
  }

  private fun detectFromCountryCode(
    countryIso: String?,
    phoneNumberUtil: PhoneNumberUtil,
  ): CountryInfo? {
    val countryCode = countryIso?.uppercase()
    val phoneCode = phoneNumberUtil.getCountryCodeForRegion(countryCode)

    return if (!countryCode.isNullOrEmpty() && countryCode.length == 2 && phoneCode > 0) {
      CountryInfo(
        flag = regionToFlagEmoji(countryCode),
        code = phoneCode,
        countryShortName = countryCode,
      )
    } else {
      null
    }
  }

  private fun regionToFlagEmoji(regionCode: String): String {
    val first = if (regionCode.length == 2) regionCode[0].uppercaseChar() else ' '
    val second = if (regionCode.length == 2) regionCode[1].uppercaseChar() else ' '

    fun indicator(c: Char) = REGIONAL_INDICATOR_SYMBOL_A + (c.code - 'A'.code)

    return if (regionCode.length == 2 && first in 'A'..'Z' && second in 'A'..'Z') {
      String(intArrayOf(indicator(first), indicator(second)), 0, FLAG_EMOJI_CODEPOINT_COUNT)
    } else {
      ""
    }
  }

  internal fun getAllCountries(): List<CountryInfo> {
    val phoneNumberUtil = PhoneNumberUtil.getInstance()
    return phoneNumberUtil.supportedRegions
      .filter { it.length == 2 } // Filter out non-standard region codes
      .map { region ->
        CountryInfo(
          flag = regionToFlagEmoji(region),
          code = phoneNumberUtil.getCountryCodeForRegion(region),
          countryShortName = region,
        )
      }
      .sortedBy { it.countryShortName }
  }

  fun detectCountryCode(context: Context): Int? {
    return detectCountry(context)?.code
  }

  internal fun getDefaultCountry(): CountryInfo {
    return CountryInfo(flag = "\uD83C\uDDFA\uD83C\uDDF8", code = 1, countryShortName = "US")
  }
}
