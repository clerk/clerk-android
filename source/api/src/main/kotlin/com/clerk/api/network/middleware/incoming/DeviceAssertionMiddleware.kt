package com.clerk.api.network.middleware.incoming

import com.clerk.api.Clerk
import com.clerk.api.attestation.DeviceAttestationHelper
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error as ClerkError
import com.clerk.api.network.serialization.ClerkResult
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * OkHttp interceptor that handles device assertion when API calls fail with "requires_assertion"
 * error. This interceptor will automatically perform device attestation and retry the original
 * request.
 */
internal class DeviceAssertionInterceptor : Interceptor {

  override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
    val originalRequest = chain.request()
    val response = chain.proceed(originalRequest)

    // Only handle error responses
    if (response.isSuccessful) {
      return@runBlocking response
    }

    // Parse error response to check if it requires assertion
    val originalBody = response.body

    // Read the response body content and create a copy for later use
    val bodyString = originalBody.string()
    if (bodyString.isBlank()) {
      return@runBlocking response
    }

    // Create a new response with a fresh response body for downstream processing
    val newResponseBody = bodyString.toResponseBody(originalBody.contentType())
    val newResponse = response.newBuilder().body(newResponseBody).build()

    val clerkError =
      try {
        ClerkApi.json.decodeFromString<ClerkErrorResponse>(bodyString)
      } catch (e: Exception) {
        ClerkLog.e("Failed to parse error response: $e")
        return@runBlocking newResponse
      }

    // Check if any error in the response requires device assertion
    val requiresAssertion = clerkError.errors.any { it.code == "requires_assertion" }
    if (!requiresAssertion) {
      return@runBlocking newResponse
    }

    // Get the specific assertion error
    val assertionError = clerkError.errors.first { it.code == "requires_assertion" }

    val shouldRetry =
      try {
        DeviceAttestationManager.performDeviceAssertion(originalRequest, assertionError)
      } catch (e: Exception) {
        ClerkLog.e("Device assertion failed: $e")
        false
      }

    if (shouldRetry) {
      ClerkLog.d("Retrying request after successful device assertion")
      // Close the original response and retry
      response.close()
      chain.proceed(originalRequest)
    } else {
      newResponse
    }
  }

  /**
   * Thread-safe manager for handling device assertion operations. Uses atomic reference to manage
   * in-flight tasks.
   */
  private object DeviceAttestationManager {
    private val inFlightTask = AtomicReference<Deferred<Boolean>?>(null)

    suspend fun performDeviceAssertion(request: Request, error: ClerkError): Boolean {
      if (error.code != "requires_assertion") {
        return false
      }

      // Check if there's already an in-flight task
      val existingTask = inFlightTask.get()
      if (existingTask != null) {
        return try {
          existingTask.await()
        } catch (e: Exception) {
          ClerkLog.e("In-flight assertion task failed: $e")
          false
        }
      }

      // Create new task
      val newTask =
        DeviceAttestationHelper.scope.async {
          try {
            when (error.code) {
              "requires_assertion" -> {
                try {
                  handleRequiresAssertionError()
                } catch (e: ClerkAPIError) {
                  if (e.code == "requires_device_attestation") {
                    handleRequiresDeviceAttestationError(request)
                  } else {
                    throw e
                  }
                }
              }
              else -> false
            }
          } finally {
            // Clear the in-flight task when done
            inFlightTask.set(null)
          }
        }

      // Set the new task atomically
      if (inFlightTask.compareAndSet(null, newTask)) {
        return try {
          newTask.await()
        } catch (e: Exception) {
          ClerkLog.e("Device assertion failed: $e")
          false
        }
      } else {
        // Another task was set concurrently, cancel ours and wait for the other
        newTask.cancel()
        val concurrentTask = inFlightTask.get()
        return concurrentTask?.await() ?: false
      }
    }

    /** Handles the "requires_assertion" error by performing device attestation. */
    private suspend fun handleRequiresAssertionError(): Boolean {
      return performAssertion()
    }

    /**
     * Handles the "requires_device_attestation" error by performing full device attestation
     * followed by assertion.
     */
    private suspend fun handleRequiresDeviceAttestationError(request: Request): Boolean {
      // Perform device attestation first
      performDeviceAttestation()

      // Then perform assertion
      performAssertion()

      // Check if the original request was a client/verify endpoint
      val url = request.url.toString()
      if (url.endsWith("client/verify")) {
        // Don't retry client/verify requests as the assertion above already verified
        return false
      }

      return true
    }

    /** Performs device assertion using the DeviceAttestationHelper. */
    private suspend fun performAssertion(): Boolean {
      val clientId = getCurrentClientId()
      val applicationId = getApplicationId()

      // Get integrity token
      val tokenResult = DeviceAttestationHelper.attestDevice(clientId)
      val token =
        when (tokenResult) {
          is ClerkResult.Success -> tokenResult.value
          is ClerkResult.Failure -> {
            throw ClerkAPIError("device_attestation_failed", "Failed to get integrity token")
          }
        }

      // Perform assertion
      val assertionResult = DeviceAttestationHelper.performAssertion(token, applicationId)
      return when (assertionResult) {
        is ClerkResult.Success -> {
          ClerkLog.d("Device assertion completed successfully")
          true
        }
        is ClerkResult.Failure -> {
          throw ClerkAPIError("assertion_failed", "Assertion verification failed")
        }
      }
    }

    /** Performs device attestation (prepare the device for attestation). */
    private suspend fun performDeviceAttestation(): Boolean {
      // This would typically involve preparing the device attestation
      // For now, we assume the DeviceAttestationHelper.prepareIntegrityTokenProvider
      // has already been called during app initialization
      ClerkLog.d("Device attestation preparation completed")
      return true
    }

    /** Gets the current client ID from your authentication state. */
    private fun getCurrentClientId(): String {
      return requireNotNull(Clerk.client.id)
    }

    /** Gets the application ID (package name). */
    private fun getApplicationId(): String {
      return requireNotNull(Clerk.applicationId)
    }
  }
}

/** Custom exception class for Clerk API errors used in device assertion. */
class ClerkAPIError(val code: String, message: String) : Exception(message)
