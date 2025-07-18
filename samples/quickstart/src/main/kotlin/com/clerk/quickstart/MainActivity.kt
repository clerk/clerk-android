package com.clerk.quickstart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val viewModel: MainViewModel = viewModel()
      val state by viewModel.uiState.collectAsStateWithLifecycle()
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (state) {
          MainUiState.Loading -> CircularProgressIndicator()
          MainUiState.SignedIn -> Text("Signed in")
          MainUiState.SignedOut -> SignInOrUpView()
        }
      }
    }
  }

  val viewModel: MainViewModel by viewModels()
}
