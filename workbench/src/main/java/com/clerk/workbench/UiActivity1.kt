package com.clerk.workbench

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.ui.auth.AuthView
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.workbench.ui.theme.WorkbenchTheme
import kotlinx.coroutines.launch

class UiActivity1 : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      val scope = rememberCoroutineScope()
      val user by Clerk.userFlow.collectAsStateWithLifecycle()
      var showAuth by rememberSaveable { mutableStateOf(true) }

      LaunchedEffect(user?.id) { if (user != null) showAuth = false }

      WorkbenchTheme {
        Box(
          modifier = Modifier.fillMaxSize().background(color = Color(0xFFF9F9F9)),
          contentAlignment = Alignment.Center,
        ) {
          when {
            user == null && showAuth ->
              AuthView(
                modifier = Modifier.fillMaxSize(),
                isDismissable = true,
                onDismiss = { showAuth = false },
                onAuthComplete = { showAuth = false },
              )
            user == null -> DismissedAuthExample(onOpenAuth = { showAuth = true })
            else ->
              ClerkButton(
                modifier = Modifier.align(Alignment.Center),
                text = "Sign Out",
                onClick = { scope.launch { Clerk.auth.signOut() } },
              )
          }
        }
      }
    }
  }

  @Composable
  private fun DismissedAuthExample(onOpenAuth: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "AuthView dismissed",
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.titleLarge,
      )
      ClerkButton(
        modifier = Modifier.padding(top = 16.dp),
        text = "Open AuthView",
        onClick = onOpenAuth,
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewMainContent() {
  WorkbenchTheme {}
}
