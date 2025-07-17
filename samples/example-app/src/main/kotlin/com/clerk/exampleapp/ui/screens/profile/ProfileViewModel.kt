package com.clerk.exampleapp.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.Clerk
import com.clerk.network.serialization.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ProfileViewModel @Inject constructor() : ViewModel() {

  private val _state =
    MutableStateFlow<ProfileAuthenticationState>(ProfileAuthenticationState.SignedIn)
  val state = _state.asStateFlow()

  fun signOut() {
    viewModelScope.launch(Dispatchers.IO) {
      Clerk.signOut().onSuccess {
        withContext(Dispatchers.Main) {
          Log.e("MainViewModel", "Setting authenticationState to SignedOut")
          _state.value = ProfileAuthenticationState.SignedOut
        }
      }
    }
  }
}

sealed interface ProfileAuthenticationState {
  data object SignedIn : ProfileAuthenticationState

  data object SignedOut : ProfileAuthenticationState
}
