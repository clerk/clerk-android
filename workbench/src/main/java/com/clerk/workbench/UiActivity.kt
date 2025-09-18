package com.clerk.workbench

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.ui.auth.AuthMode
import com.clerk.ui.auth.AuthStartView
import com.clerk.workbench.ui.theme.WorkbenchTheme

class UiActivity : ComponentActivity() {
  val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { WorkbenchTheme { MainContent(viewModel) } }
  }
}

@Composable
private fun MainContent(viewModel: MainViewModel) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  when (state) {
    MainViewModel.UiState.Loading -> CircularProgressIndicator()
    MainViewModel.UiState.SignedIn -> {}

    MainViewModel.UiState.SignedOut -> AuthStartView(authMode = AuthMode.SignIn)
  }
}

@PreviewLightDark
@Composable
private fun PreviewMainContent() {
  WorkbenchTheme {}
}
