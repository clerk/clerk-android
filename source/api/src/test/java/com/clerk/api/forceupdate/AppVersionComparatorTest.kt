package com.clerk.api.forceupdate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionComparatorTest {
  @Test
  fun `1 dot 2 equals 1 dot 2 dot 0`() {
    assertEquals(0, AppVersionComparator.compare("1.2", "1.2.0"))
  }

  @Test
  fun `1 dot 10 is greater than 1 dot 2`() {
    assertTrue((AppVersionComparator.compare("1.10", "1.2") ?: 0) > 0)
  }

  @Test
  fun `invalid versions are rejected`() {
    assertNull(AppVersionComparator.compare("1.0-beta", "1.0"))
    assertNull(AppVersionComparator.compare("1..0", "1.0"))
    assertNull(AppVersionComparator.compare("v1.0", "1.0"))
  }
}
