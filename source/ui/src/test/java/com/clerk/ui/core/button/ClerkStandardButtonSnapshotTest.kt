package com.clerk.ui.core.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonConfig
import com.clerk.ui.core.button.standard.ClerkButtonIconConfig
import com.clerk.ui.core.button.standard.ClerkButtonWithPressedState
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import org.junit.Test

class ClerkStandardButtonSnapshotTest : BaseSnapshotTest() {

  @Test
  fun clerkButtonGallery() {
    paparazzi.snapshot {
      Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
      ClerkMaterialTheme {
        Column(
          modifier =
            Modifier.fillMaxSize()
              .padding(16.dp)
              .background(color = ClerkMaterialTheme.colors.background),
          verticalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterVertically),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          ClerkButton(
            text = "Continue",
            onClick = {},
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButtonWithPressedState(
            text = "Continue",
            onClick = {},
            isPressed = true,
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButton(
            text = "Continue",
            isEnabled = false,
            onClick = {},
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButton(
            text = "Continue",
            onClick = {},
            buttonConfig = ClerkButtonConfig(emphasis = ClerkButtonConfig.Emphasis.None),
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButtonWithPressedState(
            text = "Continue",
            onClick = {},
            isPressed = true,
            buttonConfig = ClerkButtonConfig(emphasis = ClerkButtonConfig.Emphasis.None),
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButton(
            text = "Continue",
            isEnabled = false,
            onClick = {},
            buttonConfig = ClerkButtonConfig(emphasis = ClerkButtonConfig.Emphasis.None),
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButton(
            text = "Continue",
            onClick = {},
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.Low,
                size = ClerkButtonConfig.Size.Small,
              ),
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButtonWithPressedState(
            text = "Continue",
            onClick = {},
            isPressed = true,
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.Low,
                size = ClerkButtonConfig.Size.Small,
              ),
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
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
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButton(
            text = "Continue",
            onClick = {},
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Small,
              ),
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButtonWithPressedState(
            text = "Continue",
            onClick = {},
            isPressed = true,
            buttonConfig =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Small,
              ),
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
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
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButton(
            text = "Continue",
            onClick = {},
            buttonConfig =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Secondary,
                emphasis = ClerkButtonConfig.Emphasis.High,
                size = ClerkButtonConfig.Size.Large,
              ),
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButtonWithPressedState(
            text = "Continue",
            onClick = {},
            isPressed = true,
            buttonConfig =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Secondary,
                emphasis = ClerkButtonConfig.Emphasis.High,
                size = ClerkButtonConfig.Size.Large,
              ),
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButton(
            text = "Continue",
            onClick = {},
            isEnabled = false,
            buttonConfig =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Secondary,
                emphasis = ClerkButtonConfig.Emphasis.High,
                size = ClerkButtonConfig.Size.Large,
              ),
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButton(
            text = "Continue",
            onClick = {},
            buttonConfig =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Secondary,
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Large,
              ),
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButtonWithPressedState(
            text = "Continue",
            onClick = {},
            isPressed = true,
            buttonConfig =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Secondary,
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Large,
              ),
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButton(
            text = "Continue",
            onClick = {},
            isEnabled = false,
            buttonConfig =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Secondary,
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Large,
              ),
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButton(
            text = "Continue",
            onClick = {},
            buttonConfig =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Negative,
                emphasis = ClerkButtonConfig.Emphasis.High,
                size = ClerkButtonConfig.Size.Large,
              ),
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButtonWithPressedState(
            text = "Continue",
            onClick = {},
            isPressed = true,
            buttonConfig =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Negative,
                emphasis = ClerkButtonConfig.Emphasis.High,
                size = ClerkButtonConfig.Size.Large,
              ),
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButton(
            text = "Continue",
            onClick = {},
            isEnabled = false,
            buttonConfig =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Negative,
                emphasis = ClerkButtonConfig.Emphasis.High,
                size = ClerkButtonConfig.Size.Large,
              ),
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButton(
            text = "Continue",
            onClick = {},
            buttonConfig =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Negative,
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Large,
              ),
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButtonWithPressedState(
            text = "Continue",
            onClick = {},
            isPressed = true,
            buttonConfig =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Negative,
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Large,
              ),
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButton(
            text = "Continue",
            onClick = {},
            isEnabled = false,
            buttonConfig =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Negative,
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Large,
              ),
            iconConfig =
              ClerkButtonIconConfig(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )
        }
      }
    }
  }
}
