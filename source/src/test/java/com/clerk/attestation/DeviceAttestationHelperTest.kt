package com.clerk.attestation

import android.content.Context
import com.clerk.network.ClerkApi
import com.clerk.network.api.DeviceAttestationApi
import com.clerk.network.serialization.ClerkResult
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceAttestationHelperTest {

  private val mockContext = mockk<Context>(relaxed = true)
  private val mockIntegrityManager = mockk<StandardIntegrityManager>(relaxed = true)
  private val mockDeviceAttestationApi = mockk<DeviceAttestationApi>(relaxed = true)

  @Before
  fun setup() {
    // Mock static dependencies
    mockkStatic(IntegrityManagerFactory::class)
    mockkObject(ClerkApi)

    // Set up default behavior
    every { IntegrityManagerFactory.createStandard(any()) } returns mockIntegrityManager
    every { ClerkApi.deviceAttestationApi } returns mockDeviceAttestationApi

    // Reset the helper state
    DeviceAttestationHelper.integrityManager = null
    DeviceAttestationHelper.integrityTokenProvider = null
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `prepareIntegrityTokenProvider throws exception when cloudProjectNumber is null`() = runTest {
    // When & Then
    try {
      DeviceAttestationHelper.prepareIntegrityTokenProvider(mockContext, null)
      throw AssertionError("Expected IllegalArgumentException to be thrown")
    } catch (e: IllegalArgumentException) {
      assertEquals("Cloud project number is required", e.message)
    }
  }

  @Test
  fun `attestDevice throws exception when token provider is null`() = runTest {
    // Given
    DeviceAttestationHelper.integrityTokenProvider = null

    // When & Then
    try {
      DeviceAttestationHelper.attestDevice("client-id")
      throw AssertionError("Expected IllegalArgumentException to be thrown")
    } catch (e: IllegalArgumentException) {
      assertNotNull("Exception message should not be null", e.message)
      assertTrue(
        "Exception message should mention token provider",
        e.message!!.contains("Integrity token provider must not be null"),
      )
    }
  }

  @Test
  fun `performAssertion calls device attestation API`() = runTest {
    // Given
    val token = "test-token"
    val applicationId = "com.example.app"
    val mockClient = mockk<com.clerk.network.model.client.Client>()

    coEvery { mockDeviceAttestationApi.verify(any(), any()) } returns
      ClerkResult.success(mockClient)

    // When
    val result = DeviceAttestationHelper.performAssertion(token, applicationId)

    // Then
    assertTrue("Result should be success", result is ClerkResult.Success)
    assertEquals(mockClient, (result as ClerkResult.Success).value)
  }

  @Test
  fun `getHashedClientId generates correct SHA-256 hash`() {
    // Given
    val clientId = "test-client-id"

    // When
    val result = DeviceAttestationHelper.getHashedClientId(clientId)

    // Then
    assertNotNull(result)
    assertEquals(64, result.length) // SHA-256 produces 64 character hex string
    assertTrue(result.matches(Regex("[0-9a-f]{64}"))) // Should be valid hex string
  }

  @Test
  fun `getHashedClientId handles different input values consistently`() {
    // Test different inputs
    val inputs =
      listOf("", "a", "test-client-id", "another-longer-client-id-with-special-chars!@#$%")

    inputs.forEach { input ->
      val result1 = DeviceAttestationHelper.getHashedClientId(input)
      val result2 = DeviceAttestationHelper.getHashedClientId(input)

      // Same input should always produce same hash
      assertEquals("Hash should be consistent for input: $input", result1, result2)
      assertEquals("Hash should be 64 characters for input: $input", 64, result1.length)
      assertTrue(
        "Hash should be valid hex for input: $input",
        result1.matches(Regex("[0-9a-f]{64}")),
      )
    }
  }

  @Test
  fun `getHashedClientId produces different hashes for different inputs`() {
    // Given different inputs
    val input1 = "client-id-1"
    val input2 = "client-id-2"

    // When
    val hash1 = DeviceAttestationHelper.getHashedClientId(input1)
    val hash2 = DeviceAttestationHelper.getHashedClientId(input2)

    // Then
    assertTrue("Different inputs should produce different hashes", hash1 != hash2)
  }

  @Test
  fun `scope is properly configured`() {
    // Then
    assertNotNull(DeviceAttestationHelper.scope)
  }
}
