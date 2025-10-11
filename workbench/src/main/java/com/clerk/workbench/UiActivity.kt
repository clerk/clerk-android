package com.clerk.workbench

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.user.createPasskey
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.userprofile.email.UserProfileEmailRow
import com.clerk.workbench.ui.theme.WorkbenchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UiActivity : ComponentActivity() {
  val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      WorkbenchTheme {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        Box(
          modifier =
            Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.background),
          contentAlignment = Alignment.Center,
        ) {
          when (state) {
            MainViewModel.UiState.Loading -> CircularProgressIndicator()
            MainViewModel.UiState.SignedIn -> {
              Column {
                ClerkButton(
                  text = "Sign out",
                  onClick = { scope.launch(Dispatchers.IO) { Clerk.signOut() } },
                )
                Spacer(modifier = Modifier.height(8.dp))
                ClerkButton(
                  text = "Create passkey",
                  onClick = { scope.launch(Dispatchers.IO) { Clerk.user?.createPasskey() } },
                )
              }
            }
            MainViewModel.UiState.SignedOut -> {
              MainContent()
            }
          }
        }
      }
    }
  }

  @Suppress("MagicNumber")
  @Composable
  private fun MainContent() {
    val color = if (isSystemInDarkTheme()) Color(0xFF1A1A1D) else Color(0xFFF9F9F9)
    Column(modifier = Modifier.background(color = color).padding(vertical = dp24)) {
      UserProfileEmailRow(
        isPrimary = true,
        emailAddress =
          EmailAddress(
            id = "123",
            emailAddress = "user@example.com",
            verification = Verification(status = Verification.Status.VERIFIED),
          ),
      )
      UserProfileEmailRow(
        isPrimary = false,
        emailAddress =
          EmailAddress(
            id = "123",
            emailAddress = "user@example.com",
            verification = Verification(status = Verification.Status.UNVERIFIED),
          ),
      )
      UserProfileEmailRow(
        isPrimary = false,
        emailAddress =
          EmailAddress(
            id = "123",
            emailAddress = "user@example.com",
            linkedTo = listOf(EmailAddress.LinkedEntity(id = "1", type = "email")),
            verification = Verification(status = Verification.Status.VERIFIED),
          ),
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun PreviewMainContent() {
  WorkbenchTheme {}
}
