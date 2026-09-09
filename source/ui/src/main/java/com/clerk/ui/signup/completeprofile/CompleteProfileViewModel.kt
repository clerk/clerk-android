package com.clerk.ui.signup.completeprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.auth.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal const val FIRST_NAME_FIELD = "first_name"
internal const val LAST_NAME_FIELD = "last_name"

internal class CompleteProfileViewModel(private val clerk: Clerk) : ViewModel() {
  private val _state = MutableStateFlow<AuthenticationViewState>(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  fun updateSignUp(firstName: String?, lastName: String?, legalAccepted: Boolean? = null) =
    guardSignUp(clerk, _state) { signUp ->
      _state.value = AuthenticationViewState.Loading
      viewModelScope.launch {
        runUiOperation {
            signUp.update(signUp.completeProfileUpdateParams(firstName, lastName, legalAccepted))
          }
          .onSuccess { _state.value = AuthenticationViewState.Success.SignUp(signUp) }
          .onFailure { _state.value = AuthenticationViewState.Error(it.displayMessage) }
      }
    }

  fun resetState() {
    _state.value = AuthenticationViewState.Idle
  }
}

internal fun SignUp.completeProfileUpdateParams(
  firstName: String?,
  lastName: String?,
  legalAccepted: Boolean?,
): SignUpUpdateParams =
  SignUpUpdateParams(
    firstName = firstName.takeIf { supportsField(FIRST_NAME_FIELD) && !it.isNullOrBlank() },
    lastName = lastName.takeIf { supportsField(LAST_NAME_FIELD) && !it.isNullOrBlank() },
    legalAccepted = legalAccepted,
  )

internal fun SignUp.supportsField(field: String): Boolean = requiredFields.any {
  it.rawValue == field
}
