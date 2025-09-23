package com.clerk.ui.signup.completeprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.api.Clerk
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.common.ClerkThemedAuthScaffold
import com.clerk.ui.core.common.dimens.dp12
import com.clerk.ui.core.common.dimens.dp24
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.progress.ClerkLinearProgressIndicator
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.collections.immutable.toImmutableList

/**
 * Public, production-friendly wrapper that pulls enablement from Clerk and owns its own state. Use
 * [SignUpCompleteProfileView] in the app, and [SignUpCompleteProfileImpl] in previews/tests to
 * inject specific values and flags.
 */
@Composable
fun SignUpCompleteProfileView(progress: Int, modifier: Modifier = Modifier) {
  SignUpCompleteProfileImpl(progress = progress, modifier = modifier)
}

/** Internal enum for focus tracking & label logic. */
internal enum class CompleteProfileField {
  FirstName,
  LastName,
}

/** Hoisted-state composable for previews/tests. You control all values here. */
@Composable
private fun SignUpCompleteProfileImpl(
  progress: Int,
  modifier: Modifier = Modifier,
  firstName: String = "",
  lastName: String = "",
  firstNameEnabled: Boolean = false,
  lastNameEnabled: Boolean = false,
  viewModel: CompleteProfileViewModel = viewModel(),
) {
  // Enablement pulled from Clerk unless explicitly overridden for preview/tests.
  val firstEnabled = Clerk.isFirstNameEnabled || firstNameEnabled
  val lastEnabled = Clerk.isLastNameEnabled || lastNameEnabled

  // Hoisted text state (wired to InputRow)
  var first by rememberSaveable(firstName) { mutableStateOf(firstName) }
  var last by rememberSaveable(lastName) { mutableStateOf(lastName) }

  // VM + error plumbing
  val state by viewModel.state.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val genericErrorMessage = stringResource(R.string.something_went_wrong_please_try_again)
  val errorMessage =
    when (val s = state) {
      is CompleteProfileViewModel.State.Error -> s.message
      else -> null
    }
  LaunchedEffect(errorMessage) {
    errorMessage?.let { snackbarHostState.showSnackbar(it.ifBlank { genericErrorMessage }) }
  }

  // Helper centralizes focus, labels, validation.
  val enabledFields =
    remember(firstEnabled, lastEnabled) {
      CompleteProfileHelper.enabledFields(firstEnabled = firstEnabled, lastEnabled = lastEnabled)
    }
  val helper = rememberCompleteProfileHelper(enabledFields.toImmutableList())

  val isSubmitEnabled by
    remember(first, last, helper, enabledFields) {
      derivedStateOf { helper.isSubmitEnabled(first, last) }
    }

  ClerkThemedAuthScaffold(
    modifier = Modifier.then(modifier),
    title = stringResource(R.string.profile_details),
    subtitle = stringResource(R.string.complete_your_profile),
    snackbarHostState = snackbarHostState,
    hasLogo = false,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(dp24, alignment = Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      ClerkLinearProgressIndicator(progress = progress)

      InputRow(
        firstEnabled = firstEnabled,
        lastEnabled = lastEnabled,
        first = first,
        last = last,
        onFirstChange = { first = it },
        onLastChange = { last = it },
        onFocusChange = { helper.focusTo(it) },
      )

      ClerkButton(
        modifier = Modifier.fillMaxWidth(),
        isEnabled = isSubmitEnabled,
        text = stringResource(helper.submitLabelRes()),
        isLoading = state is CompleteProfileViewModel.State.Loading,
        onClick = { viewModel.updateSignUp(first, last) },
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
  ClerkMaterialTheme {
    SignUpCompleteProfileImpl(
      firstNameEnabled = true,
      lastNameEnabled = true,
      progress = 2,
      firstName = "Ada",
      lastName = "Lovelace",
    )
  }
}

@PreviewLightDark
@Composable
private fun Preview_OnlyFirstEnabled_Empty() {
  ClerkMaterialTheme {
    SignUpCompleteProfileImpl(
      firstNameEnabled = true,
      lastNameEnabled = false,
      progress = 1,
      firstName = "",
      lastName = "",
    )
  }
}

@PreviewLightDark
@Composable
private fun Preview_OnlyLastEnabled_Partial() {
  ClerkMaterialTheme {
    SignUpCompleteProfileImpl(
      firstNameEnabled = false,
      lastNameEnabled = true,
      progress = 3,
      firstName = "",
      lastName = "Ng",
    )
  }
}
