package com.clerk.exampleapp.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.Clerk
import com.clerk.exampleapp.R
import com.clerk.exampleapp.navigation.Route

@Composable
fun ProfileScreen(
  navigateTo: (Route) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: ProfileViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  LaunchedEffect(state) {
    if (state is ProfileAuthenticationState.SignedOut) {
      navigateTo(Route.SignIn)
    }
  }
  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp).then(modifier),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
  ) {
    Text("Session ID: ${Clerk.session?.id}")

    Button(
      modifier = Modifier.fillMaxWidth().height(52.dp),
      shape = RoundedCornerShape(8.dp),
      onClick = { viewModel.signOut() },
    ) {
      Text(stringResource(R.string.sign_out))
    }
  }
}
