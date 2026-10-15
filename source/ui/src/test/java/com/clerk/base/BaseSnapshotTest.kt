package com.clerk.base

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import com.android.ide.common.rendering.api.SessionParams
import com.android.resources.LayoutDirection
import com.android.resources.NightMode
import com.android.resources.UiMode
import com.clerk.api.Clerk
import org.junit.After
import org.junit.Before
import org.junit.Rule

abstract class BaseSnapshotTest {

  @OptIn(DelicateCoilApi::class)
  @Before
  fun setUp() {
    Clerk.customTheme = null
    SingletonImageLoader.setUnsafe { context ->
      ErrorImageLoader(ImageLoader.Builder(context).build())
    }
  }

  @After
  fun tearDown() {
    Clerk.customTheme = null
  }

  @get:Rule
  val paparazzi =
    Paparazzi(
      deviceConfig =
        DeviceConfig.PIXEL_6_PRO.copy(
          locale = "en",
          fontScale = 1f,
          layoutDirection = LayoutDirection.LTR,
          nightMode = NightMode.NOTNIGHT,
          uiMode = UiMode.NORMAL,
        ),
      theme = "android:Theme.Material.Light.NoActionBar",
      renderingMode = SessionParams.RenderingMode.V_SCROLL,
    )
}
