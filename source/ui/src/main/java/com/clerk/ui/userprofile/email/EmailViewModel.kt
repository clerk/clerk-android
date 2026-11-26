package com.clerk.ui.userprofile.email

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.emailaddress.delete
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.user.User
import com.clerk.api.user.update
import com.clerk.ui.core.common.guardUser
import com.clerk.ui.reverification.isReverificationCancelled
import com.clerk.ui.reverification.reverify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val EMAIL_IMMUTABLE_MESSAGE =
  "Email addresses cannot be changed for this application."

internal class EmailViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun setAsPrimary(emailAddress: EmailAddress) {
    if (Clerk.isEmailImmutable) {
      _state.value = State.Failure(EMAIL_IMMUTABLE_MESSAGE)
      return
    }
    _state.value = State.Loading
    guardUser(userDoesNotExist = {}) { user ->
      viewModelScope.launch(Dispatchers.IO) {
        Clerk.reverify { user.update(User.UpdateParams(primaryEmailAddressId = emailAddress.id)) }
          .onSuccess { _state.value = State.SetAsPrimary.Success }
          .onFailure { _state.value = failureState(it) }
      }
    }
  }

  fun remove(emailAddress: EmailAddress) {
    if (Clerk.isEmailImmutable) {
      _state.value = State.Failure(EMAIL_IMMUTABLE_MESSAGE)
      return
    }
    _state.value = State.Loading
    viewModelScope.launch(Dispatchers.IO) {
      Clerk.reverify { emailAddress.delete() }
        .onFailure { _state.value = failureState(it) }
        .onSuccess { _state.value = State.Remove.Success }
    }
  }

  private fun failureState(failure: ClerkResult.Failure<ClerkErrorResponse>): State =
    if (failure.isReverificationCancelled) State.Idle else State.Failure(failure.errorMessage)

  fun clearError(reported: State.Failure) {
    _state.compareAndSet(reported, State.Idle)
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data class Failure(val message: String) : State

    sealed interface SetAsPrimary : State {
      data object Success : SetAsPrimary
    }

    sealed interface Remove : State {
      data object Success : Remove
    }
  }
}
