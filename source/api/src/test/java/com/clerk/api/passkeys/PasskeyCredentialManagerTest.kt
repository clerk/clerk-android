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
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PasskeyCredentialManagerTest {

  private val context = mockk<Context>(relaxed = true)
  private val credentialManager = mockk<CredentialManager>()

  @Before
  fun setUp() {
    mockkObject(CredentialManager.Companion)
    every { CredentialManager.create(context) } returns credentialManager
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `createCredential delegates to CredentialManager and returns its response`() = runTest {
    val request = mockk<CreatePublicKeyCredentialRequest>()
    val response = mockk<CreateCredentialResponse>()
    coEvery { credentialManager.createCredential(context, request) } returns response

    val result = PasskeyCredentialManagerImpl().createCredential(context, request)

    assertSame(response, result)
    coVerify(exactly = 1) { credentialManager.createCredential(context, request) }
  }

  @Test
  fun `getCredential delegates to CredentialManager and returns its response`() = runTest {
    val request = mockk<GetCredentialRequest>()
    val response = mockk<GetCredentialResponse>()
    coEvery { credentialManager.getCredential(context, request) } returns response

    val result = PasskeyCredentialManagerImpl().getCredential(context, request)

    assertSame(response, result)
    coVerify(exactly = 1) { credentialManager.getCredential(context, request) }
  }
}
