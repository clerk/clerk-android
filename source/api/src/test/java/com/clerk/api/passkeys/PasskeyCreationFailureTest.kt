package com.clerk.api.passkeys

import android.app.Activity
import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.UserApi
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.LocalFailureCodes
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PasskeyCreationFailureTest {
  private val credentialManager = mockk<PasskeyCredentialManager>(relaxed = true)
  private val userApi = mockk<UserApi>(relaxed = true)
  private val passkey = mockk<Passkey>(relaxed = true)

  @Before
  fun setup() {
    mockkObject(Clerk)
    every { Clerk.credentialActivity() } returns mockk<Activity>(relaxed = true)
    every { Clerk.session } returns null
    mockkObject(ClerkApi)
    every { ClerkApi.user } returns userApi
    every { passkey.id } returns "passkey_123"
    coEvery { userApi.createPasskey(any()) } returns ClerkResult.success(passkey)
    PasskeyCreationService.setCredentialManager(credentialManager)
  }

  @After
  fun tearDown() {
    PasskeyCreationService.setCredentialManager(PasskeyCredentialManagerImpl())
    unmockkAll()
  }

  @Test
  fun `createPasskey rethrows coroutine cancellation instead of returning a failure`() = runTest {
    every { passkey.verification } returns Verification(nonce = REGISTRATION_REQUEST_JSON)
    coEvery { credentialManager.createCredential(any(), any()) } throws
      CancellationException("caller cancelled")

    val thrown = runCatching { PasskeyCreationService.createPasskey() }.exceptionOrNull()

    assertTrue("Expected cancellation but got $thrown", thrown is CancellationException)
  }

  @Test
  fun `createPasskey returns a failure when the creation response has no nonce`() = runTest {
    every { passkey.verification } returns Verification(nonce = null)

    val result = PasskeyCreationService.createPasskey()

    val failure = result as ClerkResult.Failure
    assertEquals(ClerkResult.Failure.ErrorType.API, failure.errorType)
    assertEquals(
      LocalFailureCodes.MISSING_RESOURCE_DATA,
      failure.error?.errors?.single()?.code,
    )
    coVerify(exactly = 0) { credentialManager.createCredential(any(), any()) }
  }

  private companion object {
    const val REGISTRATION_REQUEST_JSON =
      """{"challenge":"Y2hhbGxlbmdl","rp":{"id":"example.com","name":"Example"},""" +
        """"user":{"id":"dXNlcg","name":"user@example.com","displayName":"User"},""" +
        """"pubKeyCredParams":[{"type":"public-key","alg":-7}]}"""
  }
}
