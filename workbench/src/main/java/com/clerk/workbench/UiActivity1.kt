package com.clerk.workbench

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.workbench.ui.theme.WorkbenchTheme
import kotlinx.coroutines.launch

class UiActivity1 : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      val scope = rememberCoroutineScope()
      WorkbenchTheme {
        Box(
          modifier = Modifier.fillMaxSize().background(color = Color(0xFFF9F9F9)),
          contentAlignment = Alignment.Center,
        ) {
          WorkbenchAuthGate {
            ClerkButton(
              modifier = Modifier.align(Alignment.Center),
              text = "Sign Out",
              onClick = { scope.launch { Clerk.auth.signOut() } },
            )
          }
        }
      }
    }
  }

  @Suppress("MagicNumber")
  @Composable
  private fun MainContent() {
    Box(
      modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.background),
      contentAlignment = Alignment.Center,
    ) {}
  }
}

@PreviewLightDark
@Composable
private fun PreviewMainContent() {
  WorkbenchTheme {}
}
