package com.clerk.workbench

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.userprofile.UserProfileView
import com.clerk.workbench.ui.theme.WorkbenchTheme

class UserProfileCloseActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle =
        SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
    )
    setContent {
      WorkbenchTheme {
        UserProfileDemoSurface { UserProfileView(isDismissible = true, onDismiss = { finish() }) }
      }
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewUserProfileCloseActivity() {
  WorkbenchTheme {}
}
