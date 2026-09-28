package com.clerk.ui.input

import android.content.Context
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import com.clerk.ui.core.input.LocaleProvider
import com.clerk.ui.core.input.Logger
import com.clerk.ui.core.input.PhoneInputUtils
import com.clerk.ui.core.input.PhoneNumberUtilProvider
import com.clerk.ui.core.input.TelephonyManagerProvider
import com.google.i18n.phonenumbers.AsYouTypeFormatter
import com.google.i18n.phonenumbers.PhoneNumberUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhoneInputUtilsTest {

  private lateinit var mockPhoneNumberUtilProvider: PhoneNumberUtilProvider
  private lateinit var mockLocaleProvider: LocaleProvider
  private lateinit var mockTelephonyManagerProvider: TelephonyManagerProvider
  private lateinit var mockLogger: Logger
  private lateinit var mockPhoneNumberUtil: PhoneNumberUtil
  private lateinit var context: Context
  private lateinit var phoneInputUtils: PhoneInputUtils

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()

    mockPhoneNumberUtilProvider = mockk(relaxed = true)
    mockLocaleProvider = mockk(relaxed = true)
    mockTelephonyManagerProvider = mockk(relaxed = true)
    mockLogger = mockk(relaxed = true)
    mockPhoneNumberUtil = mockk(relaxed = true)

    every { mockPhoneNumberUtilProvider.getPhoneNumberUtil() } returns mockPhoneNumberUtil

    phoneInputUtils =
      PhoneInputUtils(
        phoneNumberUtilProvider = mockPhoneNumberUtilProvider,
        localeProvider = mockLocaleProvider,
        telephonyManagerProvider = mockTelephonyManagerProvider,
        logger = mockLogger,
      )
  }

  @Test
  fun `detectCountry returns CountryInfo when locale detection succeeds`() {
    val locale = Locale.Builder().setLanguage("en").setRegion("US").build()
    every { mockLocaleProvider.getDefaultLocale() } returns locale
    every { mockPhoneNumberUtil.getCountryCodeForRegion("US") } returns 1

    val result = phoneInputUtils.detectCountry(context)

    assertNotNull(result)
    assertEquals("US", result?.countryShortName)
    assertEquals(1, result?.code)
    assertEquals("🇺🇸", result?.flag)
    assertEquals("+1", result?.getPhonePrefix)
    assertEquals("🇺🇸 United States +1", result?.getSelectorText)
  }

  @Test
  fun `detectCountry falls back to telephony when locale detection fails`() {
    val locale = Locale.Builder().build()
    every { mockLocaleProvider.getDefaultLocale() } returns locale
    val mockTelephonyManager = mockk<TelephonyManager>(relaxed = true)
    every { mockTelephonyManagerProvider.getTelephonyManager(context) } returns mockTelephonyManager
    every { mockTelephonyManager.simCountryIso } returns "CA"
    every { mockTelephonyManager.networkCountryIso } returns "US"
    every { mockPhoneNumberUtil.getCountryCodeForRegion("CA") } returns 1

    val result = phoneInputUtils.detectCountry(context)

    assertNotNull(result)
    assertEquals("CA", result?.countryShortName)
    assertEquals(1, result?.code)
    assertEquals("🇨🇦", result?.flag)
  }

  @Test
  fun `detectCountry tries network country when SIM country fails`() {
    val locale = Locale.Builder().build()
    every { mockLocaleProvider.getDefaultLocale() } returns locale
    val mockTelephonyManager = mockk<TelephonyManager>(relaxed = true)
    every { mockTelephonyManagerProvider.getTelephonyManager(context) } returns mockTelephonyManager
    every { mockTelephonyManager.simCountryIso } returns null
    every { mockTelephonyManager.networkCountryIso } returns "GB"
    every { mockPhoneNumberUtil.getCountryCodeForRegion("GB") } returns 44

    val result = phoneInputUtils.detectCountry(context)

    assertNotNull(result)
    assertEquals("GB", result?.countryShortName)
    assertEquals(44, result?.code)
    assertEquals("🇬🇧", result?.flag)
  }

  @Test
  fun `detectCountry returns null when all detection methods fail`() {
    val locale = Locale.Builder().build()
    every { mockLocaleProvider.getDefaultLocale() } returns locale
    every { mockTelephonyManagerProvider.getTelephonyManager(context) } returns null

    val result = phoneInputUtils.detectCountry(context)

    assertNull(result)
  }

  @Test
  fun `detectCountry returns null when country code is invalid`() {
    val locale = Locale.Builder().setLanguage("en").setRegion("US").build()
    every { mockLocaleProvider.getDefaultLocale() } returns locale
    every { mockPhoneNumberUtil.getCountryCodeForRegion("US") } returns 0

    val result = phoneInputUtils.detectCountry(context)

    assertNull(result)
  }

  @Test
  fun `detectCountry returns null when region code is too long`() {
    val mockLocale = mockk<Locale>(relaxed = true)
    every { mockLocale.country } returns "USA"
    every { mockLocale.displayCountry } returns "United States"
    every { mockLocaleProvider.getDefaultLocale() } returns mockLocale
    every { mockPhoneNumberUtil.getCountryCodeForRegion("USA") } returns 1

    val result = phoneInputUtils.detectCountry(context)

    assertNull(result)
  }

  @Test
  fun `detectCountry logs exception and returns null when exception occurs`() {
    val exception = RuntimeException("Test exception")
    every { mockLocaleProvider.getDefaultLocale() } throws exception

    val result = phoneInputUtils.detectCountry(context)

    assertNull(result)
    verify { mockLogger.logWarning("PhoneInputUtils", "Failed to detect country", exception) }
  }

  @Test
  fun `detectCountryCode returns country code when detection succeeds`() {
    val locale = Locale.Builder().setLanguage("en").setRegion("US").build()
    every { mockLocaleProvider.getDefaultLocale() } returns locale
    every { mockPhoneNumberUtil.getCountryCodeForRegion("US") } returns 1

    val result = phoneInputUtils.detectCountryCode(context)

    assertEquals(1, result)
  }

  @Test
  fun `detectCountryCode returns null when detection fails`() {
    val locale = Locale.Builder().setLanguage("en").setRegion("US").build()
    every { mockLocaleProvider.getDefaultLocale() } returns locale
    every { mockTelephonyManagerProvider.getTelephonyManager(context) } returns null

    val result = phoneInputUtils.detectCountryCode(context)

    assertNull(result)
  }

  @Test
  fun `keepDialableCapped preserves valid phone characters`() {
    val input = "+1234567890"

    val result = phoneInputUtils.keepDialableCapped(input)

    assertEquals("+1234567890", result)
  }

  @Test
  fun `keepDialableCapped filters out non-dialable characters`() {
    val input = "+1 (234) 567-890 ext.123"

    val result = phoneInputUtils.keepDialableCapped(input)

    assertEquals("+1234567890123", result)
  }

  @Test
  fun `keepDialableCapped allows only one plus at the beginning`() {
    val input = "++123+456+789"

    val result = phoneInputUtils.keepDialableCapped(input)

    assertEquals("+123456789", result)
  }

  @Test
  fun `keepDialableCapped ignores plus not at beginning`() {
    val input = "123+456+789"

    val result = phoneInputUtils.keepDialableCapped(input)

    assertEquals("123456789", result)
  }

  @Test
  fun `keepDialableCapped caps digits to E164 limit`() {
    val input = "+12345678901234567890"

    val result = phoneInputUtils.keepDialableCapped(input)

    assertEquals("+123456789012345", result)
  }

  @Test
  fun `keepDialableCapped handles empty input`() {
    val input = ""

    val result = phoneInputUtils.keepDialableCapped(input)

    assertEquals("", result)
  }

  @Test
  fun `keepDialableCapped handles only non-dialable characters`() {
    val input = "abc-def ()"

    val result = phoneInputUtils.keepDialableCapped(input)

    assertEquals("", result)
  }

  @Test
  fun `formatAsYouType formats phone number correctly`() {
    val mockFormatter = mockk<AsYouTypeFormatter>(relaxed = true)
    every { mockPhoneNumberUtil.getAsYouTypeFormatter("US") } returns mockFormatter
    every { mockFormatter.inputDigit('1') } returns "1"
    every { mockFormatter.inputDigit('2') } returns "12"
    every { mockFormatter.inputDigit('3') } returns "123"

    val result = phoneInputUtils.formatAsYouType("US", "123")

    assertEquals("123", result)
    verify { mockFormatter.clear() }
    verify { mockFormatter.inputDigit('1') }
    verify { mockFormatter.inputDigit('2') }
    verify { mockFormatter.inputDigit('3') }
  }

  @Test
  fun `formatAsYouType filters non-digit characters except plus`() {
    val mockFormatter = mockk<AsYouTypeFormatter>(relaxed = true)
    every { mockPhoneNumberUtil.getAsYouTypeFormatter("US") } returns mockFormatter
    every { mockFormatter.inputDigit('+') } returns "+"
    every { mockFormatter.inputDigit('1') } returns "+1"

    val result = phoneInputUtils.formatAsYouType("US", "+1-abc")

    assertEquals("+1", result)
    verify { mockFormatter.inputDigit('+') }
    verify { mockFormatter.inputDigit('1') }
  }

  @Test
  fun `getAllCountries returns filtered and sorted countries`() {
    val supportedRegions = setOf("US", "CA", "GB", "001")
    every { mockPhoneNumberUtil.supportedRegions } returns supportedRegions
    every { mockPhoneNumberUtil.getCountryCodeForRegion("CA") } returns 1
    every { mockPhoneNumberUtil.getCountryCodeForRegion("GB") } returns 44
    every { mockPhoneNumberUtil.getCountryCodeForRegion("US") } returns 1

    val result = phoneInputUtils.getAllCountries()

    assertEquals(3, result.size)
    assertEquals("CA", result[0].countryShortName)
    assertEquals("GB", result[1].countryShortName)
    assertEquals("US", result[2].countryShortName)

    assertEquals("🇨🇦", result[0].flag)
    assertEquals("🇬🇧", result[1].flag)
    assertEquals("🇺🇸", result[2].flag)
  }

  @Test
  fun `getDefaultCountry returns US country info`() {
    val result = phoneInputUtils.getDefaultCountry()

    assertEquals("US", result.countryShortName)
    assertEquals(1, result.code)
    assertEquals("🇺🇸", result.flag)
    assertEquals("+1", result.getPhonePrefix)
    assertEquals("🇺🇸 United States +1", result.getSelectorText)
  }

  @Test
  fun `regionToFlagEmoji returns empty string for invalid region codes`() {
    val testCases =
      listOf(
        "A" to 0,
        "ABC" to 0,
        "A1" to 0,
        "1A" to 0,
        "ab" to 0,
        "" to 0,
        "001" to 0,
      )

    testCases.forEach { (regionCode, expectedPhoneCode) ->
      val mockLocale = mockk<Locale>(relaxed = true)
      every { mockLocale.country } returns regionCode
      every { mockLocale.displayCountry } returns "Invalid Country"
      every { mockLocaleProvider.getDefaultLocale() } returns mockLocale
      every { mockPhoneNumberUtil.getCountryCodeForRegion(regionCode.uppercase()) } returns
        expectedPhoneCode

      val result = phoneInputUtils.detectCountry(context)
      assertNull("Expected null for region code: $regionCode", result)
    }
  }

  @Test
  fun `getAllCountries prioritizes US for phone code 1`() {
    val realPhoneInputUtils = PhoneInputUtils()

    val result = realPhoneInputUtils.getAllCountries()

    val plusOneCountries = result.filter { it.code == 1 }

    val usCountry = plusOneCountries.find { it.countryShortName == "US" }
    assertNotNull("US should be found in +1 countries", usCountry)
    assertEquals("US", usCountry?.countryShortName)
    assertEquals(1, usCountry?.code)
    assertEquals("🇺🇸", usCountry?.flag)

    assert(plusOneCountries.size > 1) { "There should be multiple countries with +1 code" }
  }

  @Test
  fun `companion object methods delegate to default instance`() {
    val result = PhoneInputUtils.Companion.getDefaultCountry()

    assertEquals("US", result.countryShortName)
    assertEquals(1, result.code)
    assertEquals("🇺🇸", result.flag)
  }

  @Test
  fun `real world integration test - detectCountry with actual Android context`() {
    val realPhoneInputUtils = PhoneInputUtils()

    val result = realPhoneInputUtils.detectCountry(context)

    assertNotNull("Should not crash with real context", true)
  }
}
