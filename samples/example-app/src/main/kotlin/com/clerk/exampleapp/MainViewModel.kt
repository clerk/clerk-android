package com.clerk.exampleapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.Clerk
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * MainViewModel manages the global authentication state for the app.
 *
 * This ViewModel observes Clerk's initialization and user state to determine whether the user
 * should see the loading screen, sign-in screen, or home screen.
 *
 * The authentication flow:
 * 1. Loading: Clerk is still initializing
 * 2. SignedOut: No active session, user needs to sign in
 * 3. SignedIn: User has an active session, can access protected content
 */
@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

  private val _authenticationState =
    MutableStateFlow<AuthenticationState>(AuthenticationState.Loading)

  /**
   * Exposes the current authentication state to UI components. UI can observe this to show
   * appropriate screens.
   */
  val uiState = _authenticationState.asStateFlow()

  init {
    /**
     * Combine initialization state with user state to determine UI state. This ensures we show
     * loading until Clerk is initialized and then react to user authentication changes.
     */
    combine(Clerk.isInitialized, Clerk.user) { isInitialized, user ->
        when {
          !isInitialized -> AuthenticationState.Loading
          user != null -> AuthenticationState.SignedIn
          else -> AuthenticationState.SignedOut
        }
      }
      .onEach { state -> _authenticationState.value = state }
      .launchIn(viewModelScope)
  }
}

/**
 * Represents the different authentication states in the app.
 * - Loading: Clerk is still initializing
 * - SignedOut: No active session, user needs to sign in
 * - SignedIn: User has an active session, can access protected content
 */
sealed interface AuthenticationState {
  object Loading : AuthenticationState

  object SignedIn : AuthenticationState

  object SignedOut : AuthenticationState
}
