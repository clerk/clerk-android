package com.clerk.api.signin

import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.passkeys.PasskeyService
import com.clerk.api.trusteddevice.TrustedDevices
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
        SignIn.CreateParams.Strategy.Passkey(),
        preferImmediatelyAvailableCredentials = true,
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

  @Test
  fun `passkey strategy preserves legacy jvm constructor and copy signatures`() {
    val passkeyClass = SignIn.CreateParams.Strategy.Passkey::class.java

    passkeyClass.getDeclaredConstructor(String::class.java)
    passkeyClass.getDeclaredMethod("copy", String::class.java)
  }

  @Test
  fun `create with trusted device strategy forwards prompt configuration`() = runTest {
    val signIn = SignIn(id = "sign_in_123")
    mockkObject(TrustedDevices)
    coEvery {
      TrustedDevices.signIn(
        id = "td_123",
        identifierHint = "user@example.com",
        promptTitle = "Welcome back",
        promptSubtitle = "Use your screen lock",
      )
    } returns ClerkResult.success(signIn)

    val result =
      SignIn.create(
        SignIn.CreateParams.Strategy.TrustedDevice(
          id = "td_123",
          identifierHint = "user@example.com",
          promptTitle = "Welcome back",
          promptSubtitle = "Use your screen lock",
        )
      )

    assertTrue(result is ClerkResult.Success)
    assertEquals(signIn, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) {
      TrustedDevices.signIn(
        id = "td_123",
        identifierHint = "user@example.com",
        promptTitle = "Welcome back",
        promptSubtitle = "Use your screen lock",
      )
    }
  }
}
