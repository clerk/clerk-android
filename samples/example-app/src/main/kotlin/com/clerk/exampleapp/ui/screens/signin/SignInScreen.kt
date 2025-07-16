package com.clerk.exampleapp.ui.screens.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.clerk.Clerk
import com.clerk.exampleapp.AuthenticationState
import com.clerk.exampleapp.R
import com.clerk.exampleapp.navigation.Route
import com.clerk.exampleapp.ui.theme.ClerkAndroidSDKTypography
import com.clerk.exampleapp.ui.theme.onPrimaryLight
import com.clerk.exampleapp.ui.theme.primaryLight
import com.clerk.exampleapp.ui.theme.secondaryLight

@Composable
fun SignInScreen(
  state: AuthenticationState,
  navigateTo: (Route) -> Unit,
  modifier: Modifier = Modifier,
) {
  var emailAddress by remember { mutableStateOf("") }
  Column(
    modifier =
      Modifier.fillMaxSize().padding(top = 24.dp).padding(horizontal = 16.dp).then(modifier),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
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
    OutlinedTextField(
      modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
      value = emailAddress,
      onValueChange = { emailAddress = it },
      label = { Text(stringResource(R.string.enter_your_email)) },
      shape = RoundedCornerShape(8.dp),
    )
    Button(
      modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(52.dp),
      shape = RoundedCornerShape(8.dp),
      colors = ButtonDefaults.buttonColors(containerColor = primaryLight),
      onClick = {},
    ) {
      Text(
        text = stringResource(R.string.continue_text),
        style = ClerkAndroidSDKTypography.labelLarge,
        color = onPrimaryLight,
      )
    }
    DividerRow()
    FlowRow(
      modifier = Modifier.padding(top = 32.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Clerk.socialProviders.forEach {
        SocialButton(
          imageUrl = it.value.logoUrl,
          providerName = it.value.name,
          modifier = Modifier,
          onClick = {},
        )
      }
    }
  }
}

@Composable
fun DividerRow(modifier: Modifier = Modifier) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 32.dp).then(modifier),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    HorizontalDivider(
      modifier = Modifier.weight(1f),
      thickness = 1.dp,
      color = MaterialTheme.colorScheme.primary.copy(alpha = .2f),
    )

    Text(
      modifier = Modifier.padding(horizontal = 16.dp),
      text = "or",
      style = MaterialTheme.typography.titleSmall,
      color = Color.LightGray,
    )

    HorizontalDivider(
      modifier = Modifier.weight(1f),
      thickness = 1.dp,
      color = MaterialTheme.colorScheme.primary.copy(alpha = .2f),
    )
  }
}

@Composable
fun SocialButton(
  imageUrl: String?,
  providerName: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  Button(
    modifier = Modifier.padding(vertical = 4.dp).height(48.dp).widthIn(120.dp).then(modifier),
    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
    shape = RoundedCornerShape(8.dp),
    onClick = onClick,
  ) {
    AsyncImage(model = imageUrl, contentDescription = providerName, modifier = Modifier.size(24.dp))
  }
}
