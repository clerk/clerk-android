package com.clerk.ui.signup.completeprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.Clerk
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.common.ClerkThemedAuthScaffold
import com.clerk.ui.core.common.dimens.dp12
import com.clerk.ui.core.common.dimens.dp24
import com.clerk.ui.core.input.ClerkTextField
import com.clerk.ui.core.progress.ClerkLinearProgressIndicator
import com.clerk.ui.theme.ClerkMaterialTheme

/**
 * Public, production-friendly wrapper that pulls enablement from Clerk and owns its own state. Use
 * [SignUpCompleteProfileView] in the app, and [SignUpCompleteProfileScreen] in previews/tests to
 * inject specific values and flags.
 */
@Composable
fun SignUpCompleteProfileView(progress: Int, modifier: Modifier = Modifier) {
  SignUpCompleteProfileScreen(progress = progress, modifier = modifier)
}

/** Internal enum for focus tracking & label logic. */
private enum class CompleteProfileField {
  FirstName,
  LastName,
}

/** Hoisted-state composable for previews/tests. You control all values here. */
@Composable
fun SignUpCompleteProfileScreen(
  progress: Int,
  modifier: Modifier = Modifier,
  firstName: String = "",
  lastName: String = "",
  firstNameEnabled: Boolean = false,
  lastNameEnabled: Boolean = false,
) {
  val firstEnabled = Clerk.isFirstNameEnabled || firstNameEnabled
  val lastEnabled = Clerk.isLastNameEnabled || lastNameEnabled

  val enabledFields =
    remember(firstEnabled, lastEnabled) {
      buildList {
        if (firstEnabled) add(CompleteProfileField.FirstName)
        if (lastEnabled) add(CompleteProfileField.LastName)
      }
    }

  var currentField by remember {
    mutableStateOf(enabledFields.firstOrNull() ?: CompleteProfileField.FirstName)
  }

  LaunchedEffect(enabledFields) {
    if (currentField !in enabledFields && enabledFields.isNotEmpty()) {
      currentField = enabledFields.first()
    }
  }

  val isSubmitEnabled by
    remember(firstName, lastName, firstEnabled, lastEnabled) {
      derivedStateOf {
        val firstOk = if (firstEnabled) firstName.isNotBlank() else true
        val lastOk = if (lastEnabled) lastName.isNotBlank() else true
        firstOk && lastOk
      }
    }

  val submitLabel =
    remember(currentField, enabledFields) {
      val isLast = enabledFields.lastOrNull() == currentField
      if (isLast) R.string.done else R.string.next
    }

  ClerkThemedAuthScaffold(
    modifier = Modifier.then(modifier),
    title = stringResource(R.string.profile_details),
    subtitle = stringResource(R.string.complete_your_profile),
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
        onFocusChange = { currentField = it },
      )

      ClerkButton(
        modifier = Modifier.fillMaxWidth(),
        isEnabled = isSubmitEnabled,
        text = stringResource(submitLabel),
        onClick = {},
      )
    }
  }
}

@Composable
private fun InputRow(
  firstEnabled: Boolean,
  lastEnabled: Boolean,
  onFocusChange: (CompleteProfileField) -> Unit = {},
) {
  var first by remember { mutableStateOf("") }
  var last by remember { mutableStateOf("") }
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(dp12, alignment = Alignment.CenterHorizontally),
  ) {
    if (firstEnabled) {
      ClerkTextField(
        modifier = Modifier.weight(1f),
        value = first,
        inputContentType = ContentType.PersonFirstName,
        onValueChange = { first = it },
        label = stringResource(R.string.first_name),
        onFocusChange = { onFocusChange(CompleteProfileField.FirstName) },
      )
    }
    if (lastEnabled) {
      ClerkTextField(
        modifier = Modifier.weight(1f),
        value = last,
        inputContentType = ContentType.PersonLastName,
        onValueChange = { last = it },
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
    SignUpCompleteProfileScreen(
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
    SignUpCompleteProfileScreen(
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
    SignUpCompleteProfileScreen(
      firstNameEnabled = false,
      lastNameEnabled = true,
      progress = 3,
      firstName = "",
      lastName = "Ng",
    )
  }
}
