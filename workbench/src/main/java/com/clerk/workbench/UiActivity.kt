package com.clerk.workbench

import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.clerk.ui.auth.AuthView
import com.clerk.ui.core.composition.LocalClerk
import kotlinx.coroutines.launch

class UiActivity : WorkbenchActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setClerkContent {
      val owner = LocalClerk.current
      var errorMessage by remember { mutableStateOf<String?>(null) }
      val scope = rememberCoroutineScope()
      errorMessage?.let { message ->
        AlertDialog(
          onDismissRequest = { errorMessage = null },
          title = { Text("Unable to sign out") },
          text = { Text(message) },
          confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("OK") } },
        )
      }
      Scaffold { innerPadding ->
        Box(
          modifier = Modifier.fillMaxSize().padding(innerPadding),
          contentAlignment = Alignment.Center,
        ) {
          if (LocalClerk.isAuthFlowComplete) {
            Button(
              onClick = {
                scope.launch {
                  try {
                    owner.signOut()
                  } catch (error: kotlinx.coroutines.CancellationException) {
                    throw error
                  } catch (error: Exception) {
                    errorMessage = error.localizedMessage ?: "Please try again."
                  }
                }
              }
            ) {
              Text("Sign out")
            }
          } else {
            AuthView(isDismissible = false)
          }
        }
      }
    }
  }
}
