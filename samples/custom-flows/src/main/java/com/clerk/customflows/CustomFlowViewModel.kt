package com.clerk.customflows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.SessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

abstract class CustomFlowViewModel(
  protected val clerk: Clerk,
  protected val feedback: CustomFlowFeedback,
) : ViewModel() {
  protected fun runOperation(operation: suspend () -> Unit) {
    viewModelScope.launch {
      feedback.error.value = null
      try {
        operation()
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        feedback.error.value = error.localizedMessage ?: "Please try again."
      }
    }
  }

  protected fun observeSession(onChanged: (Boolean) -> Unit) {
    clerk.changes
      .map {
        it.user != null &&
          it.session?.status == SessionStatus.Active &&
          it.session?.currentTask == null
      }
      .distinctUntilChanged()
      .onEach(onChanged)
      .launchIn(viewModelScope)
  }
}
