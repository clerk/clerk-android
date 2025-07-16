package com.clerk.exampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clerk.exampleapp.navigation.Route
import com.clerk.exampleapp.ui.screens.HomeScreen
import com.clerk.exampleapp.ui.screens.LoadingScreen
import com.clerk.exampleapp.ui.screens.signin.SignInScreen
import com.clerk.exampleapp.ui.theme.ClerkAndroidSDKTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val mainViewModel: MainViewModel by viewModels()
      val state by mainViewModel.uiState.collectAsStateWithLifecycle()
      val navController = rememberNavController()

      fun navigateTo(route: Route) {
        navController.navigate(route)
      }

      ClerkAndroidSDKTheme {
        Scaffold { innerPadding ->
          Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
          ) {
            NavHost(navController = navController, startDestination = Route.Loading) {
              composable<Route.Loading> { LoadingScreen(state = state, navigateTo = ::navigateTo) }
              composable<Route.SignIn> { SignInScreen(navigateTo = ::navigateTo) }
              composable<Route.Home> { HomeScreen() }
            }
          }
        }
      }
    }
  }
}
