package com.clerk.ui.signup.collectfield

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.button.standard.ClerkButtonDefaults
import com.clerk.ui.core.button.standard.ClerkTextButton
import com.clerk.ui.core.common.ClerkThemedAuthScaffold
import com.clerk.ui.core.common.dimens.dp24
import com.clerk.ui.core.input.ClerkPhoneNumberField
import com.clerk.ui.core.input.ClerkTextField

@Composable
fun SignUpCollectFieldView(
  collectField: CollectField,
  modifier: Modifier = Modifier,
  collectFieldHelper: CollectFieldHelper = CollectFieldHelper(),
) {
  SignUpCollectFieldViewImpl(
    collectField = collectField,
    modifier = modifier,
    collectFieldHelper = collectFieldHelper,
  )
}

@Composable
private fun SignUpCollectFieldViewImpl(
  collectField: CollectField,
  collectFieldHelper: CollectFieldHelper,
  modifier: Modifier = Modifier,
  viewModel: CollectFieldViewModel = viewModel(),
) {

  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var username by remember { mutableStateOf("") }

  val state by viewModel.state.collectAsStateWithLifecycle()

  val continueIsEnabled by remember {
    derivedStateOf {
      when (collectField) {
        CollectField.Email -> email.isNotEmpty()
        CollectField.Password -> password.isNotEmpty()
        CollectField.Phone -> phone.isNotEmpty()
        CollectField.Username -> username.isNotEmpty()
      }
    }
  }

  ClerkThemedAuthScaffold(
    modifier = modifier,
    title = collectFieldHelper.title(collectField),
    subtitle = collectFieldHelper.subtitle(collectField),
    hasLogo = false,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(dp24, alignment = Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      InputField(
        collectField = collectField,
        collectFieldHelper = collectFieldHelper,
        email = email,
        onEmailChange = { email = it },
        password = password,
        onPasswordChange = { password = it },
        phone = phone,
        onPhoneChange = { phone = it },
        username = username,
        onUsernameChange = { username = it },
      )

      ClerkButton(
        text = stringResource(R.string.continue_text),
        onClick = { viewModel.updateSignUp(collectField, email, password, phone, username) },
        modifier = Modifier.fillMaxWidth(),
        isEnabled = continueIsEnabled,
        isLoading = state is CollectFieldViewModel.State.Loading,
        icons = ClerkButtonDefaults.icons(trailingIcon = R.drawable.ic_triangle_right),
      )
      if (collectFieldHelper.fieldIsOptional(collectField)) {
        ClerkTextButton(text = stringResource(R.string.skip)) {}
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
  SignUpCollectFieldView(collectField = CollectField.Password)
}
