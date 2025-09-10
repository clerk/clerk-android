package com.clerk.workbench

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.clerk.api.network.model.factor.Factor
import com.clerk.ui.core.avatar.AvatarSize
import com.clerk.ui.core.avatar.OrganizationAvatar
import com.clerk.ui.signin.code.SignInFactorCodeView
import com.clerk.workbench.ui.theme.WorkbenchTheme

class UiActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      WorkbenchTheme {
        Scaffold { innerPadding ->
          SignInFactorCodeView(
            factor = Factor(strategy = "email_code", safeIdentifier = "sam@clerk.dev"),
            modifier = Modifier.padding(innerPadding),
          )
        }
      }
    }
  }
}

@Composable
fun MainContent(modifier: Modifier = Modifier) {
  Box(
    modifier =
      Modifier.background(color = MaterialTheme.colorScheme.background)
        .padding(12.dp)
        .fillMaxSize()
        .then(modifier),
    contentAlignment = Alignment.Center,
  ) {
    OrganizationAvatar(size = AvatarSize.LARGE)
  }
}

@PreviewLightDark
@Composable
private fun PreviewMainContent() {
  WorkbenchTheme { MainContent() }
}
