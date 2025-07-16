package com.clerk.exampleapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.clerk.exampleapp.AuthenticationState
import com.clerk.exampleapp.navigation.Route

@Composable
fun LoadingScreen(
  state: AuthenticationState,
  modifier: Modifier = Modifier,
  navigateTo: (Route) -> Unit,
) {

  LaunchedEffect(key1 = state) {
    if (state is AuthenticationState.SignedIn) {
      navigateTo(Route.Home)
    } else if (state is AuthenticationState.SignedOut) {
      navigateTo(Route.SignIn)
    }
  }

  Box(modifier = Modifier.fillMaxSize().then(modifier), contentAlignment = Alignment.Center) {
    CircularProgressIndicator()
  }
}
