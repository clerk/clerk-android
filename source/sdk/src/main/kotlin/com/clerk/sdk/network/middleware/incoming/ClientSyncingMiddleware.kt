package com.clerk.sdk.network.middleware.incoming

import com.clerk.sdk.Clerk
import com.clerk.sdk.log.ClerkLog
import com.clerk.sdk.model.client.Client
import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class ClientSyncingMiddleware(private val json: Json) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val response = chain.proceed(chain.request())

    // Only process JSON responses
    if (response.isSuccessful && response.body?.contentType()?.subtype == "json") {
      val responseBody = response.body?.string()
      responseBody?.let {
        try {
          // Parse the response to extract client if present
          val jsonElement = json.parseToJsonElement(it)
          if (jsonElement is JsonObject && jsonElement.containsKey("client")) {
            val clientJson = jsonElement["client"]
            if (clientJson != null && clientJson !is JsonNull) {
              // Extract and set the client
              val client = json.decodeFromJsonElement<Client>(clientJson)
              Clerk.client = client
            }
          }

          // Return the original response with its body
          val newBody = it.toResponseBody(response.body?.contentType())
          return response.newBuilder().body(newBody).build()
        } catch (e: SerializationException) {
          ClerkLog.e("Error deserializing client: ${e.message}")
        } catch (e: IOException) {
          ClerkLog.e("IO error while processing response: ${e.message}")
        } catch (e: IllegalArgumentException) {
          ClerkLog.e("Error parsing JSON: ${e.message}")
        }
      }
    }

    return response
  }
}
