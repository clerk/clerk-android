package com.clerk.ui.signup.completeprofile

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.clerk.api.Clerk
import com.clerk.ui.R

internal class CompleteProfileHelper(
  private val firstName: String,
  private val lastName: String,
  private val firstNameEnabled: Boolean,
  private val lastNameEnabled: Boolean,
  private val state: CompleteProfileViewModel.State,
  private val snackbarHostState: SnackbarHostState,
) {
  val firstEnabled = Clerk.isFirstNameEnabled || firstNameEnabled
  val lastEnabled = Clerk.isLastNameEnabled || lastNameEnabled

  val enabledFields: List<CompleteProfileField> = buildList {
    if (firstEnabled) add(CompleteProfileField.FirstName)
    if (lastEnabled) add(CompleteProfileField.LastName)
  }

  var currentField by mutableStateOf(enabledFields.firstOrNull() ?: CompleteProfileField.FirstName)
    private set

  fun setCurrentField(field: CompleteProfileField) {
    currentField = field
  }

  fun onFieldsChanged() {
    if (currentField !in enabledFields && enabledFields.isNotEmpty()) {
      currentField = enabledFields.first()
    }
  }

  val isSubmitEnabled: Boolean
    get() {
      val firstOk = if (firstEnabled) firstName.isNotBlank() else true
      val lastOk = if (lastEnabled) lastName.isNotBlank() else true
      return firstOk && lastOk
    }

  fun submitLabel(): Int {
    val isLast = enabledFields.lastOrNull() == currentField
    return if (isLast) R.string.done else R.string.next
  }

  suspend fun maybeShowError(genericErrorMessage: String) {
    val errorMessage = (state as? CompleteProfileViewModel.State.Error)?.message
    errorMessage?.let { snackbarHostState.showSnackbar(it.ifBlank { genericErrorMessage }) }
  }
}

@Composable
internal fun rememberCompleteProfileHelper(
  firstName: String,
  lastName: String,
  firstNameEnabled: Boolean,
  lastNameEnabled: Boolean,
  state: CompleteProfileViewModel.State,
  snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
): CompleteProfileHelper {
  val helper =
    remember(firstName, lastName, firstNameEnabled, lastNameEnabled, state) {
      CompleteProfileHelper(
        firstName,
        lastName,
        firstNameEnabled,
        lastNameEnabled,
        state,
        snackbarHostState,
      )
    }

  // Keep currentField valid when enabledFields changes
  LaunchedEffect(helper.enabledFields) { helper.onFieldsChanged() }

  // Show errors
  val genericErrorMessage = stringResource(R.string.something_went_wrong_please_try_again)
  LaunchedEffect(state) { helper.maybeShowError(genericErrorMessage) }

  return helper
}
