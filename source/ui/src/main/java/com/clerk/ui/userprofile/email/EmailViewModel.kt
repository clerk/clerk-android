package com.clerk.ui.userprofile.email

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.emailaddress.delete
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.user.User
import com.clerk.api.user.update
import com.clerk.ui.core.common.guardUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmailViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun setAsPrimary(emailAddress: EmailAddress) {
    _state.value = State.Loading
    guardUser(userDoesNotExist = {}) { user ->
      viewModelScope.launch(Dispatchers.IO) {
        user
          .update(User.UpdateParams(primaryEmailAddressId = emailAddress.id))
          .onSuccess { _state.value = State.SetAsPrimary.Success }
          .onFailure { _state.value = State.Failure(it.errorMessage) }
      }
    }
  }

  fun remove(emailAddress: EmailAddress) {
    _state.value = State.Loading
    viewModelScope.launch(Dispatchers.IO) {
      emailAddress
        .delete()
        .onFailure { _state.value = State.Failure(it.errorMessage) }
        .onSuccess { _state.value = State.Remove.Success }
    }
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
