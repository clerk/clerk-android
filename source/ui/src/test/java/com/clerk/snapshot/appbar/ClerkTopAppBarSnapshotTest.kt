package com.clerk.snapshot.appbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clerk.api.Clerk
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.theme.ClerkMaterialTheme
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test

class ClerkTopAppBarSnapshotTest : BaseSnapshotTest() {

  @Before
  fun setUpLogo() {
    mockkObject(Clerk)
    every { Clerk.organizationLogoUrl } returns WIDE_LOGO_DATA_URI
  }

  @After
  fun tearDownLogo() {
    unmockkAll()
  }

  @Test
  fun authTopBar_preservesWideLogoAspectRatio() {
    paparazzi.snapshot {
      Box(Modifier.size(width = 360.dp, height = 72.dp)) {
        ClerkMaterialTheme {
          ClerkTopAppBar(
            onBackPressed = {},
            title = "Log in to M2X",
            hasLogo = true,
            hasBackButton = true,
          )
        }
      }
    }
  }

  private companion object {
    private const val WIDE_LOGO_DATA_URI =
      "data:image/svg+xml;utf8," +
        "<svg xmlns='http://www.w3.org/2000/svg' width='120' height='24' viewBox='0 0 120 24'>" +
        "<rect width='120' height='24' rx='4' fill='%2308163d'/>" +
        "<rect x='6' y='4' width='16' height='16' rx='2' fill='%2391d43b'/>" +
        "<rect x='28' y='6' width='78' height='12' rx='3' fill='%23ffffff'/>" +
        "</svg>"
  }
}
