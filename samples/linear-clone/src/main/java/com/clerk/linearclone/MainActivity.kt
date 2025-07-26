package com.clerk.linearclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clerk.linearclone.navigation.GetStartedRoute
import com.clerk.linearclone.navigation.LoginRoute
import com.clerk.linearclone.ui.screens.GetStartedScreen
import com.clerk.linearclone.ui.screens.LoginScreen
import com.clerk.linearclone.ui.theme.ClerkTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      ClerkTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          val navController = rememberNavController()

          NavHost(
            navController = navController,
            startDestination = GetStartedRoute,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
          ) {
            composable<GetStartedRoute> {
              GetStartedScreen(onGetStartedClick = { navController.navigate(LoginRoute) })
            }

            composable<LoginRoute> { LoginScreen() }
          }
        }
      }
    }
  }
}
