package com.clerk.customflows.addemail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.clerk.emailaddress.EmailAddress

class AddEmailActivity : ComponentActivity() {
  val viewModel: AddEmailViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val state by viewModel.uiState.collectAsStateWithLifecycle()
      AddEmailView(
        state = state,
        onCreateEmailAddress = viewModel::createEmailAddress,
        onVerifyCode = viewModel::verifyCode,
      )
    }
  }
}

@Composable
fun AddEmailView(
  state: AddEmailViewModel.UiState,
  onCreateEmailAddress: (String) -> Unit,
  onVerifyCode: (String, EmailAddress) -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    when (state) {
      AddEmailViewModel.UiState.NeedsVerification -> {
        InputContentView(buttonText = "Continue", placeholder = "Enter email address") {
          onCreateEmailAddress(it)
        }
      }

      AddEmailViewModel.UiState.Verified -> Text("Verified!")

      is AddEmailViewModel.UiState.Verifying -> {
        InputContentView(buttonText = "Verify", placeholder = "Enter code") {
          onVerifyCode(it, state.emailAddress)
        }
      }

      AddEmailViewModel.UiState.Loading -> CircularProgressIndicator()
      AddEmailViewModel.UiState.SignedOut -> Text("You must be signed in to add an email address.")
    }
  }
}

@Composable
fun InputContentView(
  buttonText: String,
  placeholder: String,
  modifier: Modifier = Modifier,
  onClick: (String) -> Unit,
) {
  var input by remember { mutableStateOf("") }
  Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
    TextField(
      modifier = Modifier.padding(bottom = 16.dp),
      value = input,
      onValueChange = { input = it },
      placeholder = { Text(placeholder) },
    )
    Button(onClick = { onClick(input) }) { Text(buttonText) }
  }
}
