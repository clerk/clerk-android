package com.clerk.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.clerk.api.Clerk
import com.clerk.api.session.Session
import com.clerk.api.session.requiresForcedMfa
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.startingFirstFactor
import com.clerk.api.signin.startingSecondFactor
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.firstFieldToCollect
import com.clerk.api.signup.firstFieldToVerify
import com.clerk.ui.core.common.NavigableState
import com.clerk.ui.core.composition.AuthStateProvider
import com.clerk.ui.core.navigation.pop
import com.clerk.ui.signup.code.SignUpCodeField
import com.clerk.ui.signup.collectfield.CollectField

private const val EMAIL_ADDRESS = "email_address"

private const val PHONE_NUMBER = "phone_number"

private const val PASSWORD = "password"

private const val USERNAME = "username"

@Stable
@Suppress("TooManyFunctions")
internal class AuthState(
  val mode: AuthMode = AuthMode.SignInOrUp,
  val backStack: NavBackStack<NavKey>,
) : NavigableState<AuthDestination> {

  // Auth start fields
  var authStartIdentifier by mutableStateOf("")
  var authStartPhoneNumber by mutableStateOf("")

  // Sign In
  var signInPassword by mutableStateOf("")
  var signInNewPassword by mutableStateOf("")
  var signInConfirmNewPassword by mutableStateOf("")
  var signInBackupCode by mutableStateOf("")

  // Sign Up
  var signUpFirstName by mutableStateOf("")
  var signUpLastName by mutableStateOf("")
  var signUpPassword by mutableStateOf("")
  var signUpUsername by mutableStateOf("")
  var signUpEmail by mutableStateOf("")
  var signUpPhoneNumber by mutableStateOf("")
  var signUpLegalAccepted by mutableStateOf(false)

  override fun navigateTo(destination: NavKey) {
    backStack.add(destination)
  }

  override fun navigateBack() {
    backStack.removeLastOrNull()
  }

  override fun clearBackStack() {
    resetToRoot()
  }

  override fun pop(numberOfScreens: Int) {
    backStack.pop(numberOfScreens)
  }

  /**
   * Safely resets the back stack to the root entry (AuthStart). This prevents NavDisplay crashes
   * during rapid auth state transitions.
   */
  private fun resetToRoot() {
    if (backStack.size > 1) {
      backStack.pop(backStack.size - 1)
    }
  }

  override fun popTo(destination: AuthDestination) {
    val targetIndex = backStack.indexOfLast { it == destination }
    if (targetIndex == -1) return // Not found → no-op

    val toPop = (backStack.size - 1) - targetIndex
    if (toPop > 0) {
      backStack.pop(toPop) // non-inclusive: leaves `destination` on top
    }
  }

  internal fun setToStepForStatus(signIn: SignIn, onAuthComplete: () -> Unit) {
    if (routeForcedMfaIfRequired(signIn)) {
      return
    }

    when (signIn.status) {
      SignIn.Status.COMPLETE -> onAuthComplete()
      SignIn.Status.NEEDS_IDENTIFIER -> resetToRoot()
      SignIn.Status.NEEDS_FIRST_FACTOR -> routeToFirstFactorOrHelp(signIn)
      SignIn.Status.NEEDS_SECOND_FACTOR -> routeToSecondFactorOrHelp(signIn)
      SignIn.Status.NEEDS_NEW_PASSWORD -> backStack.add(AuthDestination.SignInSetNewPassword)
      SignIn.Status.NEEDS_CLIENT_TRUST -> routeToClientTrustOrHelp(signIn)
      SignIn.Status.UNKNOWN -> Unit
    }
  }

  private fun routeForcedMfaIfRequired(signIn: SignIn): Boolean {
    if (!signIn.requiresForcedMfaStep()) {
      return false
    }
    routeToSecondFactorOrHelp(signIn)
    return true
  }

  private fun routeToFirstFactorOrHelp(signIn: SignIn) {
    signIn.startingFirstFactor?.let { backStack.add(AuthDestination.SignInFactorOne(factor = it)) }
      ?: backStack.add(AuthDestination.SignInGetHelp)
  }

  private fun routeToSecondFactorOrHelp(signIn: SignIn) {
    signIn.startingSecondFactor?.let { backStack.add(AuthDestination.SignInFactorTwo(factor = it)) }
      ?: backStack.add(AuthDestination.SignInGetHelp)
  }

  private fun routeToClientTrustOrHelp(signIn: SignIn) {
    signIn.startingSecondFactor?.let {
      backStack.add(AuthDestination.SignInClientTrust(factor = it))
    } ?: backStack.add(AuthDestination.SignInGetHelp)
  }

  internal fun setToStepForStatus(signUp: SignUp, onAuthComplete: () -> Unit) {
    when (signUp.status) {
      SignUp.Status.ABANDONED -> resetToRoot()
      SignUp.Status.MISSING_REQUIREMENTS -> handleMissingRequirements(signUp)
      SignUp.Status.COMPLETE -> {
        onAuthComplete()
        return
      }
      SignUp.Status.UNKNOWN -> return
    }
  }

  private fun handleMissingRequirements(signUp: SignUp) {
    val firstFieldToVerify = signUp.firstFieldToVerify
    if (firstFieldToVerify != null) {
      handleFieldVerification(signUp, firstFieldToVerify)
    } else {
      handleFieldCollection(signUp)
    }
  }

  private fun handleFieldVerification(signUp: SignUp, fieldToVerify: String) {
    when (fieldToVerify) {
      EMAIL_ADDRESS -> {
        val emailAddress = signUp.emailAddress
        if (emailAddress != null) {
          backStack.add(AuthDestination.SignUpCode(field = SignUpCodeField.Email(emailAddress)))
        } else {
          resetToRoot()
        }
      }
      PHONE_NUMBER -> {
        val phoneNumber = signUp.phoneNumber
        if (phoneNumber != null) {
          backStack.add(AuthDestination.SignUpCode(SignUpCodeField.Phone(phoneNumber)))
        } else {
          resetToRoot()
        }
      }
      else -> resetToRoot()
    }
  }

  private fun handleFieldCollection(signUp: SignUp) {
    val nextFieldToCollect = signUp.firstFieldToCollect
    if (nextFieldToCollect != null) {
      when (nextFieldToCollect) {
        PASSWORD -> backStack.add(AuthDestination.SignUpCollectField(CollectField.Password))
        EMAIL_ADDRESS -> backStack.add(AuthDestination.SignUpCollectField(CollectField.Email))
        PHONE_NUMBER -> backStack.add(AuthDestination.SignUpCollectField(CollectField.Phone))
        USERNAME -> backStack.add(AuthDestination.SignUpCollectField(CollectField.Username))
        else -> backStack.add(AuthDestination.SignUpCompleteProfile(signUp.missingFields.count()))
      }
    }
  }
}

internal fun SignIn.requiresForcedMfaStep(
  session: Session? = this.correspondingSession()
): Boolean {
  return status == SignIn.Status.COMPLETE && session?.requiresForcedMfa == true
}

internal fun SignIn.correspondingSession(): Session? {
  val sessions = runCatching { Clerk.client.sessions }.getOrDefault(emptyList())
  val sessionFromId =
    createdSessionId?.let { sessionId -> sessions.firstOrNull { it.id == sessionId } }
  return sessionFromId ?: Clerk.session
}

@Composable
internal fun PreviewAuthStateProvider(content: @Composable () -> Unit) {
  val backStack = rememberNavBackStack()
  AuthStateProvider(backStack) { content() }
}
