package com.clerk.api.network.middleware.incoming

import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.model.client.Client
import com.clerk.api.session.Session
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Network middleware that automatically syncs the Clerk client state from API responses.
 *
 * This middleware intercepts successful JSON responses and checks for a "client" field in the
 * response body. If found, it deserializes the client data and updates the global [Clerk.client]
 * state.
 *
 * @property json The JSON serializer used for deserializing the client data.
 */
internal class ClientSyncingMiddleware(private val json: Json) : Interceptor {
  /**
   * Intercepts network responses to sync client state.
   *
   * @param chain The interceptor chain.
   * @return The original response, potentially with a new body if it was read for client syncing.
   */
  override fun intercept(chain: Interceptor.Chain): Response {
    val response = chain.proceed(chain.request())

    // Only process JSON responses
    val body = response.body
    if (response.isSuccessful && body != null && body.contentType()?.subtype == "json") {
      val responseBody = body.string()
      responseBody.let {
        try {
          // Parse the response to extract client if present
          val jsonElement = json.parseToJsonElement(it)
          if (jsonElement is JsonObject && jsonElement.containsKey("client")) {
            val clientJson = jsonElement["client"]
            if (clientJson != null && clientJson !is JsonNull) {
              // Extract and set the client
              val client = json.decodeFromJsonElement<Client>(clientJson)
              ClerkLog.d("Client synced: ${client.id}")
              logPendingSessionWarningIfNeeded(client)
              Clerk.updateClient(client)
            }
          }

          // Return the original response with its body
          val newBody = it.toResponseBody(body.contentType())
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

  /**
   * Logs a warning if a sign-in or sign-up is complete but the created session is pending.
   *
   * This can happen when additional verification or approval is required before the session becomes
   * active.
   *
   * @param client The client containing the sign-in/sign-up and session data.
   */
  private fun logPendingSessionWarningIfNeeded(client: Client) {
    if (isSignInCompleteWithPendingSession(client)) {
      ClerkLog.w(
        "Sign-in completed but the session is pending. " +
          "The user may need to complete additional verification before gaining access."
      )
    }

    if (isSignUpCompleteWithPendingSession(client)) {
      ClerkLog.w(
        "Sign-up completed but the session is pending. " +
          "The user may need to complete additional verification before gaining access."
      )
    }
  }

  private fun isSignInCompleteWithPendingSession(client: Client): Boolean {
    val signIn = client.signIn ?: return false
    val sessionId = signIn.createdSessionId
    val session = sessionId?.let { id -> client.sessions.find { it.id == id } }
    return signIn.status == SignIn.Status.COMPLETE && session?.status == Session.SessionStatus.PENDING
  }

  private fun isSignUpCompleteWithPendingSession(client: Client): Boolean {
    val signUp = client.signUp ?: return false
    val sessionId = signUp.createdSessionId
    val session = sessionId?.let { id -> client.sessions.find { it.id == id } }
    return signUp.status == SignUp.Status.COMPLETE && session?.status == Session.SessionStatus.PENDING
  }
}
