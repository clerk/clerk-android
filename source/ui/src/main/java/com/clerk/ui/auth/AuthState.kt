import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.startingFirstFactor
import com.clerk.api.signin.startingSecondFactor
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.firstFieldToCollect
import com.clerk.api.signup.firstFieldToVerify
import com.clerk.ui.auth.AuthMode
import com.clerk.ui.auth.Destinations
import com.clerk.ui.signup.code.SignUpCodeField
import com.clerk.ui.signup.collectfield.CollectField

private const val EMAIL_ADDRESS = "email_address"

private const val PHONE_NUMBER = "phone_number"

private const val PASSWORD = "password"

private const val USERNAME = "username"

@Stable
internal class AuthState(
  val mode: AuthMode = AuthMode.SignInOrUp,
  val backStack: NavBackStack<NavKey>,
) {

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

  internal fun setToStepForStatus(signIn: SignIn) {
    when (signIn.status) {
      SignIn.Status.COMPLETE -> return
      SignIn.Status.NEEDS_IDENTIFIER -> backStack.clear()
      SignIn.Status.NEEDS_FIRST_FACTOR -> {
        signIn.startingFirstFactor?.let { backStack.add(Destinations.SignInFactorOne(factor = it)) }
          ?: backStack.add(Destinations.SignInGetHelp)
      }
      SignIn.Status.NEEDS_SECOND_FACTOR -> {
        signIn.startingSecondFactor?.let {
          backStack.add(Destinations.SignInFactorTwo(factor = it))
        } ?: backStack.add(Destinations.SignInGetHelp)
      }
      SignIn.Status.NEEDS_NEW_PASSWORD -> backStack.add(Destinations.SignInSetNewPassword)
      SignIn.Status.UNKNOWN -> return
    }
  }

  internal fun setToStepForStatus(signUp: SignUp) {
    when (signUp.status) {
      SignUp.Status.ABANDONED -> backStack.clear()
      SignUp.Status.MISSING_REQUIREMENTS -> handleMissingRequirements(signUp)
      SignUp.Status.COMPLETE -> return
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
          backStack.add(Destinations.SignUpCode(field = SignUpCodeField.Email(emailAddress)))
        } else {
          backStack.clear()
        }
      }
      PHONE_NUMBER -> {
        val phoneNumber = signUp.phoneNumber
        if (phoneNumber != null) {
          backStack.add(Destinations.SignUpCode(SignUpCodeField.Phone(phoneNumber)))
        } else {
          backStack.clear()
        }
      }
      else -> backStack.clear()
    }
  }

  private fun handleFieldCollection(signUp: SignUp) {
    val nextFieldToCollect = signUp.firstFieldToCollect
    if (nextFieldToCollect != null) {
      when (nextFieldToCollect) {
        PASSWORD -> backStack.add(Destinations.SignUpCollectField(CollectField.Password))
        EMAIL_ADDRESS -> backStack.add(Destinations.SignUpCollectField(CollectField.Email))
        PHONE_NUMBER -> backStack.add(Destinations.SignUpCollectField(CollectField.Phone))
        USERNAME -> backStack.add(Destinations.SignUpCollectField(CollectField.Username))
        else -> backStack.add(Destinations.SignUpCompleteProfile(signUp.missingFields.count()))
      }
    }
  }
}
