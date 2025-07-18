package com.clerk.quickstart.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SignInView(viewModel: SignInViewModel = viewModel()) {

  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
  ) {
    Text("Sign In")
    TextField(value = email, onValueChange = { email = it }, placeholder = { Text("Email") })
    TextField(
      value = password,
      onValueChange = { password = it },
      placeholder = { Text("Password") },
      visualTransformation = PasswordVisualTransformation(),
    )
    Button(onClick = { viewModel.signIn(email, password) }) { Text("Sign In") }
  }
}
