package com.clerk.customflows.oauth

import android.os.Bundle
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
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.clerk.api.SignInSSOParamsStrategy
import com.clerk.customflows.CustomFlowActivity

class OAuthActivity : CustomFlowActivity() {
  val viewModel: OAuthViewModel by viewModels {
    viewModelFactory { initializer { OAuthViewModel(clerk, feedback) } }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setClerkContent {
      val state by viewModel.uiState.collectAsStateWithLifecycle()
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (state) {
          OAuthViewModel.UiState.Authenticated -> Text("Authenticated")
          OAuthViewModel.UiState.Loading -> CircularProgressIndicator()
          OAuthViewModel.UiState.SignedOut -> {
            val provider = SignInSSOParamsStrategy.OauthGoogle
            Button(onClick = { viewModel.signInWithOAuth(provider) }) {
              Text("Sign in with Google")
            }
          }
        }
      }
    }
  }
}
