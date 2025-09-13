package com.clerk.workbench

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.signin.help.SignInGetHelpView
import com.clerk.workbench.ui.theme.WorkbenchTheme

class UiActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { WorkbenchTheme { MainContent() } }
  }
}

@Composable
private fun MainContent() {
  SignInGetHelpView {}
}

@PreviewLightDark
@Composable
private fun PreviewMainContent() {
  WorkbenchTheme { MainContent() }
}
