package com.clerk.ui.input

import com.clerk.ui.core.input.CountryInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class CountryInfoTest {

  @Test
  fun `getPhonePrefix returns formatted phone code with plus`() {
    val countryInfo = CountryInfo(flag = "🇺🇸", code = 1, countryShortName = "US", "United States")

    assertEquals("+1", countryInfo.getPhonePrefix)
  }

  @Test
  fun `getPhonePrefix works with multi-digit codes`() {
    val countryInfo =
      CountryInfo(flag = "🇬🇧", code = 44, countryShortName = "GB", "United Kingdom")

    assertEquals("+44", countryInfo.getPhonePrefix)
  }

  @Test
  fun `getSelectorText returns flag and country name`() {
    val countryInfo = CountryInfo(flag = "🇺🇸", code = 1, countryShortName = "US", "United States")

    assertEquals("🇺🇸 United States +1", countryInfo.getSelectorText)
  }

  @Test
  fun `getSelectorText works with different countries`() {
    val countryInfo = CountryInfo(flag = "🇨🇦", code = 1, countryShortName = "CA", "Canada")

    assertEquals("🇨🇦 Canada +1", countryInfo.getSelectorText)
  }

  @Test
  fun `data class equality works correctly`() {
    val country1 = CountryInfo(flag = "🇺🇸", code = 1, countryShortName = "US", "United States")
    val country2 = CountryInfo(flag = "🇺🇸", code = 1, countryShortName = "US", "United States")
    val country3 = CountryInfo(flag = "🇬🇧", code = 44, countryShortName = "GB", "United Kingdom")

    assertEquals(country1, country2)
    assertEquals(country1.hashCode(), country2.hashCode())
    assert(country1 != country3)
  }

  @Test
  fun `toString returns expected format`() {
    val countryInfo = CountryInfo(flag = "🇺🇸", code = 1, countryShortName = "US", "United States")

    val result = countryInfo.toString()

    assert(result.contains("flag=🇺🇸"))
    assert(result.contains("code=1"))
    assert(result.contains("countryShortName=US"))
  }
}
