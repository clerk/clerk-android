package com.clerk.customflows

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clerk.customflows.emailpassword.signin.EmailPasswordSignInActivity
import com.clerk.customflows.emailpassword.signup.EmailPasswordSignUpActivity
import com.clerk.customflows.ui.theme.ClerkTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val context = LocalContext.current
      ClerkTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 24.dp),
            verticalArrangement =
              Arrangement.spacedBy(24.dp, alignment = Alignment.CenterVertically),
          ) {
            LaunchCustomFlowButton(buttonText = stringResource(R.string.email_password_sign_in)) {
              context.startActivity(Intent(context, EmailPasswordSignInActivity::class.java))
            }
            LaunchCustomFlowButton(buttonText = stringResource(R.string.email_password_sign_up)) {
              context.startActivity(Intent(context, EmailPasswordSignUpActivity::class.java))
            }
          }
        }
      }
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
