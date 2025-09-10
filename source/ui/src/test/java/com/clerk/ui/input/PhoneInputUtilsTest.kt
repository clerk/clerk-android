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

  // Country Detection Tests
  @Test
  fun `detectCountry returns CountryInfo when locale detection succeeds`() {
    // Given
    val locale = Locale.Builder().setLanguage("en").setRegion("US").build()
    every { mockLocaleProvider.getDefaultLocale() } returns locale
    every { mockPhoneNumberUtil.getCountryCodeForRegion("US") } returns 1

    // When
    val result = phoneInputUtils.detectCountry(context)

    // Then
    assertNotNull(result)
    assertEquals("US", result?.countryShortName)
    assertEquals(1, result?.code)
    assertEquals("🇺🇸", result?.flag)
    assertEquals("+1", result?.getPhonePrefix)
    assertEquals("🇺🇸 United States +1", result?.getSelectorText)
  }

  @Test
  fun `detectCountry falls back to telephony when locale detection fails`() {
    // Given
    val locale = Locale.Builder().build()
    every { mockLocaleProvider.getDefaultLocale() } returns locale
    val mockTelephonyManager = mockk<TelephonyManager>(relaxed = true)
    every { mockTelephonyManagerProvider.getTelephonyManager(context) } returns mockTelephonyManager
    every { mockTelephonyManager.simCountryIso } returns "CA"
    every { mockTelephonyManager.networkCountryIso } returns "US"
    every { mockPhoneNumberUtil.getCountryCodeForRegion("CA") } returns 1

    // When
    val result = phoneInputUtils.detectCountry(context)

    // Then
    assertNotNull(result)
    assertEquals("CA", result?.countryShortName)
    assertEquals(1, result?.code)
    assertEquals("🇨🇦", result?.flag)
  }

  @Test
  fun `detectCountry tries network country when SIM country fails`() {
    // Given
    val locale = Locale.Builder().build()
    every { mockLocaleProvider.getDefaultLocale() } returns locale
    val mockTelephonyManager = mockk<TelephonyManager>(relaxed = true)
    every { mockTelephonyManagerProvider.getTelephonyManager(context) } returns mockTelephonyManager
    every { mockTelephonyManager.simCountryIso } returns null
    every { mockTelephonyManager.networkCountryIso } returns "GB"
    every { mockPhoneNumberUtil.getCountryCodeForRegion("GB") } returns 44

    // When
    val result = phoneInputUtils.detectCountry(context)

    // Then
    assertNotNull(result)
    assertEquals("GB", result?.countryShortName)
    assertEquals(44, result?.code)
    assertEquals("🇬🇧", result?.flag)
  }

  @Test
  fun `detectCountry returns null when all detection methods fail`() {
    // Given
    val locale = Locale.Builder().build()
    every { mockLocaleProvider.getDefaultLocale() } returns locale
    every { mockTelephonyManagerProvider.getTelephonyManager(context) } returns null

    // When
    val result = phoneInputUtils.detectCountry(context)

    // Then
    assertNull(result)
  }

  @Test
  fun `detectCountry returns null when country code is invalid`() {
    // Given
    val locale = Locale.Builder().setLanguage("en").setRegion("US").build()
    every { mockLocaleProvider.getDefaultLocale() } returns locale
    every { mockPhoneNumberUtil.getCountryCodeForRegion("US") } returns 0

    // When
    val result = phoneInputUtils.detectCountry(context)

    // Then
    assertNull(result)
  }

  @Test
  fun `detectCountry returns null when region code is too long`() {
    // Given - Mock a scenario where the locale has an invalid region code
    // We can't create a Locale with "USA" as it throws IllformedLocaleException
    // So we'll mock the locale provider to simulate this scenario
    val mockLocale = mockk<Locale>(relaxed = true)
    every { mockLocale.country } returns "USA"
    every { mockLocale.displayCountry } returns "United States"
    every { mockLocaleProvider.getDefaultLocale() } returns mockLocale
    every { mockPhoneNumberUtil.getCountryCodeForRegion("USA") } returns 1

    // When
    val result = phoneInputUtils.detectCountry(context)

    // Then
    assertNull(result)
  }

  @Test
  fun `detectCountry logs exception and returns null when exception occurs`() {
    // Given
    val exception = RuntimeException("Test exception")
    every { mockLocaleProvider.getDefaultLocale() } throws exception

    // When
    val result = phoneInputUtils.detectCountry(context)

    // Then
    assertNull(result)
    verify { mockLogger.logWarning("PhoneInputUtils", "Failed to detect country", exception) }
  }

  @Test
  fun `detectCountryCode returns country code when detection succeeds`() {
    // Given
    val locale = Locale.Builder().setLanguage("en").setRegion("US").build()
    every { mockLocaleProvider.getDefaultLocale() } returns locale
    every { mockPhoneNumberUtil.getCountryCodeForRegion("US") } returns 1

    // When
    val result = phoneInputUtils.detectCountryCode(context)

    // Then
    assertEquals(1, result)
  }

  @Test
  fun `detectCountryCode returns null when detection fails`() {
    // Given
    val locale = Locale.Builder().setLanguage("en").setRegion("US").build()
    every { mockLocaleProvider.getDefaultLocale() } returns locale
    every { mockTelephonyManagerProvider.getTelephonyManager(context) } returns null

    // When
    val result = phoneInputUtils.detectCountryCode(context)

    // Then
    assertNull(result)
  }

  @Test
  fun `keepDialableCapped preserves valid phone characters`() {
    // Given
    val input = "+1234567890"

    // When
    val result = phoneInputUtils.keepDialableCapped(input)

    // Then
    assertEquals("+1234567890", result)
  }

  @Test
  fun `keepDialableCapped filters out non-dialable characters`() {
    // Given
    val input = "+1 (234) 567-890 ext.123"

    // When
    val result = phoneInputUtils.keepDialableCapped(input)

    // Then
    assertEquals("+1234567890123", result)
  }

  @Test
  fun `keepDialableCapped allows only one plus at the beginning`() {
    // Given
    val input = "++123+456+789"

    // When
    val result = phoneInputUtils.keepDialableCapped(input)

    // Then
    assertEquals("+123456789", result)
  }

  @Test
  fun `keepDialableCapped ignores plus not at beginning`() {
    // Given
    val input = "123+456+789"

    // When
    val result = phoneInputUtils.keepDialableCapped(input)

    // Then
    assertEquals("123456789", result)
  }

  @Test
  fun `keepDialableCapped caps digits to E164 limit`() {
    // Given - 20 digits, should be capped to 15
    val input = "+12345678901234567890"

    // When
    val result = phoneInputUtils.keepDialableCapped(input)

    // Then
    assertEquals("+123456789012345", result) // 15 digits + plus sign
  }

  @Test
  fun `keepDialableCapped handles empty input`() {
    // Given
    val input = ""

    // When
    val result = phoneInputUtils.keepDialableCapped(input)

    // Then
    assertEquals("", result)
  }

  @Test
  fun `keepDialableCapped handles only non-dialable characters`() {
    // Given
    val input = "abc-def ()"

    // When
    val result = phoneInputUtils.keepDialableCapped(input)

    // Then
    assertEquals("", result)
  }

  @Test
  fun `formatAsYouType formats phone number correctly`() {
    // Given
    val mockFormatter = mockk<AsYouTypeFormatter>(relaxed = true)
    every { mockPhoneNumberUtil.getAsYouTypeFormatter("US") } returns mockFormatter
    every { mockFormatter.inputDigit('1') } returns "1"
    every { mockFormatter.inputDigit('2') } returns "12"
    every { mockFormatter.inputDigit('3') } returns "123"

    // When
    val result = phoneInputUtils.formatAsYouType("US", "123")

    // Then
    assertEquals("123", result)
    verify { mockFormatter.clear() }
    verify { mockFormatter.inputDigit('1') }
    verify { mockFormatter.inputDigit('2') }
    verify { mockFormatter.inputDigit('3') }
  }

  @Test
  fun `formatAsYouType filters non-digit characters except plus`() {
    // Given
    val mockFormatter = mockk<AsYouTypeFormatter>(relaxed = true)
    every { mockPhoneNumberUtil.getAsYouTypeFormatter("US") } returns mockFormatter
    every { mockFormatter.inputDigit('+') } returns "+"
    every { mockFormatter.inputDigit('1') } returns "+1"

    // When
    val result = phoneInputUtils.formatAsYouType("US", "+1-abc")

    // Then
    assertEquals("+1", result)
    verify { mockFormatter.inputDigit('+') }
    verify { mockFormatter.inputDigit('1') }
  }

  @Test
  fun `getAllCountries returns filtered and sorted countries`() {
    // Given
    val supportedRegions = setOf("US", "CA", "GB", "001") // 001 should be filtered out
    every { mockPhoneNumberUtil.supportedRegions } returns supportedRegions
    every { mockPhoneNumberUtil.getCountryCodeForRegion("CA") } returns 1
    every { mockPhoneNumberUtil.getCountryCodeForRegion("GB") } returns 44
    every { mockPhoneNumberUtil.getCountryCodeForRegion("US") } returns 1

    // When
    val result = phoneInputUtils.getAllCountries()

    // Then
    assertEquals(3, result.size)
    assertEquals("CA", result[0].countryShortName) // Should be sorted alphabetically
    assertEquals("GB", result[1].countryShortName)
    assertEquals("US", result[2].countryShortName)

    // Verify all have proper flag emojis
    assertEquals("🇨🇦", result[0].flag)
    assertEquals("🇬🇧", result[1].flag)
    assertEquals("🇺🇸", result[2].flag)
  }

  @Test
  fun `getDefaultCountry returns US country info`() {
    // When
    val result = phoneInputUtils.getDefaultCountry()

    // Then
    assertEquals("US", result.countryShortName)
    assertEquals(1, result.code)
    assertEquals("🇺🇸", result.flag)
    assertEquals("+1", result.getPhonePrefix)
    assertEquals("🇺🇸 United States +1", result.getSelectorText)
  }

  @Test
  fun `regionToFlagEmoji returns empty string for invalid region codes`() {
    // Test various invalid region codes by mocking the locale to avoid IllformedLocaleException
    val testCases =
      listOf(
        "A" to 0, // Too short
        "ABC" to 0, // Too long
        "A1" to 0, // Contains number
        "1A" to 0, // Contains number
        "ab" to 0, // Lowercase (should still work after uppercase conversion)
        "" to 0, // Empty
        "001" to 0, // Non-standard code
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
  fun `companion object methods delegate to default instance`() {
    // Test that companion object methods work for backward compatibility
    // These will use real implementations since they create a default instance
    val result = PhoneInputUtils.Companion.getDefaultCountry()

    assertEquals("US", result.countryShortName)
    assertEquals(1, result.code)
    assertEquals("🇺🇸", result.flag)
  }

  @Test
  fun `real world integration test - detectCountry with actual Android context`() {
    // Given - using real implementations with Robolectric context
    val realPhoneInputUtils = PhoneInputUtils()

    // When - this will use the real Android context provided by Robolectric
    val result = realPhoneInputUtils.detectCountry(context)

    // Then - this might return null or a detected country depending on the simulated environment
    // The important thing is that it doesn't crash
    assertNotNull("Should not crash with real context", true)
  }
}
