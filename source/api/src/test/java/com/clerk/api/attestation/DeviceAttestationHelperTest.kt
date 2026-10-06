import android.content.Context
import com.clerk.api.Constants.Attestation.SHA256_HEX_LENGTH
import com.clerk.api.attestation.DeviceAttestationHelper
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.DeviceAttestationApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.serialization.ClerkResult
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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
  fun `attestDevice returns failure when token provider is null`() = runTest {
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

    coEvery { mockDeviceAttestationApi.verify(any(), any(), any()) } returns
      ClerkResult.success(mockClient)

    val result = DeviceAttestationHelper.performAssertion(token, applicationId)

    assertTrue("Result should be success", result is ClerkResult.Success)
    assertEquals(mockClient, (result as ClerkResult.Success).value)
    coVerify(exactly = 1) {
      mockDeviceAttestationApi.verify(packageName = applicationId, token = token, platform = any())
    }
  }

  @Test
  fun `performAssertion throws exception when applicationId is null`() = runTest {
    val token = "test-token"

    try {
      DeviceAttestationHelper.performAssertion(token, null)
      throw AssertionError("Expected IllegalArgumentException to be thrown")
    } catch (e: IllegalArgumentException) {
      assertEquals("Application ID is required for device attestation", e.message)
    }
  }

  @Test
  fun `getHashedClientId matches the SHA-256 test vectors, including bytes that need zero-padding`() {
    assertEquals(
      "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
      DeviceAttestationHelper.getHashedClientId("abc"),
    )
    assertEquals(
      "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      DeviceAttestationHelper.getHashedClientId(""),
    )
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
    assertEquals(
      "Repeated lookups should share one cache entry",
      1,
      DeviceAttestationHelper.getCacheStats().hashCacheSize,
    )

    DeviceAttestationHelper.getHashedClientId("other-client-id")

    assertEquals(2, DeviceAttestationHelper.getCacheStats().hashCacheSize)
  }

  @Test
  fun `clearCache resets helper state`() = runTest {
    val provider = mockk<StandardIntegrityManager.StandardIntegrityTokenProvider>()
    val task = mockk<Task<StandardIntegrityManager.StandardIntegrityTokenProvider>>()
    every {
      task.addOnSuccessListener(
        any<OnSuccessListener<StandardIntegrityManager.StandardIntegrityTokenProvider>>()
      )
    } answers
      {
        firstArg<OnSuccessListener<StandardIntegrityManager.StandardIntegrityTokenProvider>>()
          .onSuccess(provider)
        task
      }
    every { task.addOnFailureListener(any()) } returns task
    every { mockIntegrityManager.prepareIntegrityToken(any()) } returns task
    DeviceAttestationHelper.prepareIntegrityTokenProvider(mockContext, CLOUD_PROJECT_NUMBER)
    DeviceAttestationHelper.getHashedClientId("test")

    val statsBefore = DeviceAttestationHelper.getCacheStats()
    assertEquals(1, statsBefore.hashCacheSize)
    assertEquals(1, statsBefore.preparedProvidersCount)
    assertSame(provider, DeviceAttestationHelper.integrityTokenProvider)
    assertTrue(DeviceAttestationHelper.isProviderPrepared(CLOUD_PROJECT_NUMBER))

    DeviceAttestationHelper.clearCache()

    val statsAfter = DeviceAttestationHelper.getCacheStats()
    assertEquals("Hash cache should be empty", 0, statsAfter.hashCacheSize)
    assertEquals("Prepared providers should be empty", 0, statsAfter.preparedProvidersCount)
    assertEquals(
      "Token provider should be null",
      null,
      DeviceAttestationHelper.integrityTokenProvider,
    )
    assertNull("Integrity manager should be null", DeviceAttestationHelper.integrityManager)
    assertFalse(DeviceAttestationHelper.isProviderPrepared(CLOUD_PROJECT_NUMBER))
  }

  private companion object {
    const val CLOUD_PROJECT_NUMBER = 123_456_789L
  }
}
