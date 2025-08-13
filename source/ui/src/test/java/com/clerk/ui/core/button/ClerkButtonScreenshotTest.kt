package com.clerk.ui.core.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.ui.R
import com.clerk.ui.colors.DefaultColors
import com.clerk.ui.theme.ClerkMaterialTheme
import org.junit.Rule
import org.junit.Test

class ClerkButtonScreenshotTest {
  @get:Rule
  val paparazzi =
    Paparazzi(
      deviceConfig = DeviceConfig.PIXEL_6_PRO,
      theme = "android:Theme.Material.Light.NoActionBar",
      renderingMode = SessionParams.RenderingMode.V_SCROLL,
    )

  @Test
  fun clerkButtonGallery() {
    paparazzi.snapshot {
      Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
      ClerkMaterialTheme {
        Column(
          modifier =
            Modifier.fillMaxSize()
              .padding(16.dp)
              .background(color = MaterialTheme.colorScheme.background),
          verticalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterVertically),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          ClerkButton(
            text = "Continue",
            onClick = {},
            buttonStyle = ButtonStyle.Primary,
            leadingIcon = R.drawable.ic_triangle_right,
            trailingIcon = R.drawable.ic_triangle_right,
          )

          ClerkButton(
            text = "Continue",
            isEnabled = false,
            onClick = {},
            buttonStyle = ButtonStyle.Primary,
            leadingIcon = R.drawable.ic_triangle_right,
            trailingIcon = R.drawable.ic_triangle_right,
          )

          ClerkButton(
            text = "Continue",
            onClick = {},
            buttonConfig = ClerkButtonConfig(emphasis = ClerkButtonConfig.Emphasis.None),
            buttonStyle = ButtonStyle.Primary,
            leadingIcon = R.drawable.ic_triangle_right,
            trailingIcon = R.drawable.ic_triangle_right,
          )

          ClerkButton(
            text = "Continue",
            isEnabled = false,
            onClick = {},
            buttonConfig = ClerkButtonConfig(emphasis = ClerkButtonConfig.Emphasis.None),
            buttonStyle = ButtonStyle.Primary,
            leadingIcon = R.drawable.ic_triangle_right,
            trailingIcon = R.drawable.ic_triangle_right,
          )

          ClerkButton(
            text = "Continue",
            onClick = {},
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.Low,
                size = ClerkButtonConfig.Size.Small,
              ),
            buttonStyle = ButtonStyle.Primary,
            leadingIcon = R.drawable.ic_triangle_right,
            trailingIcon = R.drawable.ic_triangle_right,
          )

          ClerkButton(
            text = "Continue",
            isEnabled = false,
            onClick = {},
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.Low,
                size = ClerkButtonConfig.Size.Small,
              ),
            buttonStyle = ButtonStyle.Primary,
            leadingIcon = R.drawable.ic_triangle_right,
            trailingIcon = R.drawable.ic_triangle_right,
          )

          ClerkButton(
            text = "Continue",
            onClick = {},
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Small,
              ),
            buttonStyle = ButtonStyle.Primary,
            leadingIcon = R.drawable.ic_triangle_right,
            trailingIcon = R.drawable.ic_triangle_right,
          )

          ClerkButton(
            text = "Continue",
            isEnabled = false,
            onClick = {},
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Small,
              ),
            buttonStyle = ButtonStyle.Primary,
            leadingIcon = R.drawable.ic_triangle_right,
            trailingIcon = R.drawable.ic_triangle_right,
          )

          ClerkButton(
            text = "Continue",
            onClick = {},
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.High,
                size = ClerkButtonConfig.Size.Large,
              ),
            buttonStyle = ButtonStyle.Secondary,
            leadingIcon = R.drawable.ic_triangle_right,
            trailingIcon = R.drawable.ic_triangle_right,
          )

          ClerkButton(
            text = "Continue",
            onClick = {},
            isEnabled = false,
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.High,
                size = ClerkButtonConfig.Size.Large,
              ),
            buttonStyle = ButtonStyle.Secondary,
            leadingIcon = R.drawable.ic_triangle_right,
            trailingIcon = R.drawable.ic_triangle_right,
          )

          ClerkButton(
            text = "Continue",
            onClick = {},
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Large,
              ),
            buttonStyle = ButtonStyle.Secondary,
            leadingIcon = R.drawable.ic_triangle_right,
            trailingIcon = R.drawable.ic_triangle_right,
          )

          ClerkButton(
            text = "Continue",
            onClick = {},
            isEnabled = false,
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Large,
              ),
            buttonStyle = ButtonStyle.Secondary,
            leadingIcon = R.drawable.ic_triangle_right,
            trailingIcon = R.drawable.ic_triangle_right,
          )

          ClerkButton(
            text = "Continue",
            onClick = {},
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.High,
                size = ClerkButtonConfig.Size.Large,
              ),
            buttonStyle = ButtonStyle.Negative,
            leadingIcon = R.drawable.ic_triangle_right,
            trailingIcon = R.drawable.ic_triangle_right,
          )

          ClerkButton(
            text = "Continue",
            onClick = {},
            isEnabled = false,
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.High,
                size = ClerkButtonConfig.Size.Large,
              ),
            buttonStyle = ButtonStyle.Negative,
            leadingIcon = R.drawable.ic_triangle_right,
            trailingIcon = R.drawable.ic_triangle_right,
          )

          ClerkButton(
            text = "Continue",
            onClick = {},
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Large,
              ),
            buttonStyle = ButtonStyle.Negative,
            leadingIcon = R.drawable.ic_triangle_right,
            trailingIcon = R.drawable.ic_triangle_right,
          )

          ClerkButton(
            text = "Continue",
            onClick = {},
            isEnabled = false,
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Large,
              ),
            buttonStyle = ButtonStyle.Negative,
            leadingIcon = R.drawable.ic_triangle_right,
            trailingIcon = R.drawable.ic_triangle_right,
          )
        }
      }
    }
  }
}
