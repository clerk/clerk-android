package com.clerk.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.clerk.api.network.model.factor.Factor
import com.clerk.ui.signin.SignInFactorOneView
import com.clerk.ui.signin.SignInFactorTwoView
import com.clerk.ui.signin.alternativemethods.SignInFactorAlternativeMethodsView
import com.clerk.ui.signin.help.SignInGetHelpView
import com.clerk.ui.signin.password.forgot.SignInFactorOneForgotPasswordView
import com.clerk.ui.signin.password.reset.SignInSetNewPasswordView
import kotlinx.serialization.Serializable

@Composable
fun AuthView(modifier: Modifier = Modifier, authMode: AuthMode = AuthMode.SignInOrUp) {
  val backStack = rememberNavBackStack(Screens.AuthStart(authMode))

  NavDisplay(
    modifier = modifier,
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Screens.AuthStart> { key -> AuthStartView(authMode = key.mode) }
        entry<Screens.SignInFactorOne> { key ->
          SignInFactorOneView(factor = key.factor, onBackPressed = { backStack.removeLastOrNull() })
        }
        entry<Screens.SignInFactorOneUseAnotherMethod> { key ->
          SignInFactorAlternativeMethodsView(
            currentFactor = key.currentFactor,
            onBackPressed = { backStack.removeLastOrNull() },
            onClickFactor = {},
          )
        }
        entry<Screens.SignInFactorTwo> { key ->
          SignInFactorTwoView(factor = key.factor, onBackPressed = { backStack.removeLastOrNull() })
        }
        entry<Screens.SignInFactorTwoUseAnotherMethod> { key ->
          SignInFactorAlternativeMethodsView(
            currentFactor = key.currentFactor,
            isSecondFactor = true,
            onBackPressed = { backStack.removeLastOrNull() },
            onClickFactor = {},
          )
        }
        entry<Screens.SignInForgotPassword> { key ->
          SignInFactorOneForgotPasswordView(
            onBackPressed = { backStack.removeLastOrNull() },
            onClickFactor = {},
          )
        }
        entry<Screens.SignInSetNewPassword> {
          SignInSetNewPasswordView(onBackPressed = { backStack.removeLastOrNull() })
        }
        entry<Screens.SignInGetHelp> {
          SignInGetHelpView(onBackPressed = { backStack.removeLastOrNull() })
        }
      },
  )
}

@PreviewLightDark
@Composable
private fun Preview() {
  AuthView()
}

internal object Screens {

  @Serializable data class AuthStart(val mode: AuthMode) : NavKey

  @Serializable data class SignInFactorOne(val factor: Factor) : NavKey

  @Serializable data class SignInFactorOneUseAnotherMethod(val currentFactor: Factor) : NavKey

  @Serializable data class SignInFactorTwo(val factor: Factor) : NavKey

  @Serializable data class SignInFactorTwoUseAnotherMethod(val currentFactor: Factor) : NavKey

  @Serializable data object SignInForgotPassword : NavKey

  @Serializable data object SignInSetNewPassword : NavKey

  @Serializable data object SignInGetHelp : NavKey
}
