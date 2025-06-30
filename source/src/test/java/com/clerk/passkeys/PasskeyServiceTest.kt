package com.clerk.passkeys

import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.model.error.Error
import com.clerk.network.serialization.ClerkResult
import com.clerk.signin.SignIn
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PasskeyServiceTest {

  private lateinit var mockSignIn: SignIn
  private lateinit var mockPasskey: Passkey

  @Before
  fun setup() {
    mockSignIn = mockk(relaxed = true)
    mockPasskey = mockk(relaxed = true)

    // Mock the underlying services
    mockkObject(GoogleCredentialAuthenticationService)
    mockkObject(PasskeyCreationService)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `signInWithPasskey delegates to PasskeyAuthenticationService with empty list`() = runTest {
    // Given
    coEvery { GoogleCredentialAuthenticationService.signInWithGoogleCredential(any()) } returns
      ClerkResult.success(mockSignIn)

    // When
    val result = PasskeyService.signInWithPasskey()

    // Then
    assertTrue(result is ClerkResult.Success)
    assertEquals(mockSignIn, (result as ClerkResult.Success).value)
    coVerify { GoogleCredentialAuthenticationService.signInWithGoogleCredential(emptyList()) }
  }

  @Test
  fun `signInWithPasskey delegates to PasskeyAuthenticationService with credential IDs`() =
    runTest {
      // Given
      val allowedCredentialIds = listOf("credential-1", "credential-2", "credential-3")
      coEvery {
        GoogleCredentialAuthenticationService.signInWithGoogleCredential(allowedCredentialIds)
      } returns ClerkResult.success(mockSignIn)

      // When
      val result = PasskeyService.signInWithPasskey(allowedCredentialIds)

      // Then
      assertTrue(result is ClerkResult.Success)
      assertEquals(mockSignIn, (result as ClerkResult.Success).value)
      coVerify {
        GoogleCredentialAuthenticationService.signInWithGoogleCredential(allowedCredentialIds)
      }
    }

  @Test
  fun `signInWithPasskey returns error when PasskeyAuthenticationService fails`() = runTest {
    // Given
    val error =
      Error(
        code = "authentication_failed",
        message = "Auth failed",
        longMessage = "Authentication failed",
      )
    val errorResponse = ClerkErrorResponse(errors = listOf(error), clerkTraceId = "test-trace")

    coEvery { GoogleCredentialAuthenticationService.signInWithGoogleCredential(any()) } returns
      ClerkResult.apiFailure(errorResponse)

    // When
    val result = PasskeyService.signInWithPasskey()

    // Then
    assertTrue(result is ClerkResult.Failure)
    assertEquals(errorResponse, (result as ClerkResult.Failure).error)
    coVerify { GoogleCredentialAuthenticationService.signInWithGoogleCredential(emptyList()) }
  }

  @Test
  fun `createPasskey delegates to PasskeyCreationService`() = runTest {
    // Given
    coEvery { PasskeyCreationService.createPasskey() } returns ClerkResult.success(mockPasskey)

    // When
    val result = PasskeyService.createPasskey()

    // Then
    assertTrue(result is ClerkResult.Success)
    assertEquals(mockPasskey, (result as ClerkResult.Success).value)
    coVerify { PasskeyCreationService.createPasskey() }
  }

  @Test
  fun `createPasskey returns error when PasskeyCreationService fails`() = runTest {
    // Given
    val error =
      Error(
        code = "creation_failed",
        message = "Creation failed",
        longMessage = "Passkey creation failed",
      )
    val errorResponse = ClerkErrorResponse(errors = listOf(error), clerkTraceId = "test-trace")
    coEvery { PasskeyCreationService.createPasskey() } returns ClerkResult.apiFailure(errorResponse)

    // When
    val result = PasskeyService.createPasskey()

    // Then
    assertTrue(result is ClerkResult.Failure)
    assertEquals(errorResponse, (result as ClerkResult.Failure).error)
    coVerify { PasskeyCreationService.createPasskey() }
  }

  @Test
  fun `createPasskey handles unknown failure from PasskeyCreationService`() = runTest {
    // Given
    val exception = RuntimeException("Creation failed")
    coEvery { PasskeyCreationService.createPasskey() } returns ClerkResult.unknownFailure(exception)

    // When
    val result = PasskeyService.createPasskey()

    // Then
    assertTrue(result is ClerkResult.Failure)
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, (result as ClerkResult.Failure).errorType)
    coVerify { PasskeyCreationService.createPasskey() }
  }

  @Test
  fun `signInWithPasskey with large credential list delegates correctly`() = runTest {
    // Given
    val largeCredentialList = (1..100).map { "credential-$it" }
    coEvery {
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(largeCredentialList)
    } returns ClerkResult.success(mockSignIn)

    // When
    val result = PasskeyService.signInWithPasskey(largeCredentialList)

    // Then
    assertTrue(result is ClerkResult.Success)
    assertEquals(mockSignIn, (result as ClerkResult.Success).value)
    coVerify {
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(largeCredentialList)
    }
  }

  @Test
  fun `signInWithPasskey handles unknown failure from PasskeyAuthenticationService`() = runTest {
    // Given
    val exception = IllegalStateException("Unknown state")
    coEvery { GoogleCredentialAuthenticationService.signInWithGoogleCredential(any()) } returns
      ClerkResult.unknownFailure(exception)

    // When
    val result = PasskeyService.signInWithPasskey()

    // Then
    assertTrue(result is ClerkResult.Failure)
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, (result as ClerkResult.Failure).errorType)
    coVerify { GoogleCredentialAuthenticationService.signInWithGoogleCredential(emptyList()) }
  }
}
