package com.clerk.ui.auth

import com.clerk.api.Clerk
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp

internal suspend fun SignIn.resolvePostAuthSession(): Session? {
  return if (status == SignIn.Status.COMPLETE) {
    refreshPostAuthSession(createdSessionId, correspondingSession())
  } else {
    correspondingSession()
  }
}

internal suspend fun SignUp.resolvePostAuthSession(): Session? {
  return if (status == SignUp.Status.COMPLETE) {
    refreshPostAuthSession(createdSessionId, correspondingSession())
  } else {
    correspondingSession()
  }
}

private suspend fun refreshPostAuthSession(
  createdSessionId: String?,
  currentSession: Session?,
): Session? {
  return currentSession
    ?: when (val result = Client.get()) {
      is ClerkResult.Success -> {
        val sessions = result.value.sessions
        val fallbackSession =
          sessions.firstOrNull { it.id == result.value.lastActiveSessionId } ?: Clerk.session
        resolveCorrespondingSession(
          createdSessionId = createdSessionId,
          sessions = sessions,
          fallbackSession = fallbackSession,
        )
      }
      is ClerkResult.Failure -> Clerk.session
    }
}
