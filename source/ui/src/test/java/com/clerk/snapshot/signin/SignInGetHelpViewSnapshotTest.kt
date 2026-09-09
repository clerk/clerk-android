package com.clerk.snapshot.signin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.rememberNavBackStack
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.auth.AuthDestination
import com.clerk.ui.core.composition.AuthStateProvider
import com.clerk.ui.signin.help.SignInGetHelpView
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkTheme
import com.clerk.ui.theme.DefaultColors
import org.junit.Test

class SignInGetHelpViewSnapshotTest : BaseSnapshotTest() {

  @Test
  fun signInGetHelpSnapShotTest() {
    snapshotTheme = ClerkTheme(colors = DefaultColors.clerk)
    snapshot {
      Box(Modifier.size(740.dp)) { // finite constraints
        AuthStateProvider(rememberNavBackStack(AuthDestination.AuthStart)) {
          ClerkMaterialTheme { SignInGetHelpView(Modifier.fillMaxSize()) }
        }
      }
    }
  }
}
