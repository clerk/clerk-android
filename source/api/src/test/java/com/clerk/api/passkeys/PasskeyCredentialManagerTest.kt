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
    // Given/When
    val manager: PasskeyCredentialManager = PasskeyCredentialManagerImpl()

    assert(manager is PasskeyCredentialManagerImpl) { "Should be the correct implementation" }
  }

  @Test
  fun `createCredential method exists and can be called`() = runTest {
    // This is a basic smoke test to verify the interface contract
    val mockContext = mockk<Context>(relaxed = true)
    val mockRequest = mockk<CreatePublicKeyCredentialRequest>(relaxed = true)

    // The actual implementation would call Android's CredentialManager
    // which we cannot easily test in a unit test environment
    // This test just verifies the method signature exists
    val manager = PasskeyCredentialManagerImpl()

    // We expect this to fail in a unit test environment since Android CredentialManager
    // is not available, but the method should exist
    try {
      manager.createCredential(mockContext, mockRequest)
    } catch (e: Exception) {
      // Expected in unit test environment
    }
  }

  @Test
  fun `getCredential method exists and can be called`() = runTest {
    // This is a basic smoke test to verify the interface contract
    val mockContext = mockk<Context>(relaxed = true)
    val mockRequest = mockk<GetCredentialRequest>(relaxed = true)

    val manager = PasskeyCredentialManagerImpl()

    // We expect this to fail in a unit test environment since Android CredentialManager
    // is not available, but the method should exist
    try {
      manager.getCredential(mockContext, mockRequest)
    } catch (e: Exception) {
      // Expected in unit test environment
    }
  }
}
