package com.clerk.exampleapp.ui.common

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.clerk.network.model.environment.UserSettings

@Composable
fun SocialButton(
  socialProvider: UserSettings.SocialConfig,
  modifier: Modifier = Modifier,
  onClick: (UserSettings.SocialConfig) -> Unit,
) {
  Button(
    modifier = Modifier.padding(vertical = 4.dp).height(48.dp).widthIn(120.dp).then(modifier),
    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
    shape = RoundedCornerShape(8.dp),
    onClick = { onClick(socialProvider) },
  ) {
    AsyncImage(
      model = socialProvider.logoUrl,
      contentDescription = socialProvider.name,
      modifier = Modifier.size(24.dp),
    )
  }
}
