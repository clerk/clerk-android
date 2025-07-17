package com.clerk.exampleapp.ui.screens.signin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.Clerk
import com.clerk.network.model.environment.UserSettings
import com.clerk.network.serialization.flatMap
import com.clerk.network.serialization.longErrorMessageOrNull
import com.clerk.network.serialization.onFailure
import com.clerk.network.serialization.onSuccess
import com.clerk.signin.SignIn
import com.clerk.signin.attemptFirstFactor
import com.clerk.signin.prepareFirstFactor
import com.clerk.signup.SignUp
import com.clerk.signup.attemptVerification
import com.clerk.signup.prepareVerification
import com.clerk.sso.OAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for handling sign-in and sign-up flows with phone authentication.
 * 
 * This ViewModel manages:
 * - Phone number authentication (both sign-in and sign-up)
 * - SMS OTP verification
 * - Social authentication with OAuth providers
 * 
 * Phone Authentication Flow:
 * 1. User enters phone number
 * 2. handlePhoneNumber() creates sign-in or sign-up
 * 3. Clerk sends SMS OTP to the phone
 * 4. User enters OTP code
 * 5. verify() attempts verification
 * 6. On success, user is signed in
 */
@HiltViewModel
class SignInOrUpViewModel @Inject constructor() : ViewModel() {

  private val _state = MutableStateFlow<SignInOrUpState>(SignInOrUpState.SignedOut)
  
  /**
   * Current state of the sign-in/up process.
   * UI observes this to show appropriate screens and buttons.
   */
  val state = _state.asStateFlow()

  /**
   * Tracks whether the current flow is sign-up (true) or sign-in (false).
   * This determines which verification method to use.
   */
  var isSignUp: Boolean = false

  /**
   * Initiates phone authentication flow.
   * 
   * @param phoneNumber The user's phone number (should include country code)
   * @param isSignUp Whether this is a sign-up (true) or sign-in (false) flow
   * 
   * For sign-up: Creates a new user account and sends SMS verification
   * For sign-in: Attempts to sign in existing user and sends SMS verification
   */
  fun handlePhoneNumber(phoneNumber: String, isSignUp: Boolean) {
    Log.d("SignInOrUpViewModel", "handlePhoneNumber: $phoneNumber, isSignUp: $isSignUp")
    this.isSignUp = isSignUp
    if (isSignUp) {
      createSignUp(phoneNumber)
    } else {
      createSignIn(phoneNumber)
    }
  }

  /**
   * Creates a new sign-up with phone number verification.
   * 
   * Process:
   * 1. Create SignUp with phone number
   * 2. Prepare phone code verification (triggers SMS)
   * 3. Update UI state to show OTP input
   * 
   * @param phoneNumber User's phone number
   */
  private fun createSignUp(phoneNumber: String) {
    viewModelScope.launch(Dispatchers.IO) {
      SignUp.Companion.create(SignUp.CreateParams.Standard(phoneNumber = phoneNumber)).flatMap {
        it
          .prepareVerification(SignUp.PrepareVerificationParams.Strategy.PhoneCode())
          .onSuccess {
            withContext(Dispatchers.Main) { _state.value = SignInOrUpState.NeedsFirstFactor }
          }
          .onFailure { error ->
            withContext(Dispatchers.Main) {
              Log.e(
                "SignUpViewModel",
                "Failed to prepare verification: ${error.longErrorMessageOrNull}",
                error.throwable,
              )
              _state.value = SignInOrUpState.Error
            }
          }
      }
    }
  }

  /**
   * Creates a sign-in attempt with phone number verification.
   * 
   * Process:
   * 1. Create SignIn with phone number strategy
   * 2. Prepare first factor (phone code - triggers SMS)
   * 3. Update UI state to show OTP input
   * 
   * @param phoneNumber User's phone number
   */
  private fun createSignIn(phoneNumber: String) {
    viewModelScope.launch(Dispatchers.IO) {
      SignIn.create(SignIn.CreateParams.Strategy.PhoneCode(phoneNumber))
        .flatMap { it.prepareFirstFactor(SignIn.PrepareFirstFactorParams.PhoneCode()) }
        .onSuccess {
          withContext(Dispatchers.Main) { _state.value = SignInOrUpState.NeedsFirstFactor }
        }
        .onFailure {
          Log.e(
            "SignInViewModel",
            "Failed to create sign in: ${it.longErrorMessageOrNull}",
            it.throwable,
          )
          withContext(Dispatchers.Main) { _state.value = SignInOrUpState.Error }
        }
    }
  }

  /**
   * Verifies the SMS OTP code entered by the user.
   * 
   * @param code The OTP code from SMS (typically 6 digits)
   * 
   * Routes to either sign-up or sign-in verification based on current flow.
   */
  fun verify(code: String) {
    if (isSignUp) {
      verifySignUp(code)
    } else {
      verifySignIn(code)
    }
  }

  /**
   * Verifies the OTP code for sign-in flow.
   * 
   * Uses the in-progress SignIn instance from Clerk.signIn to attempt
   * first factor verification with the phone code.
   * 
   * @param code The SMS OTP code entered by the user
   */
  private fun verifySignIn(code: String) {
    val inProgressSignIn = Clerk.signIn ?: return
    viewModelScope.launch(Dispatchers.IO) {
      inProgressSignIn
        .attemptFirstFactor(SignIn.AttemptFirstFactorParams.PhoneCode(code))
        .onSuccess { withContext(Dispatchers.Main) { _state.value = SignInOrUpState.Success } }
        .onFailure {
          Log.e("SignInViewModel", "${it.longErrorMessageOrNull}", it.throwable)
          withContext(Dispatchers.Main) { _state.value = SignInOrUpState.Error }
        }
    }
  }

  /**
   * Verifies the OTP code for sign-up flow.
   * 
   * Uses the in-progress SignUp instance from Clerk.signUp to attempt
   * verification with the phone code. On success, checks if sign-up is complete.
   * 
   * @param code The SMS OTP code entered by the user
   */
  private fun verifySignUp(code: String) {
    // Grab in progress sign up
    val inProgressSignUp = Clerk.signUp ?: return
    viewModelScope.launch(Dispatchers.IO) {
      inProgressSignUp
        .attemptVerification(SignUp.AttemptVerificationParams.PhoneCode(code))
        .onSuccess {
          withContext(Dispatchers.Main) {
            if (it.status == SignUp.Status.COMPLETE) {
              _state.value = SignInOrUpState.Success
            }
          }
        }
        .onFailure {
          withContext(Dispatchers.Main) {
            Log.e("SignUpViewModel", "${it.longErrorMessageOrNull}", it.throwable)
            _state.value = SignInOrUpState.Error
          }
        }
    }
  }

  /**
   * Initiates OAuth authentication with social providers.
   * 
   * This opens the browser/WebView for OAuth flow with providers like:
   * - Google
   * - GitHub
   * - Apple
   * - Facebook
   * - And others configured in Clerk Dashboard
   * 
   * @param socialConfig Configuration for the social provider from Clerk
   */
  fun authenticateWithRedirect(socialConfig: UserSettings.SocialConfig) {
    viewModelScope.launch(Dispatchers.IO) {
      SignIn.authenticateWithRedirect(
          SignIn.AuthenticateWithRedirectParams.OAuth(
            OAuthProvider.fromStrategy(socialConfig.strategy)
          )
        )
        .onSuccess { withContext(Dispatchers.Main) { _state.value = SignInOrUpState.Success } }
        .onFailure {
          Log.e("SignInViewModel", "Failed to authenticate with redirect", it.throwable)
          withContext(Dispatchers.Main) { _state.value = SignInOrUpState.Error }
        }
    }
  }
}

/**
 * Represents the different states of the sign-in/up process.
 * 
 * UI components observe this state to show appropriate screens and inputs:
 * 
 * - SignedOut: Initial state, show phone number input
 * - NeedsFirstFactor: SMS sent, show OTP code input  
 * - Success: Authentication completed, navigate to home
 * - Error: Something went wrong, show error message
 */
sealed interface SignInOrUpState {
  /** User is signed out, needs to enter phone number */
  data object SignedOut : SignInOrUpState

  /** Authentication was successful, user can proceed */
  data object Success : SignInOrUpState

  /** SMS sent, user needs to enter verification code */
  data object NeedsFirstFactor : SignInOrUpState

  /** An error occurred during authentication */
  data object Error : SignInOrUpState
}
