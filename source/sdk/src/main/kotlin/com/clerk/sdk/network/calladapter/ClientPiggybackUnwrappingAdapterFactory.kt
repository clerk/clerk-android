package com.clerk.sdk.network.calladapter

import com.clerk.sdk.Clerk
import com.clerk.sdk.log.ClerkLog
import com.clerk.sdk.model.error.ClerkErrorResponse
import com.clerk.sdk.model.error.Error
import com.clerk.sdk.model.response.ClerkResponse
import com.clerk.sdk.model.response.ClientPiggybackedResponse
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlinx.serialization.json.Json
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit

/** Call adapter factory that unwraps ClientPiggybackedResponse objects */
class ClientPiggybackUnwrappingAdapterFactory(private val json: Json) : CallAdapter.Factory() {
  @Suppress("ReturnCount")
  override fun get(
    returnType: Type,
    annotations: Array<out Annotation>,
    retrofit: Retrofit,
  ): CallAdapter<*, *>? {
    // We're only handling ClerkResponse types
    if (getRawType(returnType) != ClerkResponse::class.java) {
      return null
    }

    // Extract the inner type (e.g., ClientPiggybackedResponse<SignUp>)
    val responseType = getParameterUpperBound(0, returnType as ParameterizedType)

    // We're only handling ClientPiggybackedResponse types
    if (getRawType(responseType) != ClientPiggybackedResponse::class.java) {
      return null
    }

    return object : CallAdapter<Any, ClerkResponse<*>> {
      override fun responseType(): Type = responseType

      @Suppress("TooGenericExceptionCaught")
      override fun adapt(call: Call<Any>): ClerkResponse<*> {
        return try {
          // Execute the call synchronously
          val response = call.execute()

          if (response.isSuccessful) {
            handleSuccessResponse(response)
          } else {
            handleErrorResponse(response)
          }
        } catch (e: Exception) {
          ClerkLog.e("Network request failed: ${e.message}")
          createNetworkErrorResponse(e)
        }
      }

      // Function to handle successful responses
      private fun handleSuccessResponse(response: retrofit2.Response<Any>): ClerkResponse<*> {
        val body = response.body()

        return if (body is ClientPiggybackedResponse<*>) {
          // Extract and set client if present
          body.client?.let { client -> Clerk.client = client }

          // Return unwrapped response
          ClerkResponse.Success(body.response)
        } else {
          // Handle unexpected response type
          ClerkResponse.Success(body)
        }
      }

      // Function to handle error responses
      private fun handleErrorResponse(response: retrofit2.Response<Any>): ClerkResponse.Error {
        val errorBody = response.errorBody()?.string()

        return if (errorBody != null) {
          parseErrorBody(errorBody, response)
        } else {
          createGenericErrorResponse(
            "Unknown error",
            "HTTP ${response.code()}: ${response.message()}",
            "unknown_error",
            response,
          )
        }
      }

      @Suppress("TooGenericExceptionCaught")
      private fun parseErrorBody(
        errorBody: String,
        response: retrofit2.Response<Any>,
      ): ClerkResponse.Error {
        return try {
          val errorResponse = json.decodeFromString<ClerkErrorResponse>(errorBody)

          // Extract and set client from error response if present
          errorResponse.meta.client?.let { client -> Clerk.client = client }

          ClerkResponse.Error(errorResponse)
        } catch (e: Exception) {
          ClerkLog.e("Error parsing error response: ${e.message}")
          createGenericErrorResponse(
            "Failed to parse error response",
            "HTTP ${response.code()}: ${response.message()}",
            "parse_error",
            response,
          )
        }
      }

      // Function to create a generic error response
      private fun createGenericErrorResponse(
        message: String,
        longMessage: String,
        code: String,
        response: retrofit2.Response<Any>? = null,
      ): ClerkResponse.Error {
        val clerkTraceId = response?.headers()?.get("clerk-trace-id") ?: ""

        val genericError =
          ClerkErrorResponse(
            errors = listOf(Error(message = message, longMessage = longMessage, code = code)),
            meta = com.clerk.sdk.model.error.Meta(),
            clerkTraceId = clerkTraceId,
          )

        return ClerkResponse.Error(genericError)
      }

      // Function to create a network error response
      private fun createNetworkErrorResponse(exception: Exception): ClerkResponse.Error {
        return createGenericErrorResponse(
          "Network request failed",
          exception.message ?: "Unknown error",
          "network_error",
        )
      }
    }
  }
}
