package com.clerk.ui.userprofile.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import kotlinx.coroutines.launch

class UserProfileAccountViewModel : ViewModel() {

  fun signOut() {
    viewModelScope.launch { Clerk.signOut() }
  }
}
