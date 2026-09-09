package com.clerk.base

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.android.resources.LayoutDirection
import com.android.resources.NightMode
import com.android.resources.UiMode
import com.clerk.testing.mockClerk
import com.clerk.ui.core.composition.ClerkProvider
import com.clerk.ui.theme.ClerkTheme
import io.mockk.every
import org.junit.After
import org.junit.Before
import org.junit.Rule

abstract class BaseSnapshotTest {
  protected var snapshotTheme: ClerkTheme? = null
  private val snapshotClerk by lazy {
    mockClerk().also { clerk ->
      every { clerk.environment.displayConfig.branded } returns true
      every { clerk.loaded } returns false
      every { clerk.environment.displayConfig.logoImageUrl } returns ""
    }
  }

  protected fun snapshot(content: @Composable () -> Unit) {
    paparazzi.snapshot {
      ClerkProvider(snapshotClerk, theme = snapshotTheme, content = content)
    }
  }

  @Before
  fun setUp() {
    snapshotTheme = null
  }

  @After
  fun tearDown() {
    snapshotTheme = null
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
