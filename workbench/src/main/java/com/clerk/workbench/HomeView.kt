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
import com.clerk.ui.userbutton.UserButton

@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun UserProfileTopBar() {
  val user by Clerk.userFlow.collectAsStateWithLifecycle()
  Scaffold(
    topBar = {
      TopAppBar(title = { Text("Home screen") }, actions = { user?.let { UserButton() } })
    }
  ) {}
}

@PreviewLightDark
@Composable
private fun Preview() {
  UserProfileTopBar()
}
