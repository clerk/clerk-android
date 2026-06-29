package com.clerk.api.signin

import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.passkeys.PasskeyService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignInCreateTest {

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `create with passkey strategy delegates to PasskeyService`() = runTest {
    val signIn = SignIn(id = "sign_in_123")
    mockkObject(PasskeyService)
    coEvery {
      PasskeyService.signInWithPasskey(
        allowedCredentialIds = emptyList(),
        preferImmediatelyAvailableCredentials = true,
      )
    } returns ClerkResult.success(signIn)

    val result =
      SignIn.create(
        SignIn.CreateParams.Strategy.Passkey(preferImmediatelyAvailableCredentials = true)
      )

    assertTrue(result is ClerkResult.Success)
    assertEquals(signIn, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) {
      PasskeyService.signInWithPasskey(
        allowedCredentialIds = emptyList(),
        preferImmediatelyAvailableCredentials = true,
      )
    }
  }
}
