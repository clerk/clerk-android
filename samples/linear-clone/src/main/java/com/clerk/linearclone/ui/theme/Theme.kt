package com.clerk.linearclone.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
  darkColorScheme(primary = PrimaryBlack, secondary = PrimaryGrey, tertiary = TertiaryGrey)

@Composable
fun LinearCloneTheme(content: @Composable () -> Unit) {

  MaterialTheme(colorScheme = DarkColorScheme, typography = Typography, content = content)
}
