package com.clerk.api.passkeys

import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.GetCredentialRequest
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PasskeyCredentialManagerTest {

  @Test
  fun `PasskeyCredentialManagerImpl can be used as PasskeyCredentialManager interface`() {
    val manager: PasskeyCredentialManager = PasskeyCredentialManagerImpl()

    assert(manager is PasskeyCredentialManagerImpl) { "Should be the correct implementation" }
  }

  @Test
  fun `createCredential method exists and can be called`() = runTest {
    val mockContext = mockk<Context>(relaxed = true)
    val mockRequest = mockk<CreatePublicKeyCredentialRequest>(relaxed = true)

    val manager = PasskeyCredentialManagerImpl()

    try {
      manager.createCredential(mockContext, mockRequest)
    } catch (expected: Exception) {}
  }

  @Test
  fun `getCredential method exists and can be called`() = runTest {
    val mockContext = mockk<Context>(relaxed = true)
    val mockRequest = mockk<GetCredentialRequest>(relaxed = true)

    val manager = PasskeyCredentialManagerImpl()

    try {
      manager.getCredential(mockContext, mockRequest)
    } catch (expected: Exception) {}
  }
}
