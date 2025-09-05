package com.clerk.ui.core.input

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp20
import com.clerk.ui.theme.ClerkMaterialTheme
import org.junit.Test

class ClerkPhoneNumberFieldSnapshotTest : BaseSnapshotTest() {

  @Test
  fun testClerkPhoneNumberField() {
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Column(
          modifier =
            Modifier.background(color = ClerkMaterialTheme.colors.background).padding(dp12),
          verticalArrangement = Arrangement.spacedBy(dp20, alignment = Alignment.CenterVertically),
        ) {}
      }
    }
  }
}
