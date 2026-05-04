package com.clerk.ui.auth

import androidx.navigation3.runtime.NavKey
import com.clerk.api.Clerk
import com.clerk.api.session.Session
import com.clerk.api.session.SessionTaskKey
import com.clerk.api.session.pendingTaskKey
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp

internal fun SignIn.pendingSessionTaskKey(
  session: Session? = this.correspondingSession()
): SessionTaskKey? {
  return if (status == SignIn.Status.COMPLETE) session.pendingSessionTaskKey() else null
}

internal fun SignIn.correspondingSession(): Session? {
  val sessions = runCatching { Clerk.client.sessions }.getOrDefault(emptyList())
  return resolveCorrespondingSession(
    createdSessionId = createdSessionId,
    sessions = sessions,
    fallbackSession = Clerk.session,
  )
}

internal fun SignUp.pendingSessionTaskKey(
  session: Session? = this.correspondingSession()
): SessionTaskKey? {
  return if (status == SignUp.Status.COMPLETE) session.pendingSessionTaskKey() else null
}

internal fun SignUp.correspondingSession(): Session? {
  val sessions = runCatching { Clerk.client.sessions }.getOrDefault(emptyList())
  return resolveCorrespondingSession(
    createdSessionId = createdSessionId,
    sessions = sessions,
    fallbackSession = Clerk.session,
  )
}

internal fun resolveCorrespondingSession(
  createdSessionId: String?,
  sessions: List<Session>,
  fallbackSession: Session?,
): Session? {
  return if (createdSessionId == null) {
    fallbackSession
  } else {
    sessions.firstOrNull { it.id == createdSessionId } ?: fallbackSession
  }
}

internal fun postAuthCompletionAction(
  taskKey: SessionTaskKey?,
  hasUnresolvedCreatedSession: Boolean,
  shouldChooseOrganizationForCreatedSession: Boolean,
): PostAuthCompletionAction {
  return when {
    taskKey == SessionTaskKey.MFA_REQUIRED -> PostAuthCompletionAction.ROUTE_TO_MFA
    taskKey == SessionTaskKey.RESET_PASSWORD -> PostAuthCompletionAction.ROUTE_TO_RESET_PASSWORD
    taskKey == SessionTaskKey.CHOOSE_ORGANIZATION ->
      PostAuthCompletionAction.ROUTE_TO_CHOOSE_ORGANIZATION
    taskKey == SessionTaskKey.UNKNOWN -> PostAuthCompletionAction.ROUTE_TO_HELP
    hasUnresolvedCreatedSession -> PostAuthCompletionAction.ROUTE_TO_MFA
    shouldChooseOrganizationForCreatedSession ->
      PostAuthCompletionAction.ROUTE_TO_CHOOSE_ORGANIZATION
    else -> PostAuthCompletionAction.COMPLETE_AUTH
  }
}

internal enum class PostAuthCompletionAction {
  ROUTE_TO_MFA,
  ROUTE_TO_RESET_PASSWORD,
  ROUTE_TO_CHOOSE_ORGANIZATION,
  ROUTE_TO_HELP,
  COMPLETE_AUTH,
}

internal fun Session?.pendingSessionTaskKey(): SessionTaskKey? {
  return this?.pendingTaskKey
}

internal fun AuthState.handleSessionTaskCompletion(session: Session?, onAuthComplete: () -> Unit) {
  when (session.pendingSessionTaskKey()) {
    SessionTaskKey.MFA_REQUIRED -> replaceSessionTaskDestination(AuthDestination.SessionTaskMfa)
    SessionTaskKey.RESET_PASSWORD ->
      replaceSessionTaskDestination(AuthDestination.SessionTaskResetPassword)
    SessionTaskKey.CHOOSE_ORGANIZATION ->
      replaceSessionTaskDestination(AuthDestination.SessionTaskChooseOrganization)
    SessionTaskKey.UNKNOWN -> replaceSessionTaskDestination(AuthDestination.SignInGetHelp)
    null -> onAuthComplete()
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
