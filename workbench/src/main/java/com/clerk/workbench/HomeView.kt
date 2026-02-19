@file:OptIn(ExperimentalMaterial3Api::class)

package com.clerk.workbench

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.api.session.requiresForcedMfa
import com.clerk.ui.userbutton.UserButton

@Composable
fun UserProfileTopBar() {
  val session by Clerk.sessionFlow.collectAsStateWithLifecycle()
  val user by Clerk.userFlow.collectAsStateWithLifecycle()
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Home screen") },
        actions = {
          if (user != null && session?.requiresForcedMfa != true) {
            UserButton()
          }
        },
      )
    }
  ) {}
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileTopBar()
}
