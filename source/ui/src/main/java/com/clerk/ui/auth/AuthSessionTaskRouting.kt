package com.clerk.ui.auth

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

internal fun Session?.pendingSessionTaskKey(): SessionTaskKey? {
  return this?.pendingTaskKey
}
