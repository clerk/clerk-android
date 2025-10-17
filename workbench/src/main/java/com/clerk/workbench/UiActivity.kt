package com.clerk.workbench

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.ui.ClerkColors
import com.clerk.ui.auth.AuthView
import com.clerk.ui.userprofile.security.passkey.UserProfilePasskeySection
import com.clerk.workbench.ui.theme.WorkbenchTheme

class UiActivity : ComponentActivity() {
  val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      val dark =
        ClerkColors(
          primary = Color(0xFFFAFAFB),
          background = Color(0xFF131316),
          input = Color(0xFF212126),
          danger = Color(0xFFEF4444),
          success = Color(0xFF22C543),
          warning = Color(0xFFF36B16),
          foreground = Color(0xFFFFFFFF),
          mutedForeground = Color(0xFFB7B8C2),
          primaryForeground = Color(0xFF000000),
          inputForeground = Color(0xFFFFFFFF),
          neutral = Color(0xFFFFFFFF),
          border = Color(0xFFFFFFFF),
          ring = Color(0xFFFFFFFF),
          muted = Color(0xFF1A1A1D),
          shadow = Color(0xFFFFFFFF),
        )
      WorkbenchTheme {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        Scaffold {
          Box(
            modifier = Modifier.fillMaxSize().padding(it).background(color = Color(0xFFF9F9F9)),
            contentAlignment = Alignment.Center,
          ) {
            when (state) {
              MainViewModel.UiState.Loading -> CircularProgressIndicator()
              MainViewModel.UiState.SignedIn -> {
                Box(
                  modifier = Modifier.fillMaxSize().background(dark.input!!),
                  contentAlignment = Alignment.Center,
                ) {
                  UserProfilePasskeySection(onError = {})
                }
              }

              MainViewModel.UiState.SignedOut -> {
                MainContent()
              }
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
    ) {
      AuthView {}
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewMainContent() {
  WorkbenchTheme {}
}
