package com.clerk.customflows.emailpassword.signup

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class EmailPasswordSignUpActivity : ComponentActivity() {

  val viewModel: EmailPasswordSignUpViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val state by viewModel.uiState.collectAsStateWithLifecycle()
      EmailPasswordSignInView(
        state = state,
        onSubmit = viewModel::submit,
        onVerify = viewModel::verify,
      )
    }
  }
}

@Composable
fun EmailPasswordSignInView(
  state: EmailPasswordSignUpViewModel.EmailPasswordSignUpUiState,
  onSubmit: (String, String) -> Unit,
  onVerify: (String) -> Unit,
) {
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var code by remember { mutableStateOf("") }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    when (state) {
      EmailPasswordSignUpViewModel.EmailPasswordSignUpUiState.Unverified -> {
        Column(
          verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
          TextField(
            value = password,
            onValueChange = { password = it },
            visualTransformation = PasswordVisualTransformation(),
            label = { Text("Password") },
          )
          Button(onClick = { onSubmit(email, password) }) { Text("Next") }
        }
      }
      EmailPasswordSignUpViewModel.EmailPasswordSignUpUiState.Verified -> {
        Text("Verified!")
      }
      EmailPasswordSignUpViewModel.EmailPasswordSignUpUiState.Verifying -> {
        Column(
          verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          TextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("Enter your verification code") },
          )
          Button(onClick = { onVerify(code) }) { Text("Verify") }
        }
      }
      EmailPasswordSignUpViewModel.EmailPasswordSignUpUiState.Loading -> CircularProgressIndicator()
    }
  }
}
