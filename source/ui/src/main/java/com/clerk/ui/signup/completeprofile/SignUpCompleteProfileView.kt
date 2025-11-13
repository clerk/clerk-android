package com.clerk.ui.signup.completeprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.ui.R
import com.clerk.ui.auth.AuthStateEffects
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.auth.LocalAuthState
import com.clerk.ui.auth.PreviewAuthStateProvider
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.scaffold.ClerkThemedAuthScaffold
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.collections.immutable.toImmutableList

/**
 * Public, production-friendly wrapper that pulls enablement from Clerk and owns its own state. Use
 * [SignUpCompleteProfileView] in the app, and [SignUpCompleteProfileImpl] in previews/tests to
 * inject specific values and flags.
 */
@Composable
fun SignUpCompleteProfileView(modifier: Modifier = Modifier, onAuthComplete: () -> Unit) {
  SignUpCompleteProfileImpl(modifier = modifier, onAuthComplete = onAuthComplete)
}

/** Internal enum for focus tracking & label logic. */
internal enum class CompleteProfileField {
  FirstName,
  LastName,
}

/** Hoisted-state composable for previews/tests. You control all values here. */
@Composable
private fun SignUpCompleteProfileImpl(
  onAuthComplete: () -> Unit,
  modifier: Modifier = Modifier,
  firstName: String = "",
  lastName: String = "",
  firstNameEnabled: Boolean = false,
  lastNameEnabled: Boolean = false,
  viewModel: CompleteProfileViewModel = viewModel(),
) {
  val authState = LocalAuthState.current
  val firstEnabled = Clerk.isFirstNameEnabled || firstNameEnabled
  val lastEnabled = Clerk.isLastNameEnabled || lastNameEnabled

  authState.signUpFirstName = firstName
  authState.signUpLastName = lastName

  val state by viewModel.state.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  AuthStateEffects(authState, state, snackbarHostState, onAuthComplete) { viewModel.resetState() }

  val enabledFields =
    remember(firstEnabled, lastEnabled) {
      CompleteProfileHelper.enabledFields(firstEnabled = firstEnabled, lastEnabled = lastEnabled)
    }
  val helper = rememberCompleteProfileHelper(enabledFields.toImmutableList())

  val isSubmitEnabled by
    remember(authState.signUpFirstName, authState.signUpLastName, helper, enabledFields) {
      derivedStateOf { helper.isSubmitEnabled(authState.signUpFirstName, authState.signUpLastName) }
    }

  ClerkThemedAuthScaffold(
    modifier = Modifier.then(modifier),
    title = stringResource(R.string.profile_details),
    subtitle = stringResource(R.string.complete_your_profile),
    snackbarHostState = snackbarHostState,
    onClickIdentifier = { authState.clearBackStack() },
    onBackPressed = { authState.navigateBack() },
    hasLogo = false,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(dp24, alignment = Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      InputRow(
        firstEnabled = firstEnabled,
        lastEnabled = lastEnabled,
        first = authState.signUpFirstName,
        last = authState.signUpLastName,
        onFirstChange = { authState.signUpFirstName = it },
        onLastChange = { authState.signUpLastName = it },
        onFocusChange = { helper.focusTo(it) },
      )

      ClerkButton(
        modifier = Modifier.fillMaxWidth(),
        isEnabled = isSubmitEnabled,
        text = stringResource(helper.submitLabelRes()),
        isLoading = state is AuthenticationViewState.Loading,
        onClick = { viewModel.updateSignUp(authState.signUpFirstName, authState.signUpLastName) },
      )
    }
  }
}

@Composable
private fun InputRow(
  firstEnabled: Boolean,
  lastEnabled: Boolean,
  first: String,
  last: String,
  onFirstChange: (String) -> Unit,
  onLastChange: (String) -> Unit,
  onFocusChange: (CompleteProfileField) -> Unit = {},
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(dp12, alignment = Alignment.CenterHorizontally),
  ) {
    if (firstEnabled) {
      ClerkTextField(
        modifier = Modifier.weight(1f),
        value = first,
        inputContentType = ContentType.PersonFirstName,
        onValueChange = onFirstChange,
        label = stringResource(R.string.first_name),
        onFocusChange = { onFocusChange(CompleteProfileField.FirstName) },
      )
    }
    if (lastEnabled) {
      ClerkTextField(
        modifier = Modifier.weight(1f),
        value = last,
        inputContentType = ContentType.PersonLastName,
        onValueChange = onLastChange,
        label = stringResource(R.string.last_name),
        onFocusChange = { onFocusChange(CompleteProfileField.LastName) },
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview_BothEnabled_Filled() {
  PreviewAuthStateProvider {
    ClerkMaterialTheme {
      SignUpCompleteProfileImpl(
        firstNameEnabled = true,
        lastNameEnabled = true,
        firstName = "Cal",
        lastName = "Raleigh",
        onAuthComplete = {},
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview_OnlyFirstEnabled_Empty() {
  PreviewAuthStateProvider {
    ClerkMaterialTheme {
      SignUpCompleteProfileImpl(
        firstNameEnabled = true,
        lastNameEnabled = false,
        firstName = "",
        lastName = "",
        onAuthComplete = {},
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun Preview_OnlyLastEnabled_Partial() {
  PreviewAuthStateProvider {
    ClerkMaterialTheme {
      SignUpCompleteProfileImpl(
        firstNameEnabled = false,
        lastNameEnabled = true,
        firstName = "",
        lastName = "Daniels",
        onAuthComplete = {},
      )
    }
  }
}
