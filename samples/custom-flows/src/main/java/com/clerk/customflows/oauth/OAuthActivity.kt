package com.clerk.customflows.oauth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.sso.OAuthProvider

class OAuthActivity : ComponentActivity() {
  val viewModel: OAuthViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val state by viewModel.uiState.collectAsStateWithLifecycle()
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (state) {
          OAuthViewModel.UiState.Authenticated -> Text("Authenticated")
          OAuthViewModel.UiState.Loading -> CircularProgressIndicator()
          OAuthViewModel.UiState.SignedOut -> {
            val provider = OAuthProvider.GOOGLE // Or .GITHUB, .SLACK etc.
            Button(onClick = { viewModel.signInWithOAuth(provider) }) {
              Text("Sign in with ${provider.name}")
            }
          }
        }
      }
    }
  }
}
