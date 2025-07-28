package com.clerk.linearclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clerk.linearclone.navigation.EmailEntryRoute
import com.clerk.linearclone.navigation.GetStartedRoute
import com.clerk.linearclone.navigation.LoginRoute
import com.clerk.linearclone.ui.screens.EmailEntryScreen
import com.clerk.linearclone.ui.screens.GetStartedScreen
import com.clerk.linearclone.ui.screens.LoginScreen
import com.clerk.linearclone.ui.theme.LinearCloneTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      LinearCloneTheme {
        val navController = rememberNavController()

        NavHost(
          navController = navController,
          startDestination = GetStartedRoute,
          modifier = Modifier.fillMaxSize(),
        ) {
          composable<GetStartedRoute> {
            GetStartedScreen(onGetStartedClick = { navController.navigate(LoginRoute) })
          }

          composable<LoginRoute> {
            LoginScreen(onClickUseEmail = { navController.navigate(EmailEntryRoute) })
          }
          composable<EmailEntryRoute> { EmailEntryScreen() }
        }
      }
    }
  }
}
