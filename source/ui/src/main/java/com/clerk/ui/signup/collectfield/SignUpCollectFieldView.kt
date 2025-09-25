package com.clerk.ui.signup.collectfield

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.ui.R
import com.clerk.ui.auth.LocalAuthState
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.common.AuthStateEffects
import com.clerk.ui.core.common.AuthenticationViewState
import com.clerk.ui.core.common.ClerkThemedAuthScaffold
import com.clerk.ui.core.common.dimens.dp24
import com.clerk.ui.core.input.ClerkPhoneNumberField
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.progress.ClerkLinearProgressIndicator

@Composable
fun SignUpCollectFieldView(
  field: CollectField,
  onAuthComplete: () -> Unit,
  modifier: Modifier = Modifier,
  progress: Int = 0,
  collectFieldHelper: CollectFieldHelper = CollectFieldHelper(),
) {
  SignUpCollectFieldViewImpl(
    collectField = field,
    modifier = modifier,
    collectFieldHelper = collectFieldHelper,
    progress = progress,
    onAuthComplete = onAuthComplete,
  )
}

@Composable
private fun SignUpCollectFieldViewImpl(
  collectField: CollectField,
  collectFieldHelper: CollectFieldHelper,
  progress: Int,
  onAuthComplete: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: CollectFieldViewModel = viewModel(),
) {

  val authState = LocalAuthState.current
  val state by viewModel.state.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  AuthStateEffects(authState, state, snackbarHostState, onAuthComplete) { viewModel.resetState() }

  val continueIsEnabled by
    remember(collectField) {
      derivedStateOf {
        when (collectField) {
          CollectField.Email -> authState.signUpEmail.isNotEmpty()
          CollectField.Password -> authState.signUpPassword.isNotEmpty()
          CollectField.Phone -> authState.signUpPhoneNumber.isNotEmpty()
          CollectField.Username -> authState.signUpUsername.isNotEmpty()
        }
      }
    }

  ClerkThemedAuthScaffold(
    modifier = modifier,
    title = collectFieldHelper.title(collectField),
    subtitle = collectFieldHelper.subtitle(collectField),
    hasLogo = false,
    snackbarHostState = snackbarHostState,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(dp24, alignment = Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      ClerkLinearProgressIndicator(progress = progress)
      InputField(
        collectField = collectField,
        collectFieldHelper = collectFieldHelper,
        email = authState.signUpEmail,
        onEmailChange = { authState.signUpEmail = it },
        password = authState.signUpPassword,
        onPasswordChange = { authState.signUpPassword = it },
        phone = authState.signUpPhoneNumber,
        onPhoneChange = { authState.signUpPhoneNumber = it },
        username = authState.signUpUsername,
        onUsernameChange = { authState.signUpUsername = it },
      )

      ClerkButton(
        text = stringResource(R.string.continue_text),
        onClick = {
          viewModel.updateSignUp(
            collectField,
            email = authState.signUpEmail,
            password = authState.signUpPassword,
            phone = authState.signUpPhoneNumber,
            username = authState.signUpUsername,
          )
        },
        modifier = Modifier.fillMaxWidth(),
        isEnabled = continueIsEnabled,
        isLoading = state is AuthenticationViewState.Loading,
        icons = ClerkButtonDefaults.icons(trailingIcon = R.drawable.ic_triangle_right),
      )
      if (collectFieldHelper.fieldIsOptional(collectField)) {
        ClerkTextButton(text = stringResource(R.string.skip), onClick = {})
      }
    }
  }
}

@Composable
private fun InputField(
  collectField: CollectField,
  collectFieldHelper: CollectFieldHelper,
  email: String,
  onEmailChange: (String) -> Unit,
  password: String,
  onPasswordChange: (String) -> Unit,
  phone: String,
  onPhoneChange: (String) -> Unit,
  username: String,
  onUsernameChange: (String) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    when (collectField) {
      CollectField.Email -> {
        ClerkTextField(
          value = email,
          onValueChange = onEmailChange,
          label = collectFieldHelper.label(collectField),
          inputContentType = ContentType.EmailAddress,
        )
      }

      CollectField.Password -> {
        ClerkTextField(
          value = password,
          onValueChange = onPasswordChange,
          label = collectFieldHelper.label(collectField),
          inputContentType = ContentType.Password,
          visualTransformation = PasswordVisualTransformation(),
        )
      }

      CollectField.Phone -> {
        ClerkPhoneNumberField(value = phone, onValueChange = onPhoneChange)
      }

      CollectField.Username -> {
        ClerkTextField(
          value = username,
          onValueChange = onUsernameChange,
          label = collectFieldHelper.label(collectField),
          inputContentType = ContentType.Username,
        )
      }
    }
  }
}

enum class CollectField(val rawValue: String) {

  Email(rawValue = "email_address"),
  Password("password"),
  Phone("phone_number"),
  Username("username"),
}

@PreviewLightDark
@Composable
private fun Preview() {
  PreviewAuthStateProvider {
    SignUpCollectFieldView(field = CollectField.Password, onAuthComplete = {})
  }
}
