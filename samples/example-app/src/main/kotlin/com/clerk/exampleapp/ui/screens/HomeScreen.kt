package com.clerk.exampleapp.ui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
  Text(modifier = Modifier.then(modifier), text = "Home Screen")
}
