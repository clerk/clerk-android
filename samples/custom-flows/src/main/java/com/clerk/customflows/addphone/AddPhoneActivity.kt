package com.clerk.customflows.addphone

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
import com.clerk.phonenumber.PhoneNumber

class AddPhoneActivity : ComponentActivity() {
  val viewModel: AddPhoneViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val state by viewModel.uiState.collectAsStateWithLifecycle()
      AddPhoneView(
        state = state,
        onCreatePhoneNumber = viewModel::createPhoneNumber,
        onVerifyCode = viewModel::verifyCode,
      )
    }
  }
}

@Composable
fun AddPhoneView(
  state: AddPhoneViewModel.UiState,
  onCreatePhoneNumber: (String) -> Unit,
  onVerifyCode: (String, PhoneNumber) -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    when (state) {
      AddPhoneViewModel.UiState.NeedsVerification -> {
        InputContentView(buttonText = "Continue", placeholder = "Enter phone number") {
          onCreatePhoneNumber(it)
        }
      }

      AddPhoneViewModel.UiState.Verified -> Text("Verified!")

      is AddPhoneViewModel.UiState.Verifying -> {
        InputContentView(buttonText = "Verify", placeholder = "Enter code") {
          onVerifyCode(it, state.phoneNumber)
        }
      }

      AddPhoneViewModel.UiState.Loading -> CircularProgressIndicator()
      AddPhoneViewModel.UiState.SignedOut -> Text("You must be signed in to add a phone number.")
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
