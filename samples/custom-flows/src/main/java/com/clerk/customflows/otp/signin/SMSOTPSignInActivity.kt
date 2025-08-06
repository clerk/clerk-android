package com.clerk.customflows.otp.signin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class SMSOTPSignInActivity : ComponentActivity() {
  val viewModel: SMSOTPSignInViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val state by viewModel.uiState.collectAsStateWithLifecycle()
      SMSOTPSignInView(state, viewModel::submit, viewModel::verify)
    }
  }
}

@Composable
fun SMSOTPSignInView(
  state: SMSOTPSignInViewModel.UiState,
  onSubmit: (String) -> Unit,
  onVerify: (String) -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    when (state) {
      SMSOTPSignInViewModel.UiState.Unverified -> {
        InputContent(
          placeholder = "Enter your phone number",
          buttonText = "Continue",
          onClick = onSubmit,
        )
      }
      SMSOTPSignInViewModel.UiState.Verified -> {
        Text("Verified")
      }
      SMSOTPSignInViewModel.UiState.Verifying -> {
        InputContent(
          placeholder = "Enter your verification code",
          buttonText = "Verify",
          onClick = onVerify,
        )
      }

      SMSOTPSignInViewModel.UiState.Loading -> {
        CircularProgressIndicator()
      }
    }
  }
}

@Composable
fun InputContent(placeholder: String, buttonText: String, onClick: (String) -> Unit) {
  var value by remember { mutableStateOf("") }
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
  ) {
    TextField(placeholder = { Text(placeholder) }, value = value, onValueChange = { value = it })
    Button(onClick = { onClick(value) }) { Text(buttonText) }
  }
}
