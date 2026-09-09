package com.clerk.testing

import com.clerk.api.Clerk
import com.clerk.api.ClerkAPIError
import com.clerk.api.CoreException
import com.clerk.api.User
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow

fun mockClerk(user: User? = null): Clerk {
  val clerk = mockk<Clerk>(relaxed = true)
  every { clerk.user } returns user
  every { clerk.environment.userSettings.attributes } returns emptyMap()
  every { clerk.context.requireRuntime().changes } returns MutableStateFlow(0L)
  return clerk
}

fun testCoreError(message: String, code: String = "fixture_error") =
  CoreException(code, message, errors = listOf(ClerkAPIError(code, message, longMessage = message)))

fun mockSession(
  id: String = "sess_123",
  status: com.clerk.api.SessionStatus = com.clerk.api.SessionStatus.Active,
  task: com.clerk.api.SessionTaskKey? = null,
): com.clerk.api.Session {
  val session = mockk<com.clerk.api.Session>(relaxed = true)
  every { session.id } returns id
  every { session.status } returns status
  every { session.currentTask } returns
    task?.let { key -> mockk<com.clerk.api.SessionTask> { every { this@mockk.key } returns key } }
  every { session.tasks } returns listOfNotNull(session.currentTask)
  return session
}

fun mockSignIn(
  status: com.clerk.api.SignInStatus = com.clerk.api.SignInStatus.Complete,
  createdSessionId: String? = "sess_123",
): com.clerk.api.SignIn {
  val signIn = mockk<com.clerk.api.SignIn>(relaxed = true)
  every { signIn.id } returns "sign_in_123"
  every { signIn.status } returns status
  every { signIn.createdSessionId } returns createdSessionId
  every { signIn.supportedFirstFactors } returns emptyList()
  every { signIn.supportedSecondFactors } returns emptyList()
  return signIn
}
