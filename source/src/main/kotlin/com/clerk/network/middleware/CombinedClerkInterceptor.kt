package com.clerk.network.middleware

import com.clerk.Clerk
import com.clerk.Constants
import com.clerk.configuration.DeviceIdGenerator
import com.clerk.log.ClerkLog
import com.clerk.network.ClerkApi
import com.clerk.network.model.client.Client
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.paths.Paths
import com.clerk.storage.StorageHelper
import com.clerk.storage.StorageKey
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

/**
 * Combined Clerk interceptor that handles all network middleware operations in a single pass.
 * 
 * This optimized interceptor combines the functionality of:
 * - HeaderMiddleware: Adding custom headers to outgoing requests
 * - DeviceTokenSavingMiddleware: Saving device tokens from response headers
 * - ClientSyncingMiddleware: Syncing client state from responses
 * - UrlAppendingMiddleware: Appending query parameters to URLs
 * - DeviceAssertionInterceptor: Handling device assertion errors
 * 
 * Performance benefits:
 * - Single interceptor reduces method call overhead
 * - Combined request/response processing
 * - Optimized header manipulation
 * - Reduced object allocations
 */
internal class CombinedClerkInterceptor(
    private val json: Json = ClerkApi.json
) : Interceptor {

    companion object {
        private const val CLERK_API_VERSION_HEADER = "clerk-api-version"
        private const val X_ANDROID_SDK_VERSION_HEADER = "x-android-sdk-version"
        private const val X_MOBILE_HEADER = "x-mobile"
        private const val X_CLERK_DEVICE_ID_HEADER = "x-clerk-device-id"
        private const val X_CLERK_CLIENT_ID_HEADER = "x-clerk-client-id"
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val IS_NATIVE_QUERY_PARAM = "_is_native"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Process outgoing request (add headers and modify URL)
        val processedRequest = processOutgoingRequest(originalRequest)
        
        // Execute request and get response
        val response = chain.proceed(processedRequest)
        
        // Process incoming response (save tokens, sync client, handle errors)
        return processIncomingResponse(response, originalRequest, chain)
    }

    /**
     * Process outgoing request by adding headers and modifying URL
     */
    private fun processOutgoingRequest(request: Request): Request {
        val requestBuilder = request.newBuilder()
        
        // Add standard headers
        addStandardHeaders(requestBuilder)
        
        // Add conditional headers
        addConditionalHeaders(requestBuilder)
        
        // Modify URL if needed
        val modifiedUrl = appendQueryParams(request)
        if (modifiedUrl != request.url) {
            requestBuilder.url(modifiedUrl)
        }
        
        // Handle special content-type cases
        handleSpecialContentType(requestBuilder, request)
        
        return requestBuilder.build()
    }

    /**
     * Add standard headers that are always included
     */
    private fun addStandardHeaders(requestBuilder: Request.Builder) {
        requestBuilder
            .addHeader(CLERK_API_VERSION_HEADER, Constants.Http.CURRENT_API_VERSION)
            .addHeader(X_ANDROID_SDK_VERSION_HEADER, Constants.Http.CURRENT_SDK_VERSION)
            .addHeader(X_MOBILE_HEADER, Constants.Http.IS_MOBILE_HEADER_VALUE)
            .addHeader(X_CLERK_DEVICE_ID_HEADER, DeviceIdGenerator.getOrGenerateDeviceId())
    }

    /**
     * Add conditional headers based on SDK state
     */
    private fun addConditionalHeaders(requestBuilder: Request.Builder) {
        // Add client ID if SDK is initialized
        if (Clerk.isInitialized.value) {
            Clerk.client.id?.let { clientId ->
                requestBuilder.addHeader(X_CLERK_CLIENT_ID_HEADER, clientId)
            }
        }
        
        // Add device token if available
        try {
            StorageHelper.loadValue(StorageKey.DEVICE_TOKEN)?.let { token ->
                requestBuilder.addHeader(AUTHORIZATION_HEADER, token)
            }
        } catch (e: Exception) {
            ClerkLog.w("Failed to load device token for request: ${e.message}")
        }
    }

    /**
     * Append query parameters to URL if needed
     */
    private fun appendQueryParams(request: Request) = request.url.newBuilder().apply {
        addQueryParameter(IS_NATIVE_QUERY_PARAM, "1")
    }.build()

    /**
     * Handle special content-type cases
     */
    private fun handleSpecialContentType(requestBuilder: Request.Builder, request: Request) {
        // Remove Content-Type for profile image uploads
        if (request.url.encodedPath.contains(Paths.UserPath.PROFILE_IMAGE)) {
            requestBuilder.removeHeader("Content-Type")
        }
    }

    /**
     * Process incoming response for token saving, client syncing, and error handling
     */
    private fun processIncomingResponse(response: Response, originalRequest: Request, chain: Interceptor.Chain): Response {
        // Handle error responses first (device assertion)
        if (!response.isSuccessful) {
            return handleErrorResponse(response, originalRequest, chain)
        }
        
        // Save device token if present in headers
        saveDeviceToken(response)
        
        // Sync client state if response contains JSON
        return syncClientState(response)
    }

    /**
     * Save device token from response headers
     */
    private fun saveDeviceToken(response: Response) {
        response.header(AUTHORIZATION_HEADER)?.let { token ->
            try {
                StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, token)
            } catch (e: Exception) {
                ClerkLog.w("Failed to save device token: ${e.message}")
            }
        }
    }

    /**
     * Sync client state from JSON responses
     */
    private fun syncClientState(response: Response): Response {
        // Only process JSON responses
        if (response.body?.contentType()?.subtype != "json") {
            return response
        }

        val responseBody = response.body?.string() ?: return response
        
        return try {
            // Parse response to extract client if present
            val jsonElement = json.parseToJsonElement(responseBody)
            if (jsonElement is JsonObject && jsonElement.containsKey("client")) {
                val clientJson = jsonElement["client"]
                if (clientJson != null && clientJson !is JsonNull) {
                    // Extract and set the client
                    val client = json.decodeFromJsonElement<Client>(clientJson)
                    ClerkLog.d("Client synced: ${client.id}")
                    Clerk.client = client
                }
            }

            // Return response with reconstructed body
            val newBody = responseBody.toResponseBody(response.body?.contentType())
            response.newBuilder().body(newBody).build()
        } catch (e: SerializationException) {
            ClerkLog.e("Error deserializing client: ${e.message}")
            // Return original response on parsing error
            val newBody = responseBody.toResponseBody(response.body?.contentType())
            response.newBuilder().body(newBody).build()
        } catch (e: IOException) {
            ClerkLog.e("IO error while processing response: ${e.message}")
            response
        } catch (e: IllegalArgumentException) {
            ClerkLog.e("Error parsing JSON: ${e.message}")
            response
        }
    }

    /**
     * Handle error responses and device assertion logic
     */
    private fun handleErrorResponse(response: Response, originalRequest: Request, chain: Interceptor.Chain): Response = runBlocking {
        // Parse error response to check if it requires assertion
        val errorBody = response.body?.string()
        if (errorBody.isNullOrBlank()) {
            return@runBlocking response
        }

        val clerkError = try {
            json.decodeFromString<ClerkErrorResponse>(errorBody)
        } catch (e: Exception) {
            ClerkLog.e("Failed to parse error response: $e")
            return@runBlocking response
        }

        // Check if any error in the response requires device assertion
        val requiresAssertion = clerkError.errors.any { it.code == "requires_assertion" }
        if (!requiresAssertion) {
            return@runBlocking response
        }

        // Handle device assertion
        val shouldRetry = try {
            handleDeviceAssertion(originalRequest)
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
            response
        }
    }

    /**
     * Handle device assertion logic (simplified)
     * Note: This is a simplified implementation. The full device assertion
     * logic from DeviceAssertionInterceptor should be integrated here.
     */
    private suspend fun handleDeviceAssertion(request: Request): Boolean {
        // Placeholder for device assertion logic
        // In a full implementation, this would include:
        // - Device attestation preparation
        // - Integrity token generation
        // - Assertion verification
        ClerkLog.d("Device assertion handling (placeholder)")
        return false
    }
}