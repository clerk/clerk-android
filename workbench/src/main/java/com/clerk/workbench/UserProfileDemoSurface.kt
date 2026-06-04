package com.clerk.workbench

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.clerk.workbench.ui.theme.Background
import com.clerk.workbench.ui.theme.BackgroundDark

@Composable
internal fun UserProfileDemoSurface(content: @Composable () -> Unit) {
  val backgroundColor = if (isSystemInDarkTheme()) BackgroundDark else Background
  Column(modifier = Modifier.fillMaxSize().background(color = backgroundColor)) {
    WorkbenchAuthGate(persistIdentifiers = false) {
      Column(modifier = Modifier.background(color = backgroundColor).fillMaxSize()) { content() }
    }
  }
}
