package com.clerk.snapshot.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
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
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            onClick = {},
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
                leadingIconColor = ClerkMaterialTheme.colors.primaryForeground,
                trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
              ),
          )

          ClerkButtonWithPressedState(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            onClick = {},
            isPressed = true,
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
                leadingIconColor = ClerkMaterialTheme.colors.primaryForeground,
                trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
              ),
          )

          ClerkButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            isEnabled = false,
            onClick = {},
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
                leadingIconColor = ClerkMaterialTheme.colors.primaryForeground,
                trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
              ),
          )

          ClerkButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            onClick = {},
            configuration = ClerkButtonConfig(emphasis = ClerkButtonConfig.Emphasis.None),
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButtonWithPressedState(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            onClick = {},
            isPressed = true,
            configuration = ClerkButtonConfig(emphasis = ClerkButtonConfig.Emphasis.None),
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            isEnabled = false,
            onClick = {},
            configuration = ClerkButtonConfig(emphasis = ClerkButtonConfig.Emphasis.None),
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            onClick = {},
            configuration =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.Low,
                size = ClerkButtonConfig.Size.Small,
              ),
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButtonWithPressedState(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            onClick = {},
            isPressed = true,
            configuration =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.Low,
                size = ClerkButtonConfig.Size.Small,
              ),
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            isEnabled = false,
            onClick = {},
            configuration =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.Low,
                size = ClerkButtonConfig.Size.Small,
              ),
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            onClick = {},
            configuration =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Small,
              ),
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButtonWithPressedState(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            onClick = {},
            isPressed = true,
            configuration =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Small,
              ),
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            isEnabled = false,
            onClick = {},
            configuration =
              ClerkButtonConfig(
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Small,
              ),
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
              ),
          )

          ClerkButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            onClick = {},
            configuration =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Secondary,
                emphasis = ClerkButtonConfig.Emphasis.High,
                size = ClerkButtonConfig.Size.Large,
              ),
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
                trailingIconColor = ClerkMaterialTheme.colors.mutedForeground,
                leadingIconColor = ClerkMaterialTheme.colors.mutedForeground,
              ),
          )

          ClerkButtonWithPressedState(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            onClick = {},
            isPressed = true,
            configuration =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Secondary,
                emphasis = ClerkButtonConfig.Emphasis.High,
                size = ClerkButtonConfig.Size.Large,
              ),
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
                trailingIconColor = ClerkMaterialTheme.colors.mutedForeground,
                leadingIconColor = ClerkMaterialTheme.colors.mutedForeground,
              ),
          )

          ClerkButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            onClick = {},
            isEnabled = false,
            configuration =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Secondary,
                emphasis = ClerkButtonConfig.Emphasis.High,
                size = ClerkButtonConfig.Size.Large,
              ),
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
                trailingIconColor = ClerkMaterialTheme.colors.mutedForeground,
                leadingIconColor = ClerkMaterialTheme.colors.mutedForeground,
              ),
          )

          ClerkButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            onClick = {},
            configuration =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Secondary,
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Large,
              ),
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
                trailingIconColor = ClerkMaterialTheme.colors.mutedForeground,
                leadingIconColor = ClerkMaterialTheme.colors.mutedForeground,
              ),
          )

          ClerkButtonWithPressedState(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            onClick = {},
            isPressed = true,
            configuration =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Secondary,
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Large,
              ),
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
                trailingIconColor = ClerkMaterialTheme.colors.mutedForeground,
                leadingIconColor = ClerkMaterialTheme.colors.mutedForeground,
              ),
          )

          ClerkButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            onClick = {},
            isEnabled = false,
            configuration =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Secondary,
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Large,
              ),
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
                trailingIconColor = ClerkMaterialTheme.colors.mutedForeground,
                leadingIconColor = ClerkMaterialTheme.colors.mutedForeground,
              ),
          )

          ClerkButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            onClick = {},
            configuration =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Negative,
                emphasis = ClerkButtonConfig.Emphasis.High,
                size = ClerkButtonConfig.Size.Large,
              ),
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
                leadingIconColor = ClerkMaterialTheme.colors.primaryForeground,
                trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
              ),
          )

          ClerkButtonWithPressedState(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            onClick = {},
            isPressed = true,
            configuration =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Negative,
                emphasis = ClerkButtonConfig.Emphasis.High,
                size = ClerkButtonConfig.Size.Large,
              ),
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
                leadingIconColor = ClerkMaterialTheme.colors.primaryForeground,
                trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
              ),
          )

          ClerkButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            onClick = {},
            isEnabled = false,
            configuration =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Negative,
                emphasis = ClerkButtonConfig.Emphasis.High,
                size = ClerkButtonConfig.Size.Large,
              ),
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
                leadingIconColor = ClerkMaterialTheme.colors.primaryForeground,
                trailingIconColor = ClerkMaterialTheme.colors.primaryForeground,
              ),
          )

          ClerkButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            onClick = {},
            configuration =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Negative,
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Large,
              ),
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
                leadingIconColor = ClerkMaterialTheme.colors.danger,
                trailingIconColor = ClerkMaterialTheme.colors.danger,
              ),
          )

          ClerkButtonWithPressedState(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            onClick = {},
            isPressed = true,
            configuration =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Negative,
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Large,
              ),
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
                leadingIconColor = ClerkMaterialTheme.colors.danger,
                trailingIconColor = ClerkMaterialTheme.colors.danger,
              ),
          )

          ClerkButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Continue",
            onClick = {},
            isEnabled = false,
            configuration =
              ClerkButtonConfig(
                style = ClerkButtonConfig.ButtonStyle.Negative,
                emphasis = ClerkButtonConfig.Emphasis.None,
                size = ClerkButtonConfig.Size.Large,
              ),
            icons =
              ClerkButtonDefaults.icons(
                leadingIcon = R.drawable.ic_triangle_right,
                trailingIcon = R.drawable.ic_triangle_right,
                leadingIconColor = ClerkMaterialTheme.colors.danger,
                trailingIconColor = ClerkMaterialTheme.colors.danger,
              ),
          )
        }
      }
    }
  }
}
