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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.clerk.api.Clerk
import com.clerk.api.auth.types.Strategy
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.session.Session
import com.clerk.api.session.SessionVerification
import com.clerk.api.session.sendResetPasswordEmailCode
import com.clerk.api.session.startVerification
import com.clerk.api.session.verifyWithResetPasswordEmailCode
import kotlinx.coroutines.launch

@Composable
fun ResetPasswordReverification(onVerified: () -> Unit) {
  val scope = rememberCoroutineScope()
  var sentTo by rememberSaveable { mutableStateOf<String?>(null) }
  var code by rememberSaveable { mutableStateOf("") }
  var newPassword by rememberSaveable { mutableStateOf("") }
  var error by rememberSaveable { mutableStateOf<String?>(null) }
  var busy by remember { mutableStateOf(false) }

  fun submit(request: suspend () -> Outcome) {
    if (busy) return
    busy = true
    scope.launch {
      when (val outcome = request()) {
        is Outcome.CodeSent -> {
          error = null
          sentTo = outcome.safeIdentifier
        }
        Outcome.Verified -> onVerified()
        is Outcome.Failed -> error = outcome.message
      }
      busy = false
    }
  }

  Text("Reverify with a reset code", style = MaterialTheme.typography.headlineSmall)
  val destination = sentTo
  if (destination != null) {
    Text(
      "Code sent to $destination",
      modifier = Modifier.testTag(E2ETags.ResetPasswordReverification.SENT_TO),
    )
    OutlinedTextField(
      value = code,
      onValueChange = { code = it },
      modifier = Modifier.testTag(E2ETags.ResetPasswordReverification.CODE),
      label = { Text("Email code") },
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    OutlinedTextField(
      value = newPassword,
      onValueChange = { newPassword = it },
      modifier = Modifier.testTag(E2ETags.ResetPasswordReverification.NEW_PASSWORD),
      label = { Text("New password") },
      singleLine = true,
      visualTransformation = PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
    TextAction("Verify and set password", E2ETags.ResetPasswordReverification.VERIFY) {
      submit { verify(code, newPassword) }
    }
  } else {
    TextAction("Send reset code", E2ETags.ResetPasswordReverification.SEND_CODE) {
      submit { sendCode() }
    }
  }
  error?.let {
    Text(
      text = it,
      modifier = Modifier.testTag(E2ETags.ResetPasswordReverification.ERROR),
      color = MaterialTheme.colorScheme.error,
    )
  }
}

private sealed interface Outcome {
  data class CodeSent(val safeIdentifier: String) : Outcome

  data object Verified : Outcome

  data class Failed(val message: String) : Outcome
}

private suspend fun sendCode(): Outcome {
  val session = Clerk.session ?: return Outcome.Failed("No session is active.")
  return when (val result = session.startVerification(SessionVerification.Level.FIRST_FACTOR)) {
    is ClerkResult.Failure -> Outcome.Failed(result.errorMessage)
    is ClerkResult.Success -> sendCode(session, result.value)
  }
}

private suspend fun sendCode(session: Session, verification: SessionVerification): Outcome {
  val factor =
    verification.supportedFirstFactors?.firstOrNull {
      it.strategy == Strategy.ResetPasswordEmailCode.value
    }
  val emailAddressId =
    factor?.emailAddressId
      ?: return Outcome.Failed(
        "Reverification does not offer ${Strategy.ResetPasswordEmailCode.value}."
      )
  return when (val result = session.sendResetPasswordEmailCode(emailAddressId)) {
    is ClerkResult.Failure -> Outcome.Failed(result.errorMessage)
    is ClerkResult.Success -> Outcome.CodeSent(factor.safeIdentifier.orEmpty())
  }
}

private suspend fun verify(code: String, newPassword: String): Outcome {
  val session = Clerk.session ?: return Outcome.Failed("No session is active.")
  return when (val result = session.verifyWithResetPasswordEmailCode(code.trim(), newPassword)) {
    is ClerkResult.Failure -> Outcome.Failed(result.errorMessage)
    is ClerkResult.Success ->
      if (result.value.status == SessionVerification.Status.COMPLETE) Outcome.Verified
      else Outcome.Failed("Reverification is ${result.value.status}")
  }
}
