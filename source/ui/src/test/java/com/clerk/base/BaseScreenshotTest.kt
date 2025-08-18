package com.clerk.base

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import org.junit.Rule

abstract class BaseScreenshotTest {
  @get:Rule
  val paparazzi =
    Paparazzi(
      deviceConfig = DeviceConfig.PIXEL_6_PRO,
      theme = "android:Theme.Material.Light.NoActionBar",
      renderingMode = SessionParams.RenderingMode.V_SCROLL,
    )
}
