package com.clerk.api.sso

import android.app.Activity
import com.clerk.api.Clerk
import com.clerk.api.credentials.CredentialFlowException
import com.clerk.api.network.model.environment.DisplayConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.serialization.ClerkResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GoogleCredentialManagerImplTest {

  @Suppress("DEPRECATION") private val credentialManager = GoogleCredentialManagerImpl()

  @Before
  fun setup() {
    val displayConfig = mockk<DisplayConfig>(relaxed = true)
    val environment = mockk<Environment>(relaxed = true)
    every { displayConfig.googleOneTapClientId } returns null
    every { environment.displayConfig } returns displayConfig

    mockkObject(Clerk)
    every { Clerk.environment } returns environment
    every { Clerk.credentialActivity() } returns mockk<Activity>(relaxed = true)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `getGoogleIdOption reports a missing client id as a credential flow failure`() {
    assertThrows(CredentialFlowException.GoogleOneTapNotConfigured::class.java) {
      credentialManager.getGoogleIdOption()
    }
  }

  @Test
  fun `getSignInWithGoogleCredential reports a missing client id as a credential flow failure`() =
    runTest {
      assertFailsWith<CredentialFlowException.GoogleOneTapNotConfigured> {
        credentialManager.getSignInWithGoogleCredential()
      }
    }

  @Test
  fun `signInWithGoogle returns a failure instead of throwing when One Tap is not configured`() =
    runTest {
      val result = GoogleSignInService(credentialManager).signInWithGoogle()

      assertTrue(result is ClerkResult.Failure)
      assertTrue(
        (result as ClerkResult.Failure).throwable
          is CredentialFlowException.GoogleOneTapNotConfigured
      )
    }

  @Test
  fun `signUpWithGoogle returns a failure instead of throwing when One Tap is not configured`() =
    runTest {
      val result = GoogleSignInService(credentialManager).signUpWithGoogle()

      assertTrue(result is ClerkResult.Failure)
      assertTrue(
        (result as ClerkResult.Failure).throwable
          is CredentialFlowException.GoogleOneTapNotConfigured
      )
    }
}
