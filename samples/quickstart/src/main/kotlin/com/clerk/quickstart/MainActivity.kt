package com.clerk.quickstart

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk

class MainActivity : ComponentActivity() {
  private val app
    get() = application as MainApplication

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    app.setActivity(this)
    if (savedInstanceState == null) intent.data?.let { app.handleCallback(it.toString()) }
    setContent {
      val connection by app.connection.collectAsStateWithLifecycle()
      val callbackError by app.callbackError.collectAsStateWithLifecycle()
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
          connection == null -> CircularProgressIndicator()
          connection!!.isFailure ->
            Column {
              Text(connection!!.exceptionOrNull()?.localizedMessage ?: "Unable to connect")
              Button(onClick = app::connect) { Text("Retry") }
            }
          callbackError != null ->
            Column {
              Text(callbackError!!)
              Button(onClick = app::clearCallbackError) { Text("Dismiss") }
            }
          else -> QuickstartContent(connection!!.getOrThrow())
        }
      }
    }
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
}

@Composable
private fun QuickstartContent(clerk: Clerk) {
  val viewModel: MainViewModel = viewModel { MainViewModel(clerk) }
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val error by viewModel.error.collectAsStateWithLifecycle()
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    when (val current = state) {
      MainUiState.Loading -> CircularProgressIndicator()
      MainUiState.SignedIn -> Button(onClick = viewModel::signOut) { Text("Sign Out") }
      MainUiState.SignedOut -> SignInOrUpView(clerk)
      is MainUiState.PendingTask -> {
        Text("Additional session step: ${current.task}")
        Text(
          "This custom-flow sample supports email and password. Use the prebuilt-ui sample for session tasks."
        )
        Button(onClick = viewModel::signOut) { Text("Sign Out") }
      }
    }
    error?.let { Text(it) }
  }
}
