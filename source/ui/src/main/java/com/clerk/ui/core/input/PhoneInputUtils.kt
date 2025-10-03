package com.clerk.ui.core.input

import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
import com.google.i18n.phonenumbers.AsYouTypeFormatter
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

/** Data class representing country information for phone number formatting. */
internal data class CountryInfo(
  val flag: String,
  val code: Int,
  val countryShortName: String,
  val countryFullName: String,
) {
  val getPhonePrefix: String
    get() = "+$code"

  val getSelectorText: String
    get() = "$flag $countryFullName +$code"
}

internal interface PhoneNumberUtilProvider {
  fun getPhoneNumberUtil(): PhoneNumberUtil
}

internal class DefaultPhoneNumberUtilProvider : PhoneNumberUtilProvider {
  override fun getPhoneNumberUtil(): PhoneNumberUtil = PhoneNumberUtil.getInstance()
}

internal interface LocaleProvider {
  fun getDefaultLocale(): Locale
}

internal class DefaultLocaleProvider : LocaleProvider {
  override fun getDefaultLocale(): Locale = Locale.getDefault()
}

internal interface TelephonyManagerProvider {
  fun getTelephonyManager(context: Context): TelephonyManager?
}

internal class DefaultTelephonyManagerProvider : TelephonyManagerProvider {
  override fun getTelephonyManager(context: Context): TelephonyManager? {
    return context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
  }
}

internal interface Logger {
  fun logWarning(tag: String, message: String, throwable: Throwable? = null)
}

internal class DefaultLogger : Logger {
  override fun logWarning(tag: String, message: String, throwable: Throwable?) {
    Log.w(tag, message, throwable)
  }
}

/**
 * Utility class for phone number input handling and country detection.
 *
 * This class provides functionality to:
 * - Detect country from system locale or telephony information
 * - Format phone numbers as-you-type
 * - Generate country lists with flags
 * - Validate and cap phone number input
 */
internal class PhoneInputUtils(
  private val phoneNumberUtilProvider: PhoneNumberUtilProvider = DefaultPhoneNumberUtilProvider(),
  private val localeProvider: LocaleProvider = DefaultLocaleProvider(),
  private val telephonyManagerProvider: TelephonyManagerProvider =
    DefaultTelephonyManagerProvider(),
  private val logger: Logger = DefaultLogger(),
) {

  companion object {
    private const val REGIONAL_INDICATOR_SYMBOL_A = 0x1F1E6
    private const val FLAG_EMOJI_CODEPOINT_COUNT = 2
    private const val LOG_TAG = "PhoneInputUtils"
    private const val E164_MAX_DIGITS = 15

    // Backward compatibility - default instance
    private val defaultInstance = PhoneInputUtils()

    fun detectCountry(context: Context): CountryInfo? = defaultInstance.detectCountry(context)

    fun getAllCountries(): List<CountryInfo> = defaultInstance.getAllCountries()

    fun getDefaultCountry(): CountryInfo = defaultInstance.getDefaultCountry()
  }

  private val phoneUtil: PhoneNumberUtil by lazy { phoneNumberUtilProvider.getPhoneNumberUtil() }

  /**
   * Detects the country based on system locale first, then falls back to telephony information.
   *
   * @param context Android context for accessing telephony services
   * @return CountryInfo if detection succeeds, null otherwise
   */
  internal fun detectCountry(context: Context): CountryInfo? {
    return try {
      // Try system locale first, then telephony manager for SIM and network country
      detectFromLocale() ?: detectFromTelephony(context)
    } catch (e: Exception) {
      logger.logWarning(LOG_TAG, "Failed to detect country", e)
      null
    }
  }

  /**
   * Filters input to keep only dialable characters (+ and digits) and caps to E164 limit.
   *
   * @param input Raw input string
   * @return Filtered string with only dialable characters, capped to E164 limits
   */
  internal fun keepDialableCapped(input: String): String {
    val out = StringBuilder(input.length)
    var seenPlus = false
    var digits = 0
    input.forEachIndexed { idx, ch ->
      when {
        ch == '+' && !seenPlus && idx == 0 -> {
          out.append(ch)
          seenPlus = true
        }
        ch.isDigit() && digits < E164_MAX_DIGITS -> {
          out.append(ch)
          digits++
        }
      }
    }
    return out.toString()
  }

  /**
   * Formats a phone number as the user types using the specified region.
   *
   * @param regionIso ISO country code for formatting
   * @param raw Raw phone number input
   * @return Formatted phone number string
   */
  internal fun formatAsYouType(regionIso: String, raw: String): String {
    // Keep '+' and digits only; AsYouType handles punctuation
    val filtered =
      buildString(raw.length) { raw.forEach { ch -> if (ch == '+' || ch.isDigit()) append(ch) } }

    val fmt: AsYouTypeFormatter = phoneUtil.getAsYouTypeFormatter(regionIso)
    fmt.clear()
    var out = ""
    filtered.forEach { ch -> out = fmt.inputDigit(ch) }
    return out
  }

  /**
   * Gets all supported countries with their flag emojis and phone codes.
   *
   * @return List of CountryInfo sorted by country short name
   */
  internal fun getAllCountries(): List<CountryInfo> {
    return phoneUtil.supportedRegions
      .filter { it.length == 2 } // Filter out non-standard region codes
      .map { region ->
        CountryInfo(
          flag = regionToFlagEmoji(region),
          code = phoneUtil.getCountryCodeForRegion(region),
          countryShortName = region,
          countryFullName = Locale.Builder().setRegion(region).build().displayCountry,
        )
      }
      .sortedBy { it.countryFullName }
  }

  /**
   * Detects the country code (phone prefix) based on system locale or telephony info.
   *
   * @param context Android context for accessing telephony services
   * @return Country phone code if detection succeeds, null otherwise
   */
  internal fun detectCountryCode(context: Context): Int? {
    return detectCountry(context)?.code
  }

  /**
   * Returns the default country (US) as fallback.
   *
   * @return CountryInfo for United States
   */
  internal fun getDefaultCountry(): CountryInfo {
    return CountryInfo(flag = "🇺🇸", code = 1, countryShortName = "US", "United States")
  }

  private fun detectFromLocale(): CountryInfo? {
    val locale = localeProvider.getDefaultLocale()
    val countryCode = locale.country
    val regionCode = countryCode.uppercase()
    val phoneCode = phoneUtil.getCountryCodeForRegion(regionCode)

    return if (countryCode.isNotEmpty() && regionCode.length == 2 && phoneCode > 0) {
      CountryInfo(
        flag = regionToFlagEmoji(regionCode),
        code = phoneCode,
        countryShortName = regionCode,
        countryFullName = locale.displayCountry,
      )
    } else {
      null
    }
  }

  private fun detectFromTelephony(context: Context): CountryInfo? {
    val telephonyManager = telephonyManagerProvider.getTelephonyManager(context)

    return telephonyManager?.let { tm ->
      // Try SIM country, then network country
      detectFromCountryCode(tm.simCountryIso) ?: detectFromCountryCode(tm.networkCountryIso)
    }
  }

  private fun detectFromCountryCode(countryIso: String?): CountryInfo? {
    val countryCode = countryIso?.uppercase()
    val phoneCode = phoneUtil.getCountryCodeForRegion(countryCode)

    return if (!countryCode.isNullOrEmpty() && countryCode.length == 2 && phoneCode > 0) {
      CountryInfo(
        flag = regionToFlagEmoji(countryCode),
        code = phoneCode,
        countryShortName = countryCode,
        countryFullName = Locale.Builder().setRegion(countryCode).build().displayCountry,
      )
    } else {
      null
    }
  }

  internal fun regionToFlagEmoji(regionCode: String): String {
    val first = if (regionCode.length == 2) regionCode[0].uppercaseChar() else ' '
    val second = if (regionCode.length == 2) regionCode[1].uppercaseChar() else ' '

    fun indicator(c: Char) = REGIONAL_INDICATOR_SYMBOL_A + (c.code - 'A'.code)

    return if (regionCode.length == 2 && first in 'A'..'Z' && second in 'A'..'Z') {
      String(intArrayOf(indicator(first), indicator(second)), 0, FLAG_EMOJI_CODEPOINT_COUNT)
    } else {
      ""
    }
  }
}
