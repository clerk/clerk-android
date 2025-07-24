package com.clerk.customflows.emailpassword.signin

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
import com.clerk.Clerk

class EmailPasswordSignInActivity : ComponentActivity() {

  val viewModel: EmailPasswordSignInViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val state by viewModel.uiState.collectAsStateWithLifecycle()
      EmailPasswordSignInView(state = state, onSubmit = viewModel::submit)
    }
  }
}

@Composable
fun EmailPasswordSignInView(
  state: EmailPasswordSignInViewModel.EmailPasswordSignInUiState,
  onSubmit: (String, String) -> Unit,
) {
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    when (state) {
      EmailPasswordSignInViewModel.EmailPasswordSignInUiState.SignedOut -> {
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
          Button(onClick = { onSubmit(email, password) }) { Text("Sign in") }
        }
      }
      EmailPasswordSignInViewModel.EmailPasswordSignInUiState.SignedIn -> {
        Text("Current session: ${Clerk.session?.id}")
      }

      EmailPasswordSignInViewModel.EmailPasswordSignInUiState.Loading ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
    }
  }
}
