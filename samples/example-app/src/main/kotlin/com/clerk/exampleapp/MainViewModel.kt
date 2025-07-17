package com.clerk.exampleapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.Clerk
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * MainViewModel manages the global authentication state for the app.
 * 
 * This ViewModel observes Clerk's initialization and session states to determine
 * whether the user should see the loading screen, sign-in screen, or home screen.
 * 
 * The authentication flow:
 * 1. Loading: Clerk is initializing
 * 2. SignedOut: No active session, show sign-in
 * 3. SignedIn: Active session exists, show home screen
 */
@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

  private val _authenticationState =
    MutableStateFlow<AuthenticationState>(AuthenticationState.Loading)
  
  /**
   * Exposes the current authentication state to UI components.
   * UI can observe this to show appropriate screens.
   */
  val uiState = _authenticationState.asStateFlow()

  init {
    /**
     * Listen to Clerk's initialization state.
     * Once Clerk is initialized, check if there's an active session.
     * 
     * Clerk.isInitialized is a Flow that emits true when Clerk is ready.
     * Clerk.session is null when no user is signed in.
     */
    Clerk.isInitialized
      .onEach { isInitialized ->
        if (isInitialized) {
          if (Clerk.session != null) {
            _authenticationState.value = AuthenticationState.SignedIn
          } else {
            _authenticationState.value = AuthenticationState.SignedOut
          }
        }
      }
      .launchIn(viewModelScope)
  }
}

/**
 * Represents the different authentication states in the app.
 * 
 * - Loading: Clerk is still initializing, show loading indicator
 * - SignedOut: No active session, user needs to sign in
 * - SignedIn: User has an active session, can access protected content
 */
sealed interface AuthenticationState {
  object Loading : AuthenticationState

  object SignedIn : AuthenticationState

  object SignedOut : AuthenticationState
}
