package com.clerk.workbench

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.ui.auth.AuthView
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.workbench.ui.theme.WorkbenchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UiActivity : ComponentActivity() {
  val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val context = LocalContext.current
      WorkbenchTheme {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        Box(
          modifier =
            Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.background),
          contentAlignment = Alignment.Center,
        ) {
          when (state) {
            MainViewModel.UiState.Loading -> CircularProgressIndicator()
            MainViewModel.UiState.SignedIn -> {
              ClerkButton(
                text = "Sign out",
                onClick = { scope.launch(Dispatchers.IO) { Clerk.signOut() } },
              )
            }
            MainViewModel.UiState.SignedOut ->
              AuthView(
                onAuthComplete = { Toast.makeText(context, "Signed in", Toast.LENGTH_SHORT).show() }
              )
          }
        }
      }
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewMainContent() {
  WorkbenchTheme {}
}
