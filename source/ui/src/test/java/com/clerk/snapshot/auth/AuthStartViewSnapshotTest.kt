package com.clerk.snapshot.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clerk.api.sso.OAuthProvider
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.auth.AuthStartViewHelper
import com.clerk.ui.auth.AuthStartViewImpl
import com.clerk.ui.auth.AuthStartViewModel
import com.clerk.ui.auth.PreviewAuthStateProvider
import org.junit.Test

class AuthStartViewSnapshotTest : BaseSnapshotTest() {

  @Test
  fun authStartShowsDismissActionAsTrailingChrome() {
    val authViewHelper =
      AuthStartViewHelper().apply {
        setTestValues(
          enabledFirstFactorAttributes = listOf("email_address"),
          socialProviders = listOf(OAuthProvider.GOOGLE),
          applicationName = "Acme Co",
        )
      }

    paparazzi.snapshot {
      Box(Modifier.size(width = 390.dp, height = 640.dp)) {
        PreviewAuthStateProvider {
          AuthStartViewImpl(
            modifier = Modifier.fillMaxSize(),
            authViewHelper = authViewHelper,
            isDismissable = true,
            onDismiss = {},
            onAuthComplete = {},
            authStartViewModel = AuthStartViewModel(),
          )
        }
      }
    }
  }
}
