import android.content.Context
import com.clerk.api.Constants.Attestation.SHA256_HEX_LENGTH
import com.clerk.api.attestation.DeviceAttestationHelper
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.DeviceAttestationApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.serialization.ClerkResult
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CancellationException
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
    mockkStatic(IntegrityManagerFactory::class)
    mockkObject(ClerkApi)

    every { IntegrityManagerFactory.createStandard(any()) } returns mockIntegrityManager
    every { ClerkApi.deviceAttestation } returns mockDeviceAttestationApi

    DeviceAttestationHelper.integrityManager = null
    DeviceAttestationHelper.integrityTokenProvider = null
    DeviceAttestationHelper.clearCache()
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `prepareIntegrityTokenProvider throws exception when cloudProjectNumber is null`() = runTest {
    try {
      DeviceAttestationHelper.prepareIntegrityTokenProvider(mockContext, null)
      throw AssertionError("Expected IllegalArgumentException to be thrown")
    } catch (e: IllegalArgumentException) {
      assertEquals("Cloud project number is required", e.message)
    }
  }

  @Test
  fun `attestDevice throws exception when token provider is null`() = runTest {
    DeviceAttestationHelper.integrityTokenProvider = null

    val result = DeviceAttestationHelper.attestDevice("client-id")

    assertTrue("Result should be failure", result is ClerkResult.Failure)
    val failure = result as ClerkResult.Failure
    assertNotNull("Throwable should not be null", failure.throwable)
    assertTrue("Error should be IllegalStateException", failure.throwable is IllegalStateException)
    assertTrue(
      "Error message should mention token provider",
      failure.throwable!!
        .message!!
        .contains("Integrity token provider must be prepared before attestation"),
    )
  }

  @Test
  fun `performAssertion calls device attestation API`() = runTest {
    val token = "test-token"
    val applicationId = "com.example.app"
    val mockClient = mockk<Client>()

    coEvery { mockDeviceAttestationApi.verify(any(), any()) } returns
      ClerkResult.success(mockClient)

    val result = DeviceAttestationHelper.performAssertion(token, applicationId)

    assertTrue("Result should be success", result is ClerkResult.Success)
    assertEquals(mockClient, (result as ClerkResult.Success).value)
  }

  @Test
  fun `performAssertion returns a failure when applicationId is null`() = runTest {
    val result = DeviceAttestationHelper.performAssertion("test-token", null)

    val failure = result as ClerkResult.Failure
    assertTrue(failure.throwable is IllegalArgumentException)
    assertEquals(
      "Application ID is required for device attestation",
      failure.throwable?.message,
    )
  }

  @Test
  fun `performAssertion rethrows cancellation instead of returning a failure`() = runTest {
    coEvery { mockDeviceAttestationApi.verify(any(), any()) } throws
      CancellationException("caller cancelled")

    val thrown = runCatching {
      DeviceAttestationHelper.performAssertion("test-token", "com.example")
    }
      .exceptionOrNull()

    assertTrue(thrown is CancellationException)
  }

  @Test
  fun `getHashedClientId generates correct SHA-256 hash`() {
    val clientId = "test-client-id"

    val result = DeviceAttestationHelper.getHashedClientId(clientId)

    assertNotNull(result)
    assertEquals(SHA256_HEX_LENGTH, result.length)
    assertTrue(result.matches(Regex("[0-9a-f]{$SHA256_HEX_LENGTH}")))
  }

  @Test
  fun `getHashedClientId handles different input values consistently`() {
    val inputs =
      listOf("", "a", "test-client-id", "another-longer-client-id-with-special-chars!@#$%")

    inputs.forEach { input ->
      val result1 = DeviceAttestationHelper.getHashedClientId(input)
      val result2 = DeviceAttestationHelper.getHashedClientId(input)

      assertEquals("Hash should be consistent for input: $input", result1, result2)
      assertEquals(
        "Hash should be $SHA256_HEX_LENGTH characters for input: $input",
        SHA256_HEX_LENGTH,
        result1.length,
      )
      assertTrue(
        "Hash should be valid hex for input: $input",
        result1.matches(Regex("[0-9a-f]{$SHA256_HEX_LENGTH}")),
      )
    }
  }

  @Test
  fun `getHashedClientId produces different hashes for different inputs`() {
    val input1 = "client-id-1"
    val input2 = "client-id-2"

    val hash1 = DeviceAttestationHelper.getHashedClientId(input1)
    val hash2 = DeviceAttestationHelper.getHashedClientId(input2)

    assertTrue("Different inputs should produce different hashes", hash1 != hash2)
  }

  @Test
  fun `getHashedClientId uses cache for performance`() {
    val clientId = "test-client-id"

    val result1 = DeviceAttestationHelper.getHashedClientId(clientId)
    val result2 = DeviceAttestationHelper.getHashedClientId(clientId)
    val result3 = DeviceAttestationHelper.getHashedClientId(clientId)

    assertEquals("Results should be identical from cache", result1, result2)
    assertEquals("Results should be identical from cache", result2, result3)
  }

  @Test
  fun `clearCache resets helper state`() {
    DeviceAttestationHelper.getHashedClientId("test")

    DeviceAttestationHelper.clearCache()

    val statsAfter = DeviceAttestationHelper.getCacheStats()
    assertEquals("Hash cache should be empty", 0, statsAfter.hashCacheSize)
    assertEquals("Prepared providers should be empty", 0, statsAfter.preparedProvidersCount)
    assertEquals(
      "Token provider should be null",
      null,
      DeviceAttestationHelper.integrityTokenProvider,
    )
  }

  @Test
  fun `scope is properly configured`() {
    assertNotNull(DeviceAttestationHelper.scope)
  }
}
