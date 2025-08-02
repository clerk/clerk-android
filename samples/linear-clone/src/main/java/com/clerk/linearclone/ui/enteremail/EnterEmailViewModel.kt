package com.clerk.linearclone.ui.enteremail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EnterEmailViewModel : ViewModel() {

  private val _uiState = MutableStateFlow<UiState>(UiState.SignedOut)
  val uiState = _uiState.asStateFlow()

  fun prepareEmailVerification(email: String) {
    viewModelScope.launch(Dispatchers.IO) {
      SignIn.create(SignIn.CreateParams.Strategy.EmailCode(identifier = email))
        .onSuccess {
          withContext(Dispatchers.Main) { _uiState.value = UiState.NeedsEmailCode(email) }
        }
        .onFailure {
          withContext(Dispatchers.Main) {
            Log.e("EnterEmailViewModel", "Error preparing email verification", it.throwable)
            _uiState.value = UiState.Error
          }
        }
    }
  }

  sealed interface UiState {
    data object SignedOut : UiState

    data class NeedsEmailCode(val email: String) : UiState

    data object Error : UiState
  }
}
