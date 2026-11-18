package com.clerk.ui.input

import com.clerk.ui.core.input.CountryInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class CountryInfoTest {

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
}
