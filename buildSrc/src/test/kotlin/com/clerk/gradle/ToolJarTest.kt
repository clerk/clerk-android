package com.clerk.gradle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToolJarTest {
  @Test
  fun `tells pre-releases from releases`() {
    listOf("2.0.0-alpha.6", "0.65-rc1", "1.0.0-beta01", "2.10.0-M1", "1.0-SNAPSHOT", "1.0.0-dev-12")
      .forEach { assertTrue(isPreRelease(it), it) }
    listOf("0.64", "2.0.0", "1.23.8", "0.21.0", "1.0-mod").forEach {
      assertFalse(isPreRelease(it), it)
    }
  }

  @Test
  fun `finds the cache directory the way tool-jar sh does`() {
    val home = mapOf("HOME" to "/home/me")

    assertEquals("/home/me/.cache/clerk-android/tools", toolJarCacheDirectory(home::get))
    assertEquals(
      "/xdg/clerk-android/tools",
      toolJarCacheDirectory((home + ("XDG_CACHE_HOME" to "/xdg"))::get),
    )
    assertEquals("/tools", toolJarCacheDirectory((home + ("CLERK_TOOLS_CACHE" to "/tools"))::get))
    assertEquals(
      "/home/me/.cache/clerk-android/tools",
      toolJarCacheDirectory((home + ("CLERK_TOOLS_CACHE" to "") + ("XDG_CACHE_HOME" to ""))::get),
    )
  }
}
