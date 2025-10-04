package com.clerk.snapshot.input

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp20
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.theme.ClerkMaterialTheme
import org.junit.Test

class ClerkTextFieldSnapshotTest : BaseSnapshotTest() {

  @Test
  fun testClerkTextFieldUnfocused() {
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Column(
          modifier =
            Modifier.background(color = ClerkMaterialTheme.colors.background).padding(dp12),
          verticalArrangement = Arrangement.spacedBy(dp20, alignment = Alignment.CenterVertically),
        ) {
          ClerkTextField(
            onValueChange = {},
            value = "Input",
            label = "Label",
            trailingIcon = R.drawable.ic_cross,
            supportingText = "Supporting text",
          )

          ClerkTextField(
            onValueChange = {},
            value = "",
            label = "Label",
            trailingIcon = R.drawable.ic_cross,
            supportingText = "Supporting text",
          )

          ClerkTextField(
            onValueChange = {},
            value = "Input",
            placeholder = "Placeholder",
            label = "Label",
            supportingText = "Supporting text",
          )

          ClerkTextField(
            onValueChange = {},
            value = "",
            label = "Label",
            supportingText = "Supporting text",
            leadingIcon = R.drawable.ic_search,
          )

          ClerkTextField(
            onValueChange = {},
            value = "Input",
            label = "Label",
            trailingIcon = R.drawable.ic_cross,
            leadingIcon = R.drawable.ic_search,
            supportingText = "Supporting text",
          )

          ClerkTextField(
            onValueChange = {},
            value = "",
            label = "Label",
            trailingIcon = R.drawable.ic_cross,
            leadingIcon = R.drawable.ic_search,
            supportingText = "Supporting text",
          )

          ClerkTextField(
            onValueChange = {},
            value = "Input",
            label = "Label",
            supportingText = "Supporting text",
          )
        }
      }
    }
  }

  @Test
  fun testClerkTextFieldError() {
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Column(
          modifier =
            Modifier.background(color = ClerkMaterialTheme.colors.background).padding(dp12),
          verticalArrangement = Arrangement.spacedBy(dp20, alignment = Alignment.CenterVertically),
        ) {
          ClerkTextField(
            onValueChange = {},
            value = "Input",
            label = "Label",
            trailingIcon = R.drawable.ic_cross,
            supportingText = "Supporting text",
            isError = true,
          )

          ClerkTextField(
            onValueChange = {},
            value = "",
            label = "Label",
            trailingIcon = R.drawable.ic_cross,
            supportingText = "Supporting text",
            isError = true,
          )

          ClerkTextField(
            onValueChange = {},
            value = "Input",
            placeholder = "Placeholder",
            label = "Label",
            supportingText = "Supporting text",
            isError = true,
          )

          ClerkTextField(
            onValueChange = {},
            value = "",
            label = "Label",
            supportingText = "Supporting text",
            leadingIcon = R.drawable.ic_search,
            isError = true,
          )

          ClerkTextField(
            onValueChange = {},
            value = "Input",
            label = "Label",
            trailingIcon = R.drawable.ic_cross,
            leadingIcon = R.drawable.ic_search,
            supportingText = "Supporting text",
            isError = true,
          )

          ClerkTextField(
            onValueChange = {},
            value = "",
            label = "Label",
            trailingIcon = R.drawable.ic_cross,
            leadingIcon = R.drawable.ic_search,
            supportingText = "Supporting text",
            isError = true,
          )

          ClerkTextField(
            onValueChange = {},
            value = "Input",
            label = "Label",
            supportingText = "Supporting text",
            isError = true,
          )
        }
      }
    }
  }
}
