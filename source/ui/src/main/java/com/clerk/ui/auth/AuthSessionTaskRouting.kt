package com.clerk.ui.auth

import androidx.navigation3.runtime.NavKey
import com.clerk.api.Clerk
import com.clerk.api.Session
import com.clerk.api.SessionTaskKey
import com.clerk.api.SignIn
import com.clerk.api.SignUp

/** Never route one completed attempt using a different active session. */
internal fun SignIn.correspondingSession(clerk: Clerk): Session? =
  resolveCorrespondingSession(createdSessionId, clerk.sessions, clerk.session)

internal fun SignUp.correspondingSession(clerk: Clerk): Session? =
  resolveCorrespondingSession(createdSessionId, clerk.sessions, clerk.session)

internal fun resolveCorrespondingSession(
  createdSessionId: String?,
  sessions: List<Session>,
  fallbackSession: Session?,
): Session? =
  if (createdSessionId == null) fallbackSession
  else
    sessions.firstOrNull { it.id == createdSessionId }
      ?: fallbackSession?.takeIf { it.id == createdSessionId }

internal fun postAuthCompletionAction(
  taskKey: SessionTaskKey?,
  hasUnresolvedCreatedSession: Boolean,
): PostAuthCompletionAction =
  when {
    taskKey == SessionTaskKey.SetupMfa -> PostAuthCompletionAction.ROUTE_TO_MFA
    taskKey == SessionTaskKey.ResetPassword -> PostAuthCompletionAction.ROUTE_TO_RESET_PASSWORD
    taskKey == SessionTaskKey.ChooseOrganization ->
      PostAuthCompletionAction.ROUTE_TO_CHOOSE_ORGANIZATION
    taskKey != null || hasUnresolvedCreatedSession -> PostAuthCompletionAction.ROUTE_TO_HELP
    else -> PostAuthCompletionAction.COMPLETE_AUTH
  }

internal enum class PostAuthCompletionAction {
  ROUTE_TO_MFA,
  ROUTE_TO_RESET_PASSWORD,
  ROUTE_TO_CHOOSE_ORGANIZATION,
  ROUTE_TO_HELP,
  COMPLETE_AUTH,
}

internal fun Session?.pendingSessionTaskKey(): SessionTaskKey? {
  return this?.currentTask?.key
}

internal fun AuthState.handleSessionTaskCompletion(session: Session?, onAuthComplete: () -> Unit) {
  if (session == null) {
    clearBackStack()
    return
  }
  when (session.pendingSessionTaskKey()) {
    SessionTaskKey.SetupMfa -> replaceSessionTaskDestination(AuthDestination.SessionTaskMfa)
    SessionTaskKey.ResetPassword ->
      replaceSessionTaskDestination(AuthDestination.SessionTaskResetPassword)
    SessionTaskKey.ChooseOrganization ->
      replaceSessionTaskDestination(AuthDestination.SessionTaskChooseOrganization)
    is SessionTaskKey.Unrecognized -> replaceSessionTaskDestination(AuthDestination.SignInGetHelp)
    null -> completePresentation(onAuthComplete)
  }
}

private fun AuthState.replaceSessionTaskDestination(destination: NavKey) {
  if (backStack.lastOrNull().isSessionTaskDestination()) {
    backStack.removeLastOrNull()
  }
  if (backStack.lastOrNull() != destination) {
    backStack.add(destination)
  }
}

internal fun NavKey?.isSessionTaskDestination(): Boolean {
  return this == AuthDestination.SessionTaskMfa ||
    this == AuthDestination.SessionTaskResetPassword ||
    this == AuthDestination.SessionTaskChooseOrganization ||
    this is AuthDestination.SessionTaskCreateOrganization
}
