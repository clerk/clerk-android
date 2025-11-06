package com.clerk.workbench

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.core.view.WindowCompat.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.ui.auth.AuthView
import com.clerk.ui.userprofile.UserProfileView
import com.clerk.workbench.ui.theme.WorkbenchTheme

class UiActivity2 : ComponentActivity() {
  val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle =
        SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
    )
    setContent {
      WorkbenchTheme {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        Box(
          modifier = Modifier.fillMaxSize().background(color = Color(0xFFF9F9F9)),
          contentAlignment = Alignment.Center,
        ) {
          when (state) {
            MainViewModel.UiState.Loading -> CircularProgressIndicator()
            MainViewModel.UiState.SignedIn -> {
              UserProfileView()
            }

            MainViewModel.UiState.SignedOut -> {
              AuthView {}
            }
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
