package com.clerk.customflows.emailpassword.mfa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MFASignInActivity : ComponentActivity() {
  val viewModel: MFASignInViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val state by viewModel.uiState.collectAsStateWithLifecycle()
      MFASignInView(state = state, onSubmit = viewModel::submit, onVerify = viewModel::verify)
    }
  }
}

@Composable
fun MFASignInView(
  state: MFASignInViewModel.UiState,
  onSubmit: (String, String) -> Unit,
  onVerify: (String) -> Unit,
) {
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var code by remember { mutableStateOf("") }

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    when (state) {
      MFASignInViewModel.UiState.NeedsSecondFactor -> {
        TextField(value = code, onValueChange = { code = it }, placeholder = { Text("Code") })
        Button(onClick = { onVerify(code) }) { Text("Submit") }
      }
      MFASignInViewModel.UiState.Unverified -> {
        TextField(value = email, onValueChange = { email = it }, placeholder = { Text("Email") })
        TextField(
          value = password,
          onValueChange = { password = it },
          placeholder = { Text("Password") },
          visualTransformation = PasswordVisualTransformation(),
        )
        Button(onClick = { onSubmit(email, password) }) { Text("Next") }
      }
      MFASignInViewModel.UiState.Verified -> {
        Text("Verified")
      }
    }
  }
}
