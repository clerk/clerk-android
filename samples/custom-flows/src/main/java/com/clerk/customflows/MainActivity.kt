package com.clerk.customflows

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.customflows.emailpassword.mfa.MFASignInActivity
import com.clerk.customflows.emailpassword.signin.EmailPasswordSignInActivity
import com.clerk.customflows.emailpassword.signup.EmailPasswordSignUpActivity
import com.clerk.customflows.forgotpassword.emailaddress.ForgotPasswordEmailViewModel
import com.clerk.customflows.forgotpassword.phone.ForgotPasswordPhoneNumberActivity
import com.clerk.customflows.oauth.OAuthActivity
import com.clerk.customflows.ui.theme.ClerkTheme

class MainActivity : ComponentActivity() {
  val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val state by viewModel.uiState.collectAsStateWithLifecycle()
      ClerkTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (state) {
              MainViewModel.UiState.Loading -> {

                CircularProgressIndicator()
              }

              MainViewModel.UiState.SignedIn -> {
                Button(onClick = { viewModel.signOut() }) {
                  Text(stringResource(R.string.sign_out))
                }
              }

              MainViewModel.UiState.SignedOut ->
                SignedOutContent(modifier = Modifier.padding(innerPadding))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SignedOutContent(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).then(modifier),
    verticalArrangement = Arrangement.spacedBy(24.dp, alignment = Alignment.CenterVertically),
  ) {
    LaunchCustomFlowButton(buttonText = stringResource(R.string.email_password_sign_in)) {
      context.startActivity(Intent(context, EmailPasswordSignInActivity::class.java))
    }
    LaunchCustomFlowButton(buttonText = stringResource(R.string.email_password_sign_up)) {
      context.startActivity(Intent(context, EmailPasswordSignUpActivity::class.java))
    }
    LaunchCustomFlowButton(buttonText = stringResource(R.string.email_password_mfa)) {
      context.startActivity(Intent(context, MFASignInActivity::class.java))
    }
    LaunchCustomFlowButton(buttonText = stringResource(R.string.sign_in_with_oauth)) {
      context.startActivity(Intent(context, OAuthActivity::class.java))
    }
    LaunchCustomFlowButton(buttonText = stringResource(R.string.reset_password_phone)) {
      context.startActivity(Intent(context, ForgotPasswordPhoneNumberActivity::class.java))
    }
    LaunchCustomFlowButton(buttonText = stringResource(R.string.reset_password_email)) {
      context.startActivity(Intent(context, ForgotPasswordEmailViewModel::class.java))
    }
  }
}

@Composable
fun LaunchCustomFlowButton(modifier: Modifier = Modifier, buttonText: String, onClick: () -> Unit) {
  Button(
    shape = RoundedCornerShape(8.dp),
    modifier = Modifier.fillMaxWidth().height(48.dp).then(modifier),
    onClick = onClick,
  ) {
    Text(buttonText)
  }
}
