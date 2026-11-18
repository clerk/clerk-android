package com.clerk.api.passkeys

import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
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

    mockkObject(GoogleCredentialAuthenticationService)
    mockkObject(PasskeyCreationService)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `signInWithPasskey delegates to PasskeyAuthenticationService with empty list`() = runTest {
    coEvery {
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        allowedCredentialIds = emptyList(),
        credentialTypes = listOf(SignIn.CredentialType.PASSKEY),
      )
    } returns ClerkResult.success(mockSignIn)

    val result = PasskeyService.signInWithPasskey()

    assertTrue(result is ClerkResult.Success)
    assertEquals(mockSignIn, (result as ClerkResult.Success).value)
    coVerify {
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        allowedCredentialIds = emptyList(),
        credentialTypes = listOf(SignIn.CredentialType.PASSKEY),
      )
    }
  }

  @Test
  fun `signInWithPasskey delegates to PasskeyAuthenticationService with credential IDs`() =
    runTest {
      val allowedCredentialIds = listOf("credential-1", "credential-2", "credential-3")
      coEvery {
        GoogleCredentialAuthenticationService.signInWithGoogleCredential(
          allowedCredentialIds = allowedCredentialIds,
          credentialTypes = listOf(SignIn.CredentialType.PASSKEY),
        )
      } returns ClerkResult.success(mockSignIn)

      val result = PasskeyService.signInWithPasskey(allowedCredentialIds)

      assertTrue(result is ClerkResult.Success)
      assertEquals(mockSignIn, (result as ClerkResult.Success).value)
      coVerify {
        GoogleCredentialAuthenticationService.signInWithGoogleCredential(
          allowedCredentialIds = allowedCredentialIds,
          credentialTypes = listOf(SignIn.CredentialType.PASSKEY),
        )
      }
    }

  @Test
  fun `signInWithPasskey can prefer immediately available credentials`() = runTest {
    coEvery {
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        allowedCredentialIds = emptyList(),
        credentialTypes = listOf(SignIn.CredentialType.PASSKEY),
        preferImmediatelyAvailableCredentials = true,
      )
    } returns ClerkResult.success(mockSignIn)

    val result = PasskeyService.signInWithPasskey(preferImmediatelyAvailableCredentials = true)

    assertTrue(result is ClerkResult.Success)
    assertEquals(mockSignIn, (result as ClerkResult.Success).value)
    coVerify {
      GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        allowedCredentialIds = emptyList(),
        credentialTypes = listOf(SignIn.CredentialType.PASSKEY),
        preferImmediatelyAvailableCredentials = true,
      )
    }
  }

  @Test
  fun `authenticateWithPasskey delegates an existing sign in`() = runTest {
    val allowedCredentialIds = listOf("credential-1")
    coEvery {
      GoogleCredentialAuthenticationService.authenticateWithPasskey(
        signIn = mockSignIn,
        allowedCredentialIds = allowedCredentialIds,
      )
    } returns ClerkResult.success(mockSignIn)

    val result = PasskeyService.authenticateWithPasskey(mockSignIn, allowedCredentialIds)

    assertTrue(result is ClerkResult.Success)
    assertEquals(mockSignIn, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) {
      GoogleCredentialAuthenticationService.authenticateWithPasskey(
        signIn = mockSignIn,
        allowedCredentialIds = allowedCredentialIds,
      )
    }
  }

  @Test
  fun `createPasskey delegates to PasskeyCreationService`() = runTest {
    coEvery { PasskeyCreationService.createPasskey() } returns ClerkResult.success(mockPasskey)

    val result = PasskeyService.createPasskey()

    assertTrue(result is ClerkResult.Success)
    assertEquals(mockPasskey, (result as ClerkResult.Success).value)
    coVerify { PasskeyCreationService.createPasskey() }
  }
}
