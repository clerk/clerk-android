@file:OptIn(ExperimentalMaterial3Api::class)

package com.clerk.workbench

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.core.composition.LocalClerk
import com.clerk.ui.userbutton.UserButton

@Composable
fun UserProfileTopBar() {

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Home screen") },
        actions = {
          if (LocalClerk.currentOrNull != null && LocalClerk.isAuthFlowComplete) {
            UserButton()
          }
        },
      )
    }
  ) { innerPadding ->
    Box(modifier = Modifier.padding(innerPadding))
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileTopBar()
}
