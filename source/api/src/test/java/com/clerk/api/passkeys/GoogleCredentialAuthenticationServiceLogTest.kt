package com.clerk.api.passkeys

import android.app.Activity
import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
class GoogleCredentialAuthenticationServiceLogTest {

  private val mockSignInApi: SignInApi = mockk(relaxed = true)

  @Before
  fun setup() {
    mockkObject(Clerk, ClerkApi)
    every { Clerk.credentialActivity() } returns mockk<Activity>(relaxed = true)
    every { Clerk.debugMode } returns false
    every { ClerkApi.signIn } returns mockSignInApi
    GoogleCredentialAuthenticationService.setCredentialManager(mockk(relaxed = true))
  }

  @After
  fun tearDown() {
    GoogleCredentialAuthenticationService.setCredentialManager(PasskeyCredentialManagerImpl())
    unmockkAll()
  }

  @Test
  fun `signInWithGoogleCredential logs the error code but not the trace id or meta`() = runTest {
    val errorResponse =
      ClerkErrorResponse(
        errors =
          listOf(
            Error(
              code = "signin_error",
              message = "Failed to create signin",
              meta = JsonObject(mapOf("session_id" to JsonPrimitive("sess_in_error_meta"))),
            )
          ),
        meta = JsonObject(mapOf("client_id" to JsonPrimitive("client_in_meta"))),
        clerkTraceId = "trace_secret",
      )
    coEvery { mockSignInApi.createSignIn(any()) } returns ClerkResult.apiFailure(errorResponse)
    ShadowLog.clear()

    GoogleCredentialAuthenticationService.signInWithGoogleCredential(
      listOf(SignIn.CredentialType.PASSKEY)
    )

    val logs = ShadowLog.getLogs()
    assertTrue(
      logs.any {
        it.msg.contains("Failed to create SignIn: API signin_error: Failed to create signin")
      }
    )
    assertTrue(
      logs.none {
        it.msg.contains("trace_secret") ||
          it.msg.contains("sess_in_error_meta") ||
          it.msg.contains("client_in_meta")
      }
    )
  }
}
