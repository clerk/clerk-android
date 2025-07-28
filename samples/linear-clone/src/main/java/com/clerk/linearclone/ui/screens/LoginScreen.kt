package com.clerk.linearclone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen() {
  Column(
    modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.primary),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = Icons.Default.AccountCircle,
      contentDescription = null,
      modifier = Modifier.size(80.dp).padding(bottom = 24.dp),
      tint = MaterialTheme.colorScheme.primary,
    )

    Text(
      text = "Sign in to continue",
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(bottom = 8.dp),
    )

    Text(
      text = "Access your Linear Clone workspace",
      fontSize = 16.sp,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(bottom = 48.dp),
    )

    OutlinedButton(
      onClick = {},
      modifier = Modifier.fillMaxWidth().height(48.dp),
      border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
    ) {
      Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(text = "🔍", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
        Text(text = "Sign in with Google", fontSize = 16.sp, fontWeight = FontWeight.Medium)
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    TextButton(onClick = {}) {
      Text(text = "Other sign-in options", color = MaterialTheme.colorScheme.primary)
    }
  }
}
