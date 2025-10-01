package com.clerk.snapshot.signin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.signin.help.SignInGetHelpView
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import org.junit.Test

class SignInGetHelpViewSnapshotTest : BaseSnapshotTest() {

  @Test
  fun signInGetHelpSnapShotTest() {
    paparazzi.snapshot {
      Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
      Box(Modifier.size(740.dp)) { // finite constraints
        PreviewAuthStateProvider {
          ClerkMaterialTheme { SignInGetHelpView(Modifier.fillMaxSize()) }
        }
      }
    }
  }
}
