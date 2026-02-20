package com.clerk.ui.auth

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.clerk.api.Clerk
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.session.requiresForcedMfa
import com.clerk.api.ui.ClerkTheme
import com.clerk.telemetry.TelemetryEvents
import com.clerk.telemetry.telemetryPayload
import com.clerk.ui.core.composition.AuthStateProvider
import com.clerk.ui.core.composition.LocalAuthState
import com.clerk.ui.core.composition.LocalTelemetryCollector
import com.clerk.ui.sessiontask.mfa.SessionTaskMfaView
import com.clerk.ui.signin.SignInFactorOneView
import com.clerk.ui.signin.SignInFactorTwoView
import com.clerk.ui.signin.alternativemethods.SignInFactorAlternativeMethodsView
import com.clerk.ui.signin.clienttrust.SignInClientTrustView
import com.clerk.ui.signin.help.SignInGetHelpView
import com.clerk.ui.signin.password.forgot.SignInFactorOneForgotPasswordView
import com.clerk.ui.signin.password.reset.SignInSetNewPasswordView
import com.clerk.ui.signup.code.SignUpCodeField
import com.clerk.ui.signup.code.SignUpCodeView
import com.clerk.ui.signup.collectfield.CollectField
import com.clerk.ui.signup.collectfield.SignUpCollectFieldView
import com.clerk.ui.signup.completeprofile.SignUpCompleteProfileView
import com.clerk.ui.theme.ClerkThemeOverrideProvider
import kotlinx.serialization.Serializable

@Composable
fun AuthView(
  modifier: Modifier = Modifier,
  clerkTheme: ClerkTheme? = null,
  onAuthComplete: () -> Unit = {},
) {
  ClerkThemeOverrideProvider(clerkTheme) {
    val fullScreenModifier = Modifier.fillMaxSize().then(modifier)
    val backStack = rememberNavBackStack(AuthDestination.AuthStart)
    AuthStateProvider(backStack = backStack) {
      ObserveForcedMfaRouting(backStack = backStack)
      TrackScreenLoaded(LocalAuthState.current.mode.name)
      AuthNavDisplay(
        modifier = fullScreenModifier,
        backStack = backStack,
        onAuthComplete = onAuthComplete,
      )
    }
  }
}

@Composable
private fun ObserveForcedMfaRouting(backStack: NavBackStack<NavKey>) {
  val session = Clerk.sessionFlow.collectAsStateWithLifecycle().value
  LaunchedEffect(session?.requiresForcedMfa, backStack.lastOrNull()) {
    val top = backStack.lastOrNull()
    if (shouldRouteToSessionTaskMfa(session?.requiresForcedMfa == true, top)) {
      backStack.add(AuthDestination.SessionTaskMfa)
    }
  }
}

@Composable
private fun AuthNavDisplay(
  modifier: Modifier = Modifier,
  backStack: NavBackStack<NavKey>,
  onAuthComplete: () -> Unit,
) {
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
    entryProvider = authEntryProvider(backStack = backStack, onAuthComplete = onAuthComplete),
  )
}

private fun authEntryProvider(backStack: NavBackStack<NavKey>, onAuthComplete: () -> Unit) =
  entryProvider {
    entry<AuthDestination.AuthStart> { AuthStartView(onAuthComplete = onAuthComplete) }
    entry<AuthDestination.SignInFactorOne> {
      SignInFactorOneView(factor = it.factor, onAuthComplete = onAuthComplete)
    }
    entry<AuthDestination.SignInFactorOneUseAnotherMethod> {
      SignInFactorAlternativeMethodsView(
        currentFactor = it.currentFactor,
        onAuthComplete = onAuthComplete,
      )
    }
    entry<AuthDestination.SignInFactorTwo> {
      SignInFactorTwoView(factor = it.factor, onAuthComplete = onAuthComplete)
    }
    entry<AuthDestination.SessionTaskMfa> { SessionTaskMfaView(onAuthComplete = onAuthComplete) }
    entry<AuthDestination.SignInFactorTwoUseAnotherMethod> {
      SignInFactorAlternativeMethodsView(
        currentFactor = it.currentFactor,
        isSecondFactor = true,
        onAuthComplete = onAuthComplete,
      )
    }
    entry<AuthDestination.SignInForgotPassword> {
      SignInFactorOneForgotPasswordView(
        onClickFactor = { backStack.removeLastOrNull() },
        onAuthComplete = onAuthComplete,
      )
    }
    entry<AuthDestination.SignInSetNewPassword> {
      SignInSetNewPasswordView(onAuthComplete = onAuthComplete)
    }
    entry<AuthDestination.SignInGetHelp> { SignInGetHelpView() }
    entry<AuthDestination.SignInClientTrust> {
      SignInClientTrustView(factor = it.factor, onAuthComplete = onAuthComplete)
    }
    entry<AuthDestination.SignUpCollectField> {
      SignUpCollectFieldView(field = it.field, onAuthComplete = onAuthComplete)
    }
    entry<AuthDestination.SignUpCode> {
      SignUpCodeView(field = it.field, onAuthComplete = onAuthComplete)
    }
    entry<AuthDestination.SignUpCompleteProfile> {
      SignUpCompleteProfileView(onAuthComplete = onAuthComplete)
    }
  }

internal fun shouldRouteToSessionTaskMfa(requiresForcedMfa: Boolean, top: NavKey?): Boolean {
  return requiresForcedMfa && top != AuthDestination.SessionTaskMfa
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
