package com.clerk.ui.protect

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.protect.ClerkProtect
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.get as reloadSignIn
import com.clerk.api.signin.submitProtectCheck
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.get as reloadSignUp
import com.clerk.api.signup.submitProtectCheck
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.composition.LocalAuthState
import com.clerk.ui.core.scaffold.ClerkThemedAuthScaffold
import kotlin.coroutines.cancellation.CancellationException

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun ProtectCheckView(
  flow: ProtectCheckFlow,
  onAuthComplete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val authState = LocalAuthState.current
  val client = Clerk.clientFlow.collectAsStateWithLifecycle().value
  val snackbarHostState = remember { SnackbarHostState() }
  var challengeHost by remember { mutableStateOf<ProtectChallengeHost?>(null) }
  var retryNonce by remember { mutableIntStateOf(0) }
  var isRunning by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val fallbackError = stringResource(R.string.something_went_wrong_please_try_again)

  LaunchedEffect(flow, challengeHost, retryNonce) {
    val host = challengeHost ?: return@LaunchedEffect
    val initialResource =
      when (flow) {
        ProtectCheckFlow.SignIn -> client?.signIn
        ProtectCheckFlow.SignUp -> client?.signUp
      }

    if (initialResource == null) {
      errorMessage = fallbackError
      host.finish()
      return@LaunchedEffect
    }

    isRunning = true
    errorMessage = null
    host.prepare()
    val result =
      when (initialResource) {
        is SignIn -> resolveSignInProtectChecks(initialResource, host)
        is SignUp -> resolveSignUpProtectChecks(initialResource, host)
        else -> error("Unsupported Protect flow resource")
      }
    isRunning = false

    when (result) {
      is ProtectResolution.Success<*> -> {
        when (val resource = result.resource) {
          is SignIn -> authState.setToStepForStatus(resource, onAuthComplete = onAuthComplete)
          is SignUp -> authState.setToStepForStatus(resource, onAuthComplete = onAuthComplete)
        }
      }
      is ProtectResolution.Failure -> {
        host.finish()
        errorMessage = result.message ?: fallbackError
        snackbarHostState.showSnackbar(errorMessage ?: fallbackError)
      }
    }
  }

  ClerkThemedAuthScaffold(
    modifier = modifier,
    snackbarHostState = snackbarHostState,
    title = stringResource(R.string.security),
    subtitle = stringResource(R.string.to_continue),
    hasBackButton = false,
  ) {
    AndroidView(
      modifier = Modifier.fillMaxWidth().height(if (errorMessage == null) 320.dp else 0.dp),
      factory = { context -> ProtectChallengeHost(context).also { challengeHost = it } },
      update = { host -> host.setRunning(isRunning) },
    )
    if (errorMessage != null) {
      ClerkButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.try_again),
        onClick = { retryNonce += 1 },
      )
    }
  }
}

@Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "ReturnCount")
private suspend fun resolveSignInProtectChecks(
  initial: SignIn,
  host: ProtectChallengeHost,
): ProtectResolution<SignIn> {
  var current = initial
  var reloads = 0
  while (current.requiresProtectCheck()) {
    val check = current.protectCheck ?: return ProtectResolution.Failure()
    host.prepare()
    val proof =
      try {
        ClerkProtect.executeProtectCheck(check, host.challengeContainer)
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (throwable: Throwable) {
        if (ClerkProtect.errorCode(throwable) == PROTECT_CHECK_EXPIRED && reloads < MAX_RELOADS) {
          reloads += 1
          when (val refreshed = current.reloadSignIn()) {
            is ClerkResult.Success -> {
              current = refreshed.value
              continue
            }
            is ClerkResult.Failure -> return refreshed.toProtectFailure()
          }
        }
        return ProtectResolution.Failure()
      }

    when (val submitted = current.submitProtectCheck(proof)) {
      is ClerkResult.Success -> current = submitted.value
      is ClerkResult.Failure -> {
        if (submitted.isAlreadyResolved() && reloads < MAX_RELOADS) {
          reloads += 1
          when (val refreshed = current.reloadSignIn()) {
            is ClerkResult.Success -> current = refreshed.value
            is ClerkResult.Failure -> return refreshed.toProtectFailure()
          }
        } else {
          return submitted.toProtectFailure()
        }
      }
    }
  }
  return ProtectResolution.Success(current)
}

@Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "ReturnCount")
private suspend fun resolveSignUpProtectChecks(
  initial: SignUp,
  host: ProtectChallengeHost,
): ProtectResolution<SignUp> {
  var current = initial
  var reloads = 0
  while (current.requiresProtectCheck()) {
    val check = current.protectCheck ?: return ProtectResolution.Failure()
    host.prepare()
    val proof =
      try {
        ClerkProtect.executeProtectCheck(check, host.challengeContainer)
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (throwable: Throwable) {
        if (ClerkProtect.errorCode(throwable) == PROTECT_CHECK_EXPIRED && reloads < MAX_RELOADS) {
          reloads += 1
          when (val refreshed = current.reloadSignUp()) {
            is ClerkResult.Success -> {
              current = refreshed.value
              continue
            }
            is ClerkResult.Failure -> return refreshed.toProtectFailure()
          }
        }
        return ProtectResolution.Failure()
      }

    when (val submitted = current.submitProtectCheck(proof)) {
      is ClerkResult.Success -> current = submitted.value
      is ClerkResult.Failure -> {
        if (submitted.isAlreadyResolved() && reloads < MAX_RELOADS) {
          reloads += 1
          when (val refreshed = current.reloadSignUp()) {
            is ClerkResult.Success -> current = refreshed.value
            is ClerkResult.Failure -> return refreshed.toProtectFailure()
          }
        } else {
          return submitted.toProtectFailure()
        }
      }
    }
  }
  return ProtectResolution.Success(current)
}

private fun SignIn.requiresProtectCheck(): Boolean =
  protectCheck != null || status == SignIn.Status.NEEDS_PROTECT_CHECK

private fun SignUp.requiresProtectCheck(): Boolean =
  protectCheck != null || PROTECT_CHECK_FIELD in missingFields

private fun ClerkResult.Failure<ClerkErrorResponse>.isAlreadyResolved(): Boolean =
  error?.errors?.firstOrNull()?.code == PROTECT_CHECK_ALREADY_RESOLVED

private fun ClerkResult.Failure<ClerkErrorResponse>.toProtectFailure(): ProtectResolution.Failure =
  ProtectResolution.Failure(errorMessage)

private sealed interface ProtectResolution<out T : Any> {
  data class Success<T : Any>(val resource: T) : ProtectResolution<T>

  data class Failure(val message: String? = null) : ProtectResolution<Nothing>
}

private class ProtectChallengeHost(context: Context) : FrameLayout(context) {
  private val progress = ProgressBar(context)
  val challengeContainer: ViewGroup =
    ChallengeContainer(context).apply {
      visibilityCallback = { visible -> progress.visibility = if (visible) GONE else VISIBLE }
    }

  init {
    addView(
      progress,
      LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER),
    )
    addView(challengeContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    prepare()
  }

  fun prepare() {
    challengeContainer.removeAllViews()
    challengeContainer.visibility = GONE
    progress.visibility = VISIBLE
  }

  fun finish() {
    challengeContainer.visibility = GONE
    progress.visibility = GONE
  }

  fun setRunning(running: Boolean) {
    if (!running) {
      finish()
    } else if (challengeContainer.visibility != VISIBLE) {
      progress.visibility = VISIBLE
    }
  }
}

private class ChallengeContainer(context: Context) : FrameLayout(context) {
  var visibilityCallback: ((Boolean) -> Unit)? = null

  override fun onVisibilityChanged(changedView: View, visibility: Int) {
    super.onVisibilityChanged(changedView, visibility)
    if (changedView === this) {
      visibilityCallback?.invoke(visibility == VISIBLE)
    }
  }
}

private const val MAX_RELOADS = 2
private const val PROTECT_CHECK_ALREADY_RESOLVED = "protect_check_already_resolved"
private const val PROTECT_CHECK_EXPIRED = "protect_check_expired"
private const val PROTECT_CHECK_FIELD = "protect_check"
