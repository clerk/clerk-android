package com.clerk.api.network.api

import com.clerk.api.Clerk
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.network.ApiParams
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.user.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import retrofit2.http.Query

class UserApiTest {

  @Test
  fun `create email address defaults to active session id`() = runTest {
    val user =
      User(
        id = "user_123",
        imageUrl = "",
        hasImage = false,
        passkeys = emptyList(),
        passwordEnabled = false,
        phoneNumbers = emptyList(),
        totpEnabled = false,
        twoFactorEnabled = false,
        updatedAt = 0L,
      )
    val session =
      Session(
        id = "sess_123",
        status = Session.SessionStatus.ACTIVE,
        expireAt = 0L,
        lastActiveAt = 0L,
        user = user,
        createdAt = 0L,
        updatedAt = 0L,
      )
    Clerk.updateClient(Client(sessions = listOf(session), lastActiveSessionId = session.id))
    val api = mockk<UserApi>()
    val result =
      ClerkResult.success(EmailAddress(id = "email_123", emailAddress = "new@example.com"))
    coEvery {
      api.createEmailAddress(emailAddress = "new@example.com", sessionId = session.id)
    } returns result

    try {
      api.createEmailAddress(emailAddress = "new@example.com")

      coVerify(exactly = 1) {
        api.createEmailAddress(emailAddress = "new@example.com", sessionId = session.id)
      }
    } finally {
      Clerk.updateClient(Client())
    }
  }

  @Test
  fun `passkey endpoints include clerk session id query`() {
    val passkeyMethods =
      listOf(
        "createPasskey",
        "getPasskey",
        "deletePasskey",
        "updatePasskey",
        "attemptPasskeyVerification",
      )

    passkeyMethods.forEach { methodName ->
      assertTrue(
        method(methodName).hasQuery(ApiParams.CLERK_SESSION_ID),
        "$methodName should include _clerk_session_id",
      )
    }
  }

  @Test
  fun `mfa endpoints include clerk session id query`() {
    val mfaMethods =
      listOf("createTOTP", "deleteTOTP", "attemptTOTPVerification", "createBackupCodes")

    mfaMethods.forEach { methodName ->
      assertTrue(
        method(methodName).hasQuery(ApiParams.CLERK_SESSION_ID),
        "$methodName should include _clerk_session_id",
      )
    }
  }

  @Test
  fun `external account linking endpoints include clerk session id query`() {
    val linkingMethods = listOf("createExternalAccount", "reauthorizeExternalAccount")

    linkingMethods.forEach { methodName ->
      assertTrue(
        method(methodName).hasQuery(ApiParams.CLERK_SESSION_ID),
        "$methodName should include _clerk_session_id",
      )
    }
  }

  private fun method(name: String): java.lang.reflect.Method {
    return UserApi::class.java.methods.single { it.name == name }
  }

  private fun java.lang.reflect.Method.hasQuery(value: String): Boolean {
    return parameterAnnotations.any { annotations ->
      annotations.filterIsInstance<Query>().any { it.value == value }
    }
  }
}
