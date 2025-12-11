package com.clerk.ui.input

import com.clerk.ui.core.input.CountryInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class CountryInfoTest {

  @Test
  fun `getPhonePrefix returns formatted phone code with plus`() {
    // Given
    val countryInfo = CountryInfo(flag = "🇺🇸", code = 1, countryShortName = "US", "United States")

    // When & Then
    assertEquals("+1", countryInfo.getPhonePrefix)
  }

  @Test
  fun `getPhonePrefix works with multi-digit codes`() {
    // Given
    val countryInfo =
      CountryInfo(flag = "🇬🇧", code = 44, countryShortName = "GB", "United Kingdom")

    // When & Then
    assertEquals("+44", countryInfo.getPhonePrefix)
  }

  @Test
  fun `getSelectorText returns flag and country name`() {
    // Given
    val countryInfo = CountryInfo(flag = "🇺🇸", code = 1, countryShortName = "US", "United States")

    // When & Then
    assertEquals("🇺🇸 United States +1", countryInfo.getSelectorText)
  }

  @Test
  fun `getSelectorText works with different countries`() {
    // Given
    val countryInfo = CountryInfo(flag = "🇨🇦", code = 1, countryShortName = "CA", "Canada")

    // When & Then
    assertEquals("🇨🇦 Canada +1", countryInfo.getSelectorText)
  }

  @Test
  fun `data class equality works correctly`() {
    // Given
    val country1 = CountryInfo(flag = "🇺🇸", code = 1, countryShortName = "US", "United States")
    val country2 = CountryInfo(flag = "🇺🇸", code = 1, countryShortName = "US", "United States")
    val country3 = CountryInfo(flag = "🇬🇧", code = 44, countryShortName = "GB", "United Kingdom")

    // When & Then
    assertEquals(country1, country2)
    assertEquals(country1.hashCode(), country2.hashCode())
    assert(country1 != country3)
  }

  @Test
  fun `toString returns expected format`() {
    // Given
    val countryInfo = CountryInfo(flag = "🇺🇸", code = 1, countryShortName = "US", "United States")

    // When
    val result = countryInfo.toString()

    // Then
    assert(result.contains("flag=🇺🇸"))
    assert(result.contains("code=1"))
    assert(result.contains("countryShortName=US"))
  }
}
