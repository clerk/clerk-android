package com.clerk.ui.auth

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
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
import com.clerk.ui.signup.code.SignUpCodeField
import com.clerk.ui.signup.code.SignUpCodeView
import com.clerk.ui.signup.collectfield.CollectField
import com.clerk.ui.signup.collectfield.SignUpCollectFieldView
import com.clerk.ui.signup.completeprofile.SignUpCompleteProfileView
import kotlinx.serialization.Serializable

@SuppressLint("ComposeCompositionLocalUsage")
internal val LocalAuthState = staticCompositionLocalOf<AuthState> { error("No AuthState provided") }

@Composable
internal fun AuthStateProvider(backStack: NavBackStack<NavKey>, content: @Composable () -> Unit) {
  val authState = remember { AuthState(backStack = backStack) }
  CompositionLocalProvider(LocalAuthState provides authState) { content() }
}

@Composable
fun AuthView(modifier: Modifier = Modifier, onAuthComplete: () -> Unit = {}) {
  val backStack = rememberNavBackStack(AuthDestination.AuthStart)
  AuthStateProvider(backStack) {
    NavDisplay(
      modifier = modifier,
      backStack = backStack,
      onBack = { backStack.removeLastOrNull() },
      entryProvider =
        entryProvider {
          entry<AuthDestination.AuthStart> { key -> AuthStartView(onAuthComplete = onAuthComplete) }
          entry<AuthDestination.SignInFactorOne> { key ->
            SignInFactorOneView(factor = key.factor, onAuthComplete = onAuthComplete)
          }
          entry<AuthDestination.SignInFactorOneUseAnotherMethod> { key ->
            SignInFactorAlternativeMethodsView(
              currentFactor = key.currentFactor,
              onAuthComplete = onAuthComplete,
            )
          }
          entry<AuthDestination.SignInFactorTwo> { key ->
            SignInFactorTwoView(factor = key.factor, onAuthComplete = onAuthComplete)
          }
          entry<AuthDestination.SignInFactorTwoUseAnotherMethod> { key ->
            SignInFactorAlternativeMethodsView(
              currentFactor = key.currentFactor,
              isSecondFactor = true,
              onAuthComplete = onAuthComplete,
            )
          }
          entry<AuthDestination.SignInForgotPassword> { key ->
            SignInFactorOneForgotPasswordView(
              onClickFactor = { backStack.removeLastOrNull() },
              onAuthComplete = onAuthComplete,
            )
          }
          entry<AuthDestination.SignInSetNewPassword> {
            SignInSetNewPasswordView(onAuthComplete = onAuthComplete)
          }
          entry<AuthDestination.SignInGetHelp> { SignInGetHelpView() }
          entry<AuthDestination.SignUpCollectField> { key ->
            SignUpCollectFieldView(field = key.field, onAuthComplete = onAuthComplete)
          }
          entry<AuthDestination.SignUpCode> { key ->
            SignUpCodeView(field = key.field, onAuthComplete = onAuthComplete)
          }
          entry<AuthDestination.SignUpCompleteProfile> {
            SignUpCompleteProfileView(progress = it.progress, onAuthComplete = onAuthComplete)
          }
        },
    )
  }
}

@PreviewLightDark
@Composable
private fun Preview() {
  AuthView()
}

internal object AuthDestination {

  @Serializable data object AuthStart : NavKey

  @Serializable data class SignInFactorOne(val factor: Factor) : NavKey

  @Serializable data class SignInFactorOneUseAnotherMethod(val currentFactor: Factor) : NavKey

  @Serializable data class SignInFactorTwo(val factor: Factor) : NavKey

  @Serializable data class SignInFactorTwoUseAnotherMethod(val currentFactor: Factor) : NavKey

  @Serializable data object SignInForgotPassword : NavKey

  @Serializable data object SignInSetNewPassword : NavKey

  @Serializable data object SignInGetHelp : NavKey

  @Serializable data class SignUpCollectField(val field: CollectField) : NavKey

  @Serializable data class SignUpCode(val field: SignUpCodeField) : NavKey

  @Serializable data class SignUpCompleteProfile(val progress: Int) : NavKey
}
