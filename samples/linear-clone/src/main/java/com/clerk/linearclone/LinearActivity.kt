package com.clerk.linearclone

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.ui.auth.AuthMode
import com.clerk.ui.auth.AuthView
import com.clerk.ui.core.composition.ClerkProvider
import com.clerk.ui.core.composition.LocalClerk
import kotlinx.coroutines.flow.MutableStateFlow

class LinearFeedback {
  val error = MutableStateFlow<String?>(null)
  val continuation = MutableStateFlow<AuthMode?>(null)
}

abstract class LinearActivity : ComponentActivity() {
  private val app
    get() = application as LinearCloneApp

  protected val clerk: Clerk
    get() = app.connection.value?.getOrThrow() ?: error("Clerk is not connected")

  protected val feedback
    get() = app.feedback

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    app.setActivity(this)
    if (savedInstanceState == null) intent.data?.let { app.handleCallback(it.toString()) }
  }

  override fun onResume() {
    super.onResume()
    app.setActivity(this)
  }

  override fun onDestroy() {
    app.clearActivity(this)
    super.onDestroy()
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    intent.data?.let { app.handleCallback(it.toString()) }
  }

  protected fun setClerkContent(content: @Composable () -> Unit) {
    setContent {
      val connection by app.connection.collectAsStateWithLifecycle()
      val callbackError by app.callbackError.collectAsStateWithLifecycle()
      val operationError by feedback.error.collectAsStateWithLifecycle()
      val continuation by feedback.continuation.collectAsStateWithLifecycle()
      MaterialTheme {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          when {
            connection == null -> CircularProgressIndicator()
            connection!!.isFailure ->
              Column {
                Text(connection!!.exceptionOrNull()?.localizedMessage ?: "Unable to connect")
                Button(onClick = app::connect) { Text("Retry") }
              }
            else ->
              ClerkProvider(connection!!.getOrThrow()) {
                val owner = LocalClerk.current
                if (
                  owner.session?.currentTask != null ||
                    continuation != null ||
                    owner.authCallback != null
                ) {
                  AuthView(
                    mode = continuation ?: AuthMode.SignInOrUp,
                    isDismissible = owner.session?.currentTask == null,
                    onDismiss = { feedback.continuation.value = null },
                    onAuthComplete = { feedback.continuation.value = null },
                  )
                } else
                  androidx.compose.runtime.CompositionLocalProvider(
                    LocalLinearFeedback provides feedback
                  ) {
                    content()
                  }
              }
          }
          val message = operationError ?: callbackError
          if (message != null)
            AlertDialog(
              onDismissRequest = {
                feedback.error.value = null
                app.clearCallbackError()
              },
              title = { Text("Unable to complete authentication") },
              text = { Text(message) },
              confirmButton = {
                TextButton(
                  onClick = {
                    feedback.error.value = null
                    app.clearCallbackError()
                  }
                ) {
                  Text("OK")
                }
              },
            )
        }
      }
    }
  }
}

internal val LocalLinearFeedback =
  androidx.compose.runtime.staticCompositionLocalOf<LinearFeedback> {
    error("Missing Linear sample feedback owner")
  }

@Composable
internal inline fun <reified T : androidx.lifecycle.ViewModel> linearViewModel(
  crossinline create: (Clerk, LinearFeedback) -> T
): T {
  val clerk = LocalClerk.current
  val feedback = LocalLinearFeedback.current
  return androidx.lifecycle.viewmodel.compose.viewModel { create(clerk, feedback) }
}
