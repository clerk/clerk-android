package com.clerk.ui.protect

import android.content.Context
import android.content.MutableContextWrapper
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.protect.ClerkProtect
import com.clerk.api.protect.ProtectCheckResource
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Protect's runtime creates its WebView from the container's context, so the retained host uses a
// MutableContextWrapper that is rebound to each new Activity instead of keeping the destroyed one.
internal class ProtectCheckViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state: StateFlow<State> = _state.asStateFlow()

  private var host: ProtectChallengeHost? = null
  private var job: Job? = null

  fun attachHost(context: Context): ProtectChallengeHost {
    val existing = host
    if (existing == null) {
      return ProtectChallengeHost(MutableContextWrapper(context)).also { host = it }
    }
    (existing.parent as? ViewGroup)?.removeView(existing)
    existing.rebind(context)
    return existing
  }

  fun releaseHost(view: ProtectChallengeHost) {
    view.rebind(view.context.applicationContext)
  }

  fun start(flow: ProtectCheckFlow) {
    if (_state.value != State.Idle || job?.isActive == true) return
    val host = host ?: return
    _state.value = State.Running
    job = viewModelScope.launch { _state.value = resolve(flow, host) }
  }

  fun retry(flow: ProtectCheckFlow) {
    if (_state.value !is State.Failed) return
    _state.value = State.Idle
    start(flow)
  }

  fun completionHandled() {
    _state.value = State.Done
  }

  private suspend fun resolve(flow: ProtectCheckFlow, host: ProtectChallengeHost): State {
    val client = Clerk.clientFlow.value
    val runChallenge: suspend (ProtectCheckResource) -> String = { check ->
      host.prepare()
      ClerkProtect.executeProtectCheck(check, host.challengeContainer)
    }
    return when (flow) {
      ProtectCheckFlow.SignIn ->
        client?.signIn?.let { signIn ->
          ProtectCheckResolver(SignInProtectOperations, runChallenge)
            .resolve(signIn)
            .toState(State::SignInCompleted)
        }
      ProtectCheckFlow.SignUp ->
        client?.signUp?.let { signUp ->
          ProtectCheckResolver(SignUpProtectOperations, runChallenge)
            .resolve(signUp)
            .toState(State::SignUpCompleted)
        }
    } ?: State.Failed(message = null)
  }

  override fun onCleared() {
    host = null
  }

  sealed interface State {
    data object Idle : State

    data object Running : State

    data class SignInCompleted(val signIn: SignIn) : State

    data class SignUpCompleted(val signUp: SignUp) : State

    data class Failed(val message: String?) : State

    data object Done : State
  }
}

private fun <T : Any> ProtectResolution<T>.toState(
  completed: (T) -> ProtectCheckViewModel.State
): ProtectCheckViewModel.State =
  when (this) {
    is ProtectResolution.Success -> completed(resource)
    is ProtectResolution.Failure -> ProtectCheckViewModel.State.Failed(message)
  }
