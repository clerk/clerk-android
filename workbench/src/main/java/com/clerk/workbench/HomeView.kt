@file:OptIn(ExperimentalMaterial3Api::class)

package com.clerk.workbench

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.api.session.pendingTaskKey
import com.clerk.ui.userbutton.UserButton

@Composable
fun UserProfileTopBar() {
  val session by Clerk.sessionFlow.collectAsStateWithLifecycle()
  val currentSession = session
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Home screen") },
        actions = {
          if (currentSession != null && currentSession.pendingTaskKey == null) {
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
