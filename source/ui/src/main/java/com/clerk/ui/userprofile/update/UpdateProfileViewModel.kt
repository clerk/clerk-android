package com.clerk.ui.userprofile.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class UpdateProfileViewModel(private val clerk: Clerk) : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun removeProfileImage() {
    val user = clerk.user
    if (user == null) {
      _state.value = State.Error("User not authenticated")
      return
    }
    perform(errorPrefix = "Failed to delete profile image: ") {
      user.setProfileImage(SetProfileImageParams(file = null))
      user.reload()
    }
  }

  fun uploadProfileImage(file: File) = perform {
    val user = clerk.user ?: throw CoreException("no_user", "User not authenticated")
    val upload =
      withContext(Dispatchers.IO) {
        val contentType =
          java.net.URLConnection.guessContentTypeFromName(file.name) ?: "application/octet-stream"
        UploadFile(file.name, contentType, file.readBytes())
      }
    user.setProfileImage(SetProfileImageParams(SetOrganizationLogoParamsFile.Case2(upload)))
    user.reload()
  }

  fun save(firstName: String?, lastName: String?, username: String?) = perform {
    val user = clerk.user ?: throw CoreException("no_user", "User not authenticated")
    user.update(
      UpdateUserParams(
        firstName = firstName?.let { Field.Value(it) } ?: Field.Null,
        lastName = lastName?.let { Field.Value(it) } ?: Field.Null,
        username = username?.let { Field.Value(it) } ?: Field.Null,
      )
    )
  }

  private fun perform(errorPrefix: String = "", operation: suspend () -> Unit) {
    _state.value = State.Loading
    viewModelScope.launch {
      runUiOperation { operation() }
        .onSuccess { _state.value = State.Success }
        .onFailure { _state.value = State.Error(errorPrefix + it.displayMessage) }
    }
  }

  fun reset() {
    _state.value = State.Idle
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data object Success : State

    data class Error(val message: String) : State
  }
}
