package com.clerk.e2e

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.api.user.User
import com.clerk.ui.auth.AuthView
import com.clerk.ui.userprofile.UserProfileView

private const val TEST_PHONE_E164 = "+15555550100"

class MainActivity : ComponentActivity() {
  private val viewModel: E2EViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { E2EApp(viewModel) }
  }
}

@Composable
@Suppress("LongMethod")
private fun E2EApp(viewModel: E2EViewModel) {
  val isInitialized by Clerk.isInitialized.collectAsStateWithLifecycle()
  val user by Clerk.userFlow.collectAsStateWithLifecycle()
  val customOtpState by viewModel.customOtpState.collectAsStateWithLifecycle()
  val oauthState by viewModel.oauthState.collectAsStateWithLifecycle()
  var route by rememberSaveable { mutableStateOf(E2ERoute.Home) }

  LaunchedEffect(customOtpState) {
    if (customOtpState == CustomOtpState.SignedIn) {
      route = E2ERoute.CustomProfile
    }
  }

  LaunchedEffect(oauthState) {
    if (oauthState == OAuthState.SignedIn) {
      route = E2ERoute.PrebuiltProfile
    }
  }

  LaunchedEffect(user?.id, route) {
    if (user == null && (route == E2ERoute.CustomProfile || route == E2ERoute.PrebuiltProfile)) {
      viewModel.resetCustomOtpState()
      viewModel.resetOAuthState()
      route = E2ERoute.Home
    }
  }

  MaterialTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
      when {
        !isInitialized -> LoadingScreen()
        route == E2ERoute.Home ->
          HomeScreen(
            user = user,
            actions =
              HomeActions(
                onCustomOtpSignIn = {
                  viewModel.resetCustomOtpState()
                  route = E2ERoute.CustomOtpSignIn
                },
                onPrebuiltSignIn = { route = E2ERoute.PrebuiltAuth },
                onOAuthSignIn = {
                  viewModel.resetOAuthState()
                  route = E2ERoute.OAuthSignIn
                },
                onCustomProfile = { route = E2ERoute.CustomProfile },
                onPrebuiltProfile = { route = E2ERoute.PrebuiltProfile },
                onSignOut = viewModel::signOut,
              ),
          )
        route == E2ERoute.CustomOtpSignIn ->
          CustomOtpSignInScreen(
            state = customOtpState,
            onSubmitPhone = viewModel::submitCustomOtpPhone,
            onVerifyCode = viewModel::verifyCustomOtpCode,
            onBack = { route = E2ERoute.Home },
          )
        route == E2ERoute.CustomProfile ->
          CustomProfileScreen(user = user, onSignOut = viewModel::signOut)
        route == E2ERoute.OAuthSignIn ->
          OAuthSignInScreen(
            state = oauthState,
            onSignInWithGoogle = viewModel::signInWithGoogleOAuth,
            onBack = { route = E2ERoute.Home },
          )
        route == E2ERoute.PrebuiltAuth ->
          AuthView(
            initialIdentifier = TEST_PHONE_E164,
            persistIdentifiers = false,
            preferGoogleOneTap = false,
            onAuthComplete = { route = E2ERoute.PrebuiltProfile },
          )
        route == E2ERoute.PrebuiltProfile -> UserProfileView(onDismiss = { route = E2ERoute.Home })
      }
    }
  }
}

@Composable
private fun LoadingScreen() {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator()
  }
}

@Composable
private fun HomeScreen(user: User?, actions: HomeActions) {
  E2EColumn {
    Text("Clerk Android E2E", style = MaterialTheme.typography.headlineSmall)
    if (user == null) {
      Button(modifier = Modifier.fillMaxWidth(), onClick = actions.onCustomOtpSignIn) {
        Text("Custom OTP Sign In")
      }
      Button(modifier = Modifier.fillMaxWidth(), onClick = actions.onPrebuiltSignIn) {
        Text("Prebuilt UI Sign In")
      }
      Button(modifier = Modifier.fillMaxWidth(), onClick = actions.onOAuthSignIn) {
        Text("OAuth Sign In")
      }
    } else {
      Text("Already signed in")
      Button(modifier = Modifier.fillMaxWidth(), onClick = actions.onCustomProfile) {
        Text("Open Custom Profile")
      }
      Button(modifier = Modifier.fillMaxWidth(), onClick = actions.onPrebuiltProfile) {
        Text("Open Prebuilt Profile")
      }
      Button(modifier = Modifier.fillMaxWidth(), onClick = actions.onSignOut) { Text("Sign Out") }
    }
  }
}

private data class HomeActions(
  val onCustomOtpSignIn: () -> Unit,
  val onPrebuiltSignIn: () -> Unit,
  val onOAuthSignIn: () -> Unit,
  val onCustomProfile: () -> Unit,
  val onPrebuiltProfile: () -> Unit,
  val onSignOut: () -> Unit,
)

@Composable
private fun CustomOtpSignInScreen(
  state: CustomOtpState,
  onSubmitPhone: (String) -> Unit,
  onVerifyCode: (String) -> Unit,
  onBack: () -> Unit,
) {
  var phoneNumber by rememberSaveable { mutableStateOf("") }
  var code by rememberSaveable { mutableStateOf("") }
  val awaitingCode = state == CustomOtpState.AwaitingCode

  E2EColumn {
    Text("Custom OTP Sign In", style = MaterialTheme.typography.headlineSmall)
    if (state is CustomOtpState.Error) {
      Text(state.message ?: "Something went wrong", color = MaterialTheme.colorScheme.error)
    }
    if (state == CustomOtpState.Loading) {
      CircularProgressIndicator()
    } else if (awaitingCode) {
      OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = code,
        onValueChange = { code = it },
        label = { Text("Verification code") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
      )
      Button(modifier = Modifier.fillMaxWidth(), onClick = { onVerifyCode(code) }) {
        Text("Verify")
      }
    } else {
      OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = phoneNumber,
        onValueChange = { phoneNumber = it },
        label = { Text("Phone number") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
      )
      Button(modifier = Modifier.fillMaxWidth(), onClick = { onSubmitPhone(phoneNumber) }) {
        Text("Continue")
      }
    }
    TextButton(onClick = onBack) { Text("Back to E2E Home") }
  }
}

@Composable
private fun OAuthSignInScreen(
  state: OAuthState,
  onSignInWithGoogle: () -> Unit,
  onBack: () -> Unit,
) {
  E2EColumn {
    Text("OAuth Sign In", style = MaterialTheme.typography.headlineSmall)
    if (state is OAuthState.Error) {
      Text(state.message ?: "Something went wrong", color = MaterialTheme.colorScheme.error)
    }
    if (state == OAuthState.Loading) {
      CircularProgressIndicator()
    } else {
      Button(modifier = Modifier.fillMaxWidth(), onClick = onSignInWithGoogle) {
        Text("Sign in with Google")
      }
    }
    TextButton(onClick = onBack) { Text("Back to E2E Home") }
  }
}

@Composable
private fun CustomProfileScreen(user: User?, onSignOut: () -> Unit) {
  var showDetails by rememberSaveable { mutableStateOf(false) }
  E2EColumn {
    if (showDetails) {
      Text("Custom Profile Details", style = MaterialTheme.typography.headlineSmall)
      Text("User ID")
      Text(user?.id.orEmpty())
      Button(modifier = Modifier.fillMaxWidth(), onClick = { showDetails = false }) {
        Text("Close Details")
      }
    } else {
      Text("Custom Profile", style = MaterialTheme.typography.headlineSmall)
      Text("Signed in with custom flow")
      Button(modifier = Modifier.fillMaxWidth(), onClick = { showDetails = true }) {
        Text("Open Profile Details")
      }
      Button(modifier = Modifier.fillMaxWidth(), onClick = onSignOut) { Text("Sign Out") }
    }
  }
}

@Composable
private fun E2EColumn(content: @Composable ColumnScope.() -> Unit) {
  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 48.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
      content = content,
    )
    Spacer(modifier = Modifier.height(1.dp))
  }
}

private enum class E2ERoute {
  Home,
  CustomOtpSignIn,
  CustomProfile,
  OAuthSignIn,
  PrebuiltAuth,
  PrebuiltProfile,
}
