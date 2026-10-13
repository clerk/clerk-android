package com.clerk.ui.protect

import android.content.Context
import android.content.MutableContextWrapper
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clerk.ui.R
import com.clerk.ui.core.button.standard.ClerkButton
import com.clerk.ui.core.composition.LocalAuthState
import com.clerk.ui.core.scaffold.ClerkThemedAuthScaffold

@Composable
internal fun ProtectCheckView(
  flow: ProtectCheckFlow,
  onAuthComplete: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: ProtectCheckViewModel = viewModel(),
) {
  val authState = LocalAuthState.current
  val state by viewModel.state.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val fallbackError = stringResource(R.string.something_went_wrong_please_try_again)

  LaunchedEffect(flow) { viewModel.start(flow) }

  LaunchedEffect(state) {
    when (val current = state) {
      is ProtectCheckViewModel.State.SignInCompleted -> {
        viewModel.completionHandled()
        authState.setToStepForStatus(current.signIn, onAuthComplete = onAuthComplete)
      }
      is ProtectCheckViewModel.State.SignUpCompleted -> {
        viewModel.completionHandled()
        authState.setToStepForStatus(current.signUp, onAuthComplete = onAuthComplete)
      }
      is ProtectCheckViewModel.State.Failed ->
        snackbarHostState.showSnackbar(current.message ?: fallbackError)
      else -> Unit
    }
  }

  val failed = state is ProtectCheckViewModel.State.Failed
  ClerkThemedAuthScaffold(
    modifier = modifier,
    snackbarHostState = snackbarHostState,
    title = stringResource(R.string.security),
    subtitle = stringResource(R.string.to_continue),
    hasBackButton = false,
  ) {
    AndroidView(
      modifier = Modifier.fillMaxWidth().height(if (failed) 0.dp else 320.dp),
      factory = viewModel::attachHost,
      onRelease = viewModel::releaseHost,
      update = { host -> host.setRunning(state == ProtectCheckViewModel.State.Running) },
    )
    if (failed) {
      ClerkButton(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.try_again),
        onClick = { viewModel.retry(flow) },
      )
    }
  }
}

internal class ProtectChallengeHost(private val contextWrapper: MutableContextWrapper) :
  FrameLayout(contextWrapper) {
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

  fun rebind(context: Context) {
    contextWrapper.baseContext = context
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
