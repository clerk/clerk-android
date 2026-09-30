package com.clerk.snapshot.appbar

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkDesign
import com.clerk.api.ui.ClerkTheme
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.core.appbar.ClerkTopAppBar
import com.clerk.ui.core.composition.LocalClerkLogoContent
import com.clerk.ui.core.extensions.withMediumWeight
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

  @OptIn(ExperimentalCoilApi::class)
  @Test
  fun authTopBar_preservesWideLogoAspectRatio() {
    // Coil cannot decode the SVG data URI under Paparazzi, which silently fell back to the
    // placeholder icon. Serve a deterministic 5:1 bitmap so the wide logo is actually rendered.
    val wideLogo = wideLogoBitmap().asImage()
    paparazzi.snapshot {
      CompositionLocalProvider(
        LocalInspectionMode provides true,
        LocalAsyncImagePreviewHandler provides AsyncImagePreviewHandler { wideLogo },
      ) {
        Box(Modifier.size(width = 360.dp, height = 72.dp)) {
          ClerkMaterialTheme {
            ClerkTopAppBar(onBackPressed = {}, hasLogo = true, hasBackButton = true)
          }
        }
      }
    }
  }

  @Test
  fun authTopBar_respectsLogoMaxHeightOverride() {
    paparazzi.snapshot {
      Box(Modifier.size(width = 360.dp, height = 96.dp)) {
        ClerkMaterialTheme {
          ClerkTopAppBar(
            onBackPressed = {},
            hasLogo = true,
            hasBackButton = true,
            // Placeholder renders at the configured logo height, keeping this deterministic.
            logoUrl = null,
            clerkTheme = ClerkTheme(design = ClerkDesign(logoMaxHeight = 24.dp)),
          )
        }
      }
    }
  }

  @Test
  fun authTopBar_rendersCustomLogoWithoutSdkSizing() {
    paparazzi.snapshot {
      Box(Modifier.size(width = 360.dp, height = 96.dp)) {
        ClerkMaterialTheme {
          CompositionLocalProvider(
            LocalClerkLogoContent provides
              {
                Box(Modifier.size(width = 144.dp, height = 44.dp).background(Color.Red))
              }
          ) {
            ClerkTopAppBar(onBackPressed = {}, hasLogo = true, hasBackButton = true)
          }
        }
      }
    }
  }

  @Test
  fun profileTopBar_placesTrailingActionUsingAndroidSpacing() {
    paparazzi.snapshot {
      Box(Modifier.size(width = 412.dp, height = 72.dp)) {
        ClerkMaterialTheme {
          ClerkTopAppBar(
            onBackPressed = {},
            hasLogo = false,
            title = "Members",
            trailingContent = {
              Text(
                text = "Invite",
                style = ClerkMaterialTheme.typography.bodyLarge.withMediumWeight(),
                color = ClerkMaterialTheme.colors.primary,
              )
            },
          )
        }
      }
    }
  }

  private fun wideLogoBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(240, 48, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    canvas.drawColor(AndroidColor.rgb(0x08, 0x16, 0x3d))
    paint.color = AndroidColor.rgb(0x91, 0xd4, 0x3b)
    canvas.drawRect(12f, 8f, 44f, 40f, paint)
    paint.color = AndroidColor.WHITE
    canvas.drawRect(56f, 12f, 212f, 36f, paint)
    return bitmap
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
