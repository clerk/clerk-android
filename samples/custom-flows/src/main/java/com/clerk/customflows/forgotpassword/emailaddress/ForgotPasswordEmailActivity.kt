package com.clerk.customflows.forgotpassword.emailaddress

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk

class ForgotPasswordEmailActivity : ComponentActivity() {
  val viewModel: ForgotPasswordEmailViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val state by viewModel.uiState.collectAsStateWithLifecycle()
      ForgotPasswordView(
        state,
        onVerify = viewModel::verify,
        onSetNewPassword = viewModel::setNewPassword,
        onCreateSignIn = viewModel::createSignIn,
      )
    }
  }
}

@Composable
fun ForgotPasswordView(
  state: ForgotPasswordEmailViewModel.UiState,
  onVerify: (String) -> Unit,
  onSetNewPassword: (String) -> Unit,
  onCreateSignIn: (String) -> Unit,
) {

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    when (state) {
      ForgotPasswordEmailViewModel.UiState.Complete -> {
        Text("Active session: ${Clerk.session?.id}")
      }

      ForgotPasswordEmailViewModel.UiState.NeedsFirstFactor -> {
        InputContent(placeholder = "Enter your code", buttonText = "Verify", onClick = onVerify)
      }
      ForgotPasswordEmailViewModel.UiState.NeedsNewPassword -> {
        InputContent(
          placeholder = "Enter your new password",
          buttonText = "Set new password",
          onClick = onSetNewPassword,
          visualTransformation = PasswordVisualTransformation(),
        )
      }
      ForgotPasswordEmailViewModel.UiState.NeedsSecondFactor -> {
        Text("2FA is required but this UI does not handle that")
      }
      ForgotPasswordEmailViewModel.UiState.SignedOut -> {
        InputContent(
          placeholder = "Enter your email address",
          buttonText = "Forgot password?",
          onClick = onCreateSignIn,
        )
      }

      ForgotPasswordEmailViewModel.UiState.Loading -> CircularProgressIndicator()
    }
  }
}

@Composable
fun InputContent(
  placeholder: String,
  buttonText: String,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  onClick: (String) -> Unit,
) {
  var value by remember { mutableStateOf("") }
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = spacedBy(16.dp, Alignment.CenterVertically),
  ) {
    TextField(
      value = value,
      onValueChange = { value = it },
      visualTransformation = visualTransformation,
      placeholder = { Text(placeholder) },
    )
    Button(onClick = { onClick(value) }) { Text(buttonText) }
  }
}
