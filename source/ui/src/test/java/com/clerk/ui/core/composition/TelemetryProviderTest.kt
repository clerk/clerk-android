package com.clerk.ui.core.composition

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.clerk.telemetry.TelemetryCollector
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TelemetryProviderTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun siblingAndNestedProvidersShareOneCollector() {
    val provided = mutableListOf<TelemetryCollector>()

    composeTestRule.setContent {
      TelemetryProvider { provided += LocalTelemetryCollector.current }
      TelemetryProvider { TelemetryProvider { provided += LocalTelemetryCollector.current } }
    }
    composeTestRule.waitForIdle()

    assertEquals(2, provided.size)
    assertEquals(1, provided.distinct().size)
  }

  @Test
  fun providerReenteringCompositionReusesCollector() {
    val provided = mutableListOf<TelemetryCollector>()
    var showProvider by mutableStateOf(true)

    composeTestRule.setContent {
      if (showProvider) TelemetryProvider { provided += LocalTelemetryCollector.current }
    }
    composeTestRule.runOnUiThread { showProvider = false }
    composeTestRule.waitForIdle()
    composeTestRule.runOnUiThread { showProvider = true }
    composeTestRule.waitForIdle()

    assertEquals(2, provided.size)
    assertEquals(1, provided.distinct().size)
  }
}
