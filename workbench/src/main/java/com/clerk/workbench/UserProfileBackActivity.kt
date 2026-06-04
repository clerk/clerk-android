package com.clerk.workbench

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.userprofile.UserProfileDismissButtonStyle
import com.clerk.ui.userprofile.UserProfileView
import com.clerk.workbench.ui.theme.Background
import com.clerk.workbench.ui.theme.BackgroundDark
import com.clerk.workbench.ui.theme.WorkbenchTheme

class UserProfileBackActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle =
        SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
    )
    setContent {
      WorkbenchTheme {
        UserProfileDemoSurface {
          UserProfileView(
            isDismissible = true,
            dismissButtonStyle = UserProfileDismissButtonStyle.Back,
            onDismiss = { finish() },
          )
        }
      }
    }
  }
}

@Composable
internal fun UserProfileDemoSurface(content: @Composable () -> Unit) {
  val backgroundColor = if (isSystemInDarkTheme()) BackgroundDark else Background
  Column(modifier = Modifier.fillMaxSize().background(color = backgroundColor)) {
    WorkbenchAuthGate(persistIdentifiers = false) {
      Column(modifier = Modifier.background(color = backgroundColor).fillMaxSize()) { content() }
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewUserProfileDemoSurface() {
  WorkbenchTheme {}
}
