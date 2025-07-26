package com.clerk.linearclone.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(primary = Color.Black, secondary = PrimaryGrey, tertiary = Color.White)

@Composable
fun ClerkTheme(content: @Composable () -> Unit) {

  MaterialTheme(colorScheme = DarkColorScheme, typography = Typography, content = content)
}
