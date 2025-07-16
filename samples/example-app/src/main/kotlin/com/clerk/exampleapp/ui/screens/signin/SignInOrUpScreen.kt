package com.clerk.exampleapp.ui.screens.signin

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.exampleapp.R
import com.clerk.exampleapp.navigation.Route
import com.clerk.exampleapp.ui.common.CodeInput
import com.clerk.exampleapp.ui.common.DividerRow
import com.clerk.exampleapp.ui.common.Header
import com.clerk.exampleapp.ui.common.PhoneNumberInput
import com.clerk.exampleapp.ui.common.SocialProviderRow
import com.clerk.exampleapp.ui.theme.ClerkAndroidSDKTypography
import com.clerk.exampleapp.ui.theme.onPrimaryLight
import com.clerk.exampleapp.ui.theme.primaryLight

@Composable
fun SignInOrUpScreen(
  navigateTo: (Route) -> Unit,
  modifier: Modifier = Modifier,
  signInOrUpViewModel: SignInOrUpViewModel = hiltViewModel(),
) {
  var value by remember { mutableStateOf("") }
  val authState by signInOrUpViewModel.state.collectAsStateWithLifecycle()
  val context = LocalContext.current
  var isSignUp by remember { mutableStateOf(true) }

  LaunchedEffect(authState) {
    if (authState is SignInOrUpState.Success) {
      navigateTo(Route.Home)
    } else if (authState is SignInOrUpState.Error) {
      Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show()
    }
  }

  Column(
    modifier =
      Modifier.fillMaxSize().padding(top = 24.dp).padding(horizontal = 16.dp).then(modifier),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Header()
    if (authState is SignInOrUpState.SignedOut) {
      PhoneNumberInput(phoneNumber = value, onValueChange = { value = it })
    } else if (authState is SignInOrUpState.NeedsFirstFactor) {
      CodeInput(value = value, onValueChange = { value = it })
    }

    Button(
      modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(52.dp),
      shape = RoundedCornerShape(8.dp),
      colors = ButtonDefaults.buttonColors(containerColor = primaryLight),
      onClick = {
        if (authState is SignInOrUpState.NeedsFirstFactor) {
          signInOrUpViewModel.verify(code = value)
        } else {
          signInOrUpViewModel.handlePhoneNumber(phoneNumber = value, isSignUp = isSignUp)
          value = ""
        }
      },
    ) {
      Text(
        text = stringResource(R.string.continue_text),
        style = ClerkAndroidSDKTypography.labelLarge,
        color = onPrimaryLight,
      )
    }
    val text =
      if (isSignUp) stringResource(R.string.don_t_have_an_account_sign_up)
      else stringResource(R.string.already_have_an_account_sign_in)
    Text(
      modifier = Modifier.padding(top = 24.dp).clickable { isSignUp = !isSignUp },
      text = text,
      color = primaryLight,
    )
    DividerRow()
    SocialProviderRow(
      onClick = {
        // Clerk handles the sign up transfer flow, so no need to have separate functions for sign
        // in and sign up
        signInOrUpViewModel.authenticateWithRedirect(it)
      }
    )
  }
}
