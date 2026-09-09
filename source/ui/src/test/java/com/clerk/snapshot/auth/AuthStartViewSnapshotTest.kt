package com.clerk.snapshot.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.rememberNavBackStack
import com.clerk.api.OAuthProvider
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.auth.AuthDestination
import com.clerk.ui.auth.AuthStartViewHelper
import com.clerk.ui.auth.AuthStartViewImpl
import com.clerk.ui.auth.AuthStartViewModel
import com.clerk.ui.core.composition.AuthStateProvider
import org.junit.Test

class AuthStartViewSnapshotTest : BaseSnapshotTest() {

  @Test
  fun authStartShowsDismissActionAsTrailingChrome() {
    val authViewHelper =
      AuthStartViewHelper(com.clerk.testing.mockClerk()).apply {
        setTestValues(
          enabledFirstFactorAttributes = listOf("email_address"),
          socialProviders = listOf(OAuthProvider.Google),
          applicationName = "Acme Co",
        )
      }

    snapshot {
      Box(Modifier.size(width = 390.dp, height = 640.dp)) {
        AuthStateProvider(rememberNavBackStack(AuthDestination.AuthStart)) {
          AuthStartViewImpl(
            modifier = Modifier.fillMaxSize(),
            authViewHelper = authViewHelper,
            isDismissible = true,
            onDismiss = {},
            onAuthComplete = {},
            authStartViewModel =
              AuthStartViewModel(com.clerk.ui.core.composition.LocalClerk.current),
          )
        }
      }
    }
  }
}
