package com.clerk.prebuiltui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.prebuiltui.ui.theme.ClerkTheme
import com.clerk.ui.auth.AuthView
import com.clerk.ui.userbutton.UserButton

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val user by Clerk.userFlow.collectAsStateWithLifecycle()
      ClerkTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (user != null) {
              UserButton()
            } else {
              AuthView()
            }
          }
        }
      }
    }
  }
}
