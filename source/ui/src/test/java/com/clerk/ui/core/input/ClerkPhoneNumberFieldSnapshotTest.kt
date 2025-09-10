package com.clerk.ui.core.input

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.core.common.dimens.dp12
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import org.junit.Test

class ClerkPhoneNumberFieldSnapshotTest : BaseSnapshotTest() {

  @Test
  fun testClerkPhoneNumberField() {
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Column(
          modifier =
            Modifier.background(color = MaterialTheme.colorScheme.background)
              .fillMaxWidth()
              .padding(dp12),
          verticalArrangement = Arrangement.spacedBy(dp12),
        ) {
          ClerkPhoneNumberField()
          ClerkPhoneNumberField(
            errorText = "The value entered is in an invalid format. Please check and correct it."
          )
        }
      }
    }
  }

  @Test
  fun testClerkPhoneNumberFieldDarkMode() {
    Clerk.customTheme = ClerkTheme(colors = DefaultColors.dark)
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Column(
          modifier =
            Modifier.background(color = ClerkMaterialTheme.colors.background)
              .fillMaxWidth()
              .padding(dp12),
          verticalArrangement = Arrangement.spacedBy(dp12),
        ) {
          ClerkPhoneNumberField(inputText = "5555550100")
          ClerkPhoneNumberField(
            errorText = "The value entered is in an invalid format. Please check and correct it."
          )
        }
      }
    }
  }
}
