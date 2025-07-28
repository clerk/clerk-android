package com.clerk.linearclone.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clerk.linearclone.ui.button.LinearCloneButton
import com.clerk.linearclone.ui.theme.PrimaryPurple
import com.clerk.linearclone.ui.theme.PrimaryWhite

@Composable
fun HomeScreen(modifier: Modifier = Modifier, onSignOutClick: () -> Unit) {
  Box(
    modifier = Modifier.fillMaxSize().padding(horizontal = 42.dp).then(modifier),
    contentAlignment = Alignment.Center,
  ) {
    LinearCloneButton(
      backgroundColor = PrimaryPurple,
      onClick = onSignOutClick,
      buttonText = "Sign out",
      textColor = PrimaryWhite,
    )
  }
}
