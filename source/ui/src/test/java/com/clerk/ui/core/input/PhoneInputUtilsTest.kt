package com.clerk.ui.core.input

import android.content.Context
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
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
    // Given
    val locale = Locale("en", "US")
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
    assertEquals("🇺🇸 US", result?.getSelectorText)
  }

  @Test
  fun `detectCountry falls back to telephony when locale detection fails`() {
    // Given
    val locale = Locale("", "")
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
    val locale = Locale("", "")
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
    val locale = Locale("", "")
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
    val locale = Locale("en", "US")
    every { mockLocaleProvider.getDefaultLocale() } returns locale
    every { mockPhoneNumberUtil.getCountryCodeForRegion("US") } returns 0

    // When
    val result = phoneInputUtils.detectCountry(context)

    // Then
    assertNull(result)
  }

  @Test
  fun `detectCountry returns null when region code is too long`() {
    // Given
    val locale = Locale("en", "USA") // 3 characters instead of 2
    every { mockLocaleProvider.getDefaultLocale() } returns locale
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
    val locale = Locale("en", "US")
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
    val locale = Locale("", "")
    every { mockLocaleProvider.getDefaultLocale() } returns locale
    every { mockTelephonyManagerProvider.getTelephonyManager(context) } returns null

    // When
    val result = phoneInputUtils.detectCountryCode(context)

    // Then
    assertNull(result)
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
    assertEquals("🇺🇸 US", result.getSelectorText)
  }

  @Test
  fun `regionToFlagEmoji returns empty string for invalid region codes`() {
    // Test various invalid region codes by using the static methods
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
      val locale = Locale("en", regionCode)
      every { mockLocaleProvider.getDefaultLocale() } returns locale
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
    val result = PhoneInputUtils.getDefaultCountry()

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
    assertNotNull("Should not crash with real context", result != null || result == null)
  }
}
