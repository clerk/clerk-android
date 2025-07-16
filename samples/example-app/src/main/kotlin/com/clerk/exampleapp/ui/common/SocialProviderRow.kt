package com.clerk.exampleapp.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clerk.Clerk
import com.clerk.network.model.environment.UserSettings

@Composable
fun SocialProviderRow(modifier: Modifier = Modifier, onClick: (UserSettings.SocialConfig) -> Unit) {
  FlowRow(
    modifier = Modifier.padding(top = 32.dp).then(modifier),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Clerk.socialProviders.forEach {
      SocialButton(socialProvider = it.value, modifier = Modifier, onClick = onClick)
    }
  }
}
