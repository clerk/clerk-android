package com.clerk.linearclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
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
import com.clerk.linearclone.ui.home.HomeScreen
import com.clerk.linearclone.ui.theme.LinearCloneTheme

class MainActivity : ComponentActivity() {
  val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val state by viewModel.uiState.collectAsState()
      val navController = rememberNavController()

      LinearCloneTheme {
        Box(
          modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.primary),
          contentAlignment = Alignment.Center,
        ) {
          when (state) {
            MainViewModel.UiState.Loading ->
              CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            MainViewModel.UiState.SignedIn -> HomeScreen()
            MainViewModel.UiState.SignedOut -> AuthNavigationGraph(navController)
          }
        }
      }
    }
  }
}

@Composable
private fun AuthNavigationGraph(navController: NavHostController) {
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
      EmailVerificationScreen(email = email, onNavigateToLogin = { navController.popBackStack() })
    }
  }
}
