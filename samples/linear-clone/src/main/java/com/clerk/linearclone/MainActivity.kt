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
import androidx.navigation.toRoute
import com.clerk.linearclone.navigation.EmailEntryRoute
import com.clerk.linearclone.navigation.EmailVerificationRoute
import com.clerk.linearclone.navigation.GetStartedRoute
import com.clerk.linearclone.navigation.LoginRoute
import com.clerk.linearclone.ui.chooseloginmethod.ChooseLoginMethodScreen
import com.clerk.linearclone.ui.emailverification.EmailVerificationScreen
import com.clerk.linearclone.ui.enteremail.EnterEmailScreen
import com.clerk.linearclone.ui.getstarted.GetStartedScreen
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
            ChooseLoginMethodScreen(onClickUseEmail = { navController.navigate(EmailEntryRoute) })
          }
          composable<EmailEntryRoute> {
            EnterEmailScreen(
              onNavigateToLogin = { navController.popBackStack() },
              onNavigateToEmailVerification = { navController.navigate(EmailVerificationRoute(it)) },
            )
          }
          composable<EmailVerificationRoute> { backStackEntry ->
            val email = backStackEntry.toRoute<EmailVerificationRoute>().email
            EmailVerificationScreen(
              email = email,
              onNavigateToLogin = { navController.popBackStack() },
            )
          }
        }
      }
    }
  }
}
