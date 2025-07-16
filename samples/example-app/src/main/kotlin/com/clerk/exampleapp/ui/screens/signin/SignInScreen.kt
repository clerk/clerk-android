package com.clerk.exampleapp.ui.screens.signin

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.clerk.Clerk
import com.clerk.exampleapp.R
import com.clerk.exampleapp.navigation.Route
import com.clerk.exampleapp.ui.theme.ClerkAndroidSDKTypography
import com.clerk.exampleapp.ui.theme.onPrimaryLight
import com.clerk.exampleapp.ui.theme.primaryLight
import com.clerk.exampleapp.ui.theme.secondaryLight
import com.clerk.exampleapp.utils.NanpVisualTransformation
import com.clerk.network.model.environment.UserSettings

@Composable
fun SignInScreen(
  navigateTo: (Route) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SignInViewModel = hiltViewModel(),
) {
  var value by remember { mutableStateOf("") }
  val state by viewModel.state.collectAsStateWithLifecycle()
  val context = LocalContext.current

  LaunchedEffect(state) {
    if (state is SignInState.SignInSuccess) {
      navigateTo(Route.Home)
    } else if (state is SignInState.Error) {
      Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show()
    }
  }

  Column(
    modifier =
      Modifier.fillMaxSize().padding(top = 24.dp).padding(horizontal = 16.dp).then(modifier),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Header()
    if (state is SignInState.SignedOut) {
      PhoneNumberInput(phoneNumber = value, onValueChange = { value = it })
    } else if (state is SignInState.NeedsFirstFactor) {
      CodeInput(value = value, onValueChange = { value = it })
    }

    Button(
      modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(52.dp),
      shape = RoundedCornerShape(8.dp),
      colors = ButtonDefaults.buttonColors(containerColor = primaryLight),
      onClick = {
        if (state is SignInState.SignedOut) {
          viewModel.createSignIn(value)
        } else if (state is SignInState.NeedsFirstFactor) {
          viewModel.verify(value)
        }
      },
    ) {
      Text(
        text = stringResource(R.string.continue_text),
        style = ClerkAndroidSDKTypography.labelLarge,
        color = onPrimaryLight,
      )
    }
    Text(
      modifier = Modifier.padding(top = 24.dp).clickable { navigateTo(Route.SignUp) },
      text = stringResource(R.string.don_t_have_an_account_sign_up),
      color = primaryLight,
    )
    DividerRow()
    SocialProviderRow(onClick = { viewModel.authenticateWithRedirect(it) })
  }
}

@Composable
private fun CodeInput(value: String, onValueChange: (String) -> Unit) {
  OutlinedTextField(
    modifier = Modifier.fillMaxWidth().padding(top = 32.dp).semantics { ContentType.SmsOtpCode },
    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
    value = value,
    onValueChange = { onValueChange(it) },
    label = { Text(text = stringResource(R.string.enter_your_phone_number)) },
    shape = RoundedCornerShape(8.dp),
    colors =
      OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = primaryLight,
        focusedBorderColor = primaryLight,
        focusedLabelColor = primaryLight,
      ),
  )
}

@Composable
private fun PhoneNumberInput(phoneNumber: String, onValueChange: (String) -> Unit) {
  OutlinedTextField(
    modifier = Modifier.fillMaxWidth().padding(top = 32.dp).semantics { ContentType.PhoneNumber },
    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone),
    value = phoneNumber,
    onValueChange = { if (it.length <= 10) onValueChange(it) },
    visualTransformation = NanpVisualTransformation(),
    label = { Text(text = stringResource(R.string.enter_your_phone_number)) },
    shape = RoundedCornerShape(8.dp),
    colors =
      OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = primaryLight,
        focusedBorderColor = primaryLight,
        focusedLabelColor = primaryLight,
      ),
  )
}

@Composable
private fun SocialProviderRow(onClick: (UserSettings.SocialConfig) -> Unit) {
  FlowRow(
    modifier = Modifier.padding(top = 32.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Clerk.socialProviders.forEach {
      SocialButton(socialProvider = it.value, modifier = Modifier, onClick = onClick)
    }
  }
}

@Composable
private fun Header() {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
