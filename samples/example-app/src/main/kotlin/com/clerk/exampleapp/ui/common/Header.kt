package com.clerk.exampleapp.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.clerk.Clerk
import com.clerk.exampleapp.R
import com.clerk.exampleapp.ui.theme.ClerkAndroidSDKTypography
import com.clerk.exampleapp.ui.theme.secondaryLight

@Composable
fun Header(modifier: Modifier = Modifier) {
  Column(modifier = Modifier.then(modifier), horizontalAlignment = Alignment.CenterHorizontally) {
    AsyncImage(
      modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
      model = Clerk.logoUrl,
      contentDescription = stringResource(R.string.application_logo),
    )
    Text(
      modifier = Modifier.padding(top = 24.dp),
      text = stringResource(R.string.continue_to, Clerk.applicationName!!),
      style = ClerkAndroidSDKTypography.titleLarge,
    )
    Text(
      modifier = Modifier.padding(top = 8.dp),
      text = stringResource(R.string.welcome_back_please_sign_in_to_continue),
      style = ClerkAndroidSDKTypography.titleSmall,
      color = secondaryLight,
    )
  }
}
