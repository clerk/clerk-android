package com.clerk.ui.auth

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.ui.ClerkTheme
import com.clerk.telemetry.TelemetryEvents
import com.clerk.telemetry.telemetryPayload
import com.clerk.ui.core.composition.AuthStateProvider
import com.clerk.ui.core.composition.LocalAuthState
import com.clerk.ui.core.composition.LocalTelemetryCollector
import com.clerk.ui.signin.SignInFactorOneView
import com.clerk.ui.signin.SignInFactorTwoView
import com.clerk.ui.signin.alternativemethods.SignInFactorAlternativeMethodsView
import com.clerk.ui.signin.clienttrust.SignInClientTrustView
import com.clerk.ui.signin.help.SignInGetHelpView
import com.clerk.ui.signin.password.forgot.SignInFactorOneForgotPasswordView
import com.clerk.ui.signin.password.reset.SignInSetNewPasswordView
import com.clerk.ui.sessiontask.mfa.SessionTaskMfaView
import com.clerk.ui.signup.code.SignUpCodeField
import com.clerk.ui.signup.code.SignUpCodeView
import com.clerk.ui.signup.collectfield.CollectField
import com.clerk.ui.signup.collectfield.SignUpCollectFieldView
import com.clerk.ui.signup.completeprofile.SignUpCompleteProfileView
import com.clerk.ui.theme.ClerkThemeOverrideProvider
import kotlinx.serialization.Serializable

@Composable
fun AuthView(modifier: Modifier = Modifier, clerkTheme: ClerkTheme? = null) {
  ClerkThemeOverrideProvider(clerkTheme) {
    val backStack = rememberNavBackStack(AuthDestination.AuthStart)
    AuthStateProvider(backStack = backStack) {
      val authState = LocalAuthState.current
      TrackScreenLoaded(authState.mode.name)
      NavDisplay(
        modifier = modifier,
        backStack = backStack,
        transitionSpec = {
          val spec = tween<IntOffset>(durationMillis = 300)
          slideInHorizontally(animationSpec = spec, initialOffsetX = { it }) togetherWith
            slideOutHorizontally(animationSpec = spec, targetOffsetX = { -it })
        },
        popTransitionSpec = {
          val spec = tween<IntOffset>(durationMillis = 300)
          slideInHorizontally(animationSpec = spec, initialOffsetX = { -it }) togetherWith
            slideOutHorizontally(animationSpec = spec, targetOffsetX = { it })
        },
        predictivePopTransitionSpec = { distance ->
          slideInHorizontally(initialOffsetX = { -distance }) togetherWith
            slideOutHorizontally(targetOffsetX = { distance })
        },
        onBack = {
          if (backStack.size > 1) {
            backStack.removeLastOrNull()
          }
        },
        entryProvider =
          entryProvider {
            entry<AuthDestination.AuthStart> {
              AuthStartView(onAuthComplete = { /* AuthView will unmount naturally */ })
            }
            entry<AuthDestination.SignInFactorOne> { key ->
              SignInFactorOneView(factor = key.factor, onAuthComplete = { /* AuthView will unmount naturally */ })
            }
            entry<AuthDestination.SignInFactorOneUseAnotherMethod> { key ->
              SignInFactorAlternativeMethodsView(
                currentFactor = key.currentFactor,
                onAuthComplete = { /* AuthView will unmount naturally */ },
              )
            }
            entry<AuthDestination.SignInFactorTwo> { key ->
              SignInFactorTwoView(factor = key.factor, onAuthComplete = { /* AuthView will unmount naturally */ })
            }
            entry<AuthDestination.SessionTaskMfa> {
              SessionTaskMfaView(onAuthComplete = { /* AuthView will unmount naturally */ })
            }
            entry<AuthDestination.SignInFactorTwoUseAnotherMethod> { key ->
              SignInFactorAlternativeMethodsView(
                currentFactor = key.currentFactor,
                isSecondFactor = true,
                onAuthComplete = { /* AuthView will unmount naturally */ },
              )
            }
            entry<AuthDestination.SignInForgotPassword> {
              SignInFactorOneForgotPasswordView(
                onClickFactor = { backStack.removeLastOrNull() },
                onAuthComplete = { /* AuthView will unmount naturally */ },
              )
            }
            entry<AuthDestination.SignInSetNewPassword> {
              SignInSetNewPasswordView(onAuthComplete = { /* AuthView will unmount naturally */ })
            }
            entry<AuthDestination.SignInGetHelp> { SignInGetHelpView() }
            entry<AuthDestination.SignInClientTrust> { key ->
              SignInClientTrustView(factor = key.factor, onAuthComplete = { /* AuthView will unmount naturally */ })
            }
            entry<AuthDestination.SignUpCollectField> { key ->
              SignUpCollectFieldView(field = key.field, onAuthComplete = { /* AuthView will unmount naturally */ })
            }
            entry<AuthDestination.SignUpCode> { key ->
              SignUpCodeView(field = key.field, onAuthComplete = { /* AuthView will unmount naturally */ })
            }
            entry<AuthDestination.SignUpCompleteProfile> {
              SignUpCompleteProfileView(onAuthComplete = { /* AuthView will unmount naturally */ })
            }
          },
      )
    }
  }
}

@Composable
private fun TrackScreenLoaded(mode: String) {
  val telemetryCollector = LocalTelemetryCollector.current
  LaunchedEffect(Unit) {
    telemetryCollector.record(
      TelemetryEvents.viewDidAppear(
        viewName = "AuthView",
        payload = telemetryPayload("mode" to mode),
      )
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

  @Serializable data object SessionTaskMfa : NavKey

  @Serializable data class SignInFactorTwoUseAnotherMethod(val currentFactor: Factor) : NavKey

  @Serializable data object SignInForgotPassword : NavKey

  @Serializable data object SignInSetNewPassword : NavKey

  @Serializable data object SignInGetHelp : NavKey

  @Serializable data class SignInClientTrust(val factor: Factor) : NavKey

  @Serializable data class SignUpCollectField(val field: CollectField) : NavKey

  @Serializable data class SignUpCode(val field: SignUpCodeField) : NavKey

  @Serializable data class SignUpCompleteProfile(val progress: Int) : NavKey
}
