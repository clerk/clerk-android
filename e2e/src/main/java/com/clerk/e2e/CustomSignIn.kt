package com.clerk.e2e

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.verifyCode
import kotlinx.coroutines.launch

@Composable
fun CustomSignIn(onSignedIn: () -> Unit) {
  val scope = rememberCoroutineScope()
  var phoneNumber by rememberSaveable { mutableStateOf("") }
  var code by rememberSaveable { mutableStateOf("") }
  var codeSent by rememberSaveable { mutableStateOf(false) }
  var error by rememberSaveable { mutableStateOf<String?>(null) }
  var busy by remember { mutableStateOf(false) }

  fun submit(request: suspend () -> String?, onSuccess: () -> Unit) {
    if (busy) return
    busy = true
    scope.launch {
      error = request()
      if (error == null) onSuccess()
      busy = false
    }
  }

  Text("Custom sign-in", style = MaterialTheme.typography.headlineSmall)
  if (codeSent) {
    OutlinedTextField(
      value = code,
      onValueChange = { code = it },
      modifier = Modifier.testTag(E2ETags.CustomSignIn.CODE),
      label = { Text("SMS code") },
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    TextAction("Verify code", E2ETags.CustomSignIn.VERIFY_CODE) {
      submit({ verifyCode(code) }, onSignedIn)
    }
  } else {
    OutlinedTextField(
      value = phoneNumber,
      onValueChange = { phoneNumber = it },
      modifier = Modifier.testTag(E2ETags.CustomSignIn.PHONE_NUMBER),
      label = { Text("Phone number") },
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
    )
    TextAction("Send code", E2ETags.CustomSignIn.SEND_CODE) {
      submit({ sendCode(phoneNumber) }) { codeSent = true }
    }
  }
  error?.let {
    Text(
      text = it,
      modifier = Modifier.testTag(E2ETags.CustomSignIn.ERROR),
      color = MaterialTheme.colorScheme.error,
    )
  }
}

private suspend fun sendCode(phoneNumber: String): String? =
  when (val result = Clerk.auth.signInWithOtp { phone = phoneNumber.trim() }) {
    is ClerkResult.Success -> null
    is ClerkResult.Failure -> result.errorMessage
  }

private suspend fun verifyCode(code: String): String? {
  val signIn = Clerk.auth.currentSignIn ?: return "No sign-in is in progress."
  return when (val result = signIn.verifyCode(code.trim())) {
    is ClerkResult.Failure -> result.errorMessage
    is ClerkResult.Success -> activate(result.value)
  }
}

private suspend fun activate(signIn: SignIn): String? {
  val sessionId = signIn.createdSessionId
  if (signIn.status != SignIn.Status.COMPLETE || sessionId == null) {
    return "Sign-in is ${signIn.statusRawValue}"
  }
  return when (val result = Clerk.auth.setActive(sessionId)) {
    is ClerkResult.Success -> null
    is ClerkResult.Failure -> result.errorMessage
  }
}
