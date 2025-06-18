package com.clerk.attestation

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.clerk.log.ClerkLog
import com.clerk.network.ClerkApi
import com.clerk.network.model.client.Client
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.serialization.ClerkResult
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.suspendCancellableCoroutine

private const val HASH_CONSTANT = 0xff

/**
 * Helper object for handling device attestation using Google Play Integrity API.
 *
 * This object manages the integrity token provider preparation, token retrieval, and device
 * attestation verification with Clerk's backend services.
 */
internal object DeviceAttestationHelper {
  /** Coroutine scope for handling asynchronous operations on the IO dispatcher. */
  val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  /** Standard integrity manager instance for interacting with Google Play Integrity API. */
  var integrityManager: StandardIntegrityManager? = null

  /** Token provider for generating integrity tokens. */
  var integrityTokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider? = null

  /**
   * Prepares the integrity token provider for the given cloud project.
   *
   * @param context The Android application context
   * @param cloudProjectNumber The Google Cloud project number associated with the app
   * @throws IllegalArgumentException if cloudProjectNumber is null
   */
  fun prepareIntegrityTokenProvider(context: Context, cloudProjectNumber: Long?) {
    requireNotNull(cloudProjectNumber) { "Cloud project number is required" }
    if (integrityManager == null) {
      integrityManager = IntegrityManagerFactory.createStandard(context)
    }

    requireNotNull(integrityManager) { "IntegrityManager is not initialized" }
      .prepareIntegrityToken(
        StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
          .setCloudProjectNumber(cloudProjectNumber)
          .build()
      )
      .addOnSuccessListener { tokenProvider ->
        ClerkLog.d("Integrity token provider prepared successfully")
        integrityTokenProvider = tokenProvider
      }
      .addOnFailureListener { ClerkLog.e("Failed to prepare integrity token: $it") }
  }

  /**
   * Retrieves an integrity token and initiates device attestation verification. This function
   * suspends until the integrity verification completes.
   *
   * @param clientId The client identifier to be hashed and included in the token request
   * @return The integrity token string
   * @throws IllegalArgumentException if integrityTokenProvider is null
   */
  @Throws(IllegalArgumentException::class)
  suspend fun attestDevice(clientId: String): ClerkResult<String, ClerkErrorResponse> {
    val tokenProvider =
      requireNotNull(integrityTokenProvider) { "Integrity token provider must not be null" }

    return suspendCancellableCoroutine { continuation ->
      val response =
        tokenProvider.request(
          StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
            .setRequestHash(getHashedClientId(clientId))
            .build()
        )

      response
        .addOnSuccessListener { tokenResponse ->
          ClerkLog.d("Integrity token retrieved successfully")
          continuation.resume(ClerkResult.success(tokenResponse.token()))
        }
        .addOnFailureListener { exception ->
          ClerkLog.e("Failed to get integrity token: $exception")
          continuation.resume(
            ClerkResult.unknownFailure(error("Failed to get integrity token: $exception"))
          )
        }
    }
  }

  /**
   * Attests the device by verifying the integrity token with Clerk's backend.
   *
   * @param token The integrity token obtained from Google Play Integrity API
   * @param applicationId The application package name for verification
   * @return true if attestation was successful, false otherwise
   */
  suspend fun performAssertion(
    token: String,
    applicationId: String?,
  ): ClerkResult<Client, ClerkErrorResponse> {
    return ClerkApi.deviceAttestationApi.verify(packageName = applicationId!!, token = token)
  }

  /**
   * Generates a SHA-256 hash of the provided client ID.
   *
   * @param clientId The client identifier to hash
   * @return The hexadecimal string representation of the SHA-256 hash
   * @throws RuntimeException if hashing fails
   */
  @VisibleForTesting
  fun getHashedClientId(clientId: String): String {

    try {
      val digest = MessageDigest.getInstance("SHA-256")
      val hash = digest.digest(clientId.toByteArray(charset("UTF-8")))
      val hexString = StringBuilder()

      for (b in hash) {
        val hex = Integer.toHexString(HASH_CONSTANT and b.toInt())
        if (hex.length == 1) hexString.append('0')
        hexString.append(hex)
      }

      return hexString.toString()
    } catch (e: Exception) {
      throw RuntimeException("Failed to hash clientId", e)
    }
  }
}
