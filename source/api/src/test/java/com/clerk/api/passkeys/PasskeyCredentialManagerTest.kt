package com.clerk.api.passkeys

import android.content.Context
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class PasskeyCredentialManagerTest {

  private val context = mockk<Context>()
  private val systemCredentialManager = mockk<CredentialManager>()

  @Before
  fun setup() {
    mockkObject(CredentialManager.Companion)
    every { CredentialManager.create(any()) } returns systemCredentialManager
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `createCredential delegates to the system credential manager for the given context`() =
    runTest {
      val request = mockk<CreatePublicKeyCredentialRequest>()
      val response = mockk<CreateCredentialResponse>()
      coEvery { systemCredentialManager.createCredential(context, request) } returns response

      val result = PasskeyCredentialManagerImpl().createCredential(context, request)

      assertSame(response, result)
      verify(exactly = 1) { CredentialManager.create(context) }
      coVerify(exactly = 1) { systemCredentialManager.createCredential(context, request) }
    }

  @Test
  fun `getCredential delegates to the system credential manager for the given context`() = runTest {
    val request = mockk<GetCredentialRequest>()
    val response = mockk<GetCredentialResponse>()
    coEvery { systemCredentialManager.getCredential(context, request) } returns response

    val result = PasskeyCredentialManagerImpl().getCredential(context, request)

    assertSame(response, result)
    verify(exactly = 1) { CredentialManager.create(context) }
    coVerify(exactly = 1) { systemCredentialManager.getCredential(context, request) }
  }
}
