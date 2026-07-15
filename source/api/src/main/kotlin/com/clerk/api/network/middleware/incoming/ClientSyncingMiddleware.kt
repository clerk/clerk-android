package com.clerk.api.network.middleware.incoming

import com.clerk.api.Clerk
import com.clerk.api.auth.AuthEvent
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ApiPaths
import com.clerk.api.network.model.client.Client
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.Interceptor
import okhttp3.Request
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
    val request = chain.request()
    val response = chain.proceed(request)

    // Only process JSON responses
    val body = response.body
    if (response.isSuccessful && body.contentType()?.subtype == "json") {
      val responseBody = body.string()
      responseBody.let {
        try {
          // Parse the response to extract client if present
          val jsonElement = json.parseToJsonElement(it)
          val authEvents =
            if (jsonElement is JsonObject) {
              decodeAuthEvents(request = request, jsonObject = jsonElement)
            } else {
              emptyList()
            }
          val completedAuthFlow =
            authEvents.firstOrNull { event ->
              event is AuthEvent.SignInCompleted || event is AuthEvent.SignUpCompleted
            }
          if (jsonElement is JsonObject) {
            syncClient(jsonObject = jsonElement, completedAuthFlow = completedAuthFlow)
          }

          authEvents.forEach(Clerk.auth::send)

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

  private fun syncClient(jsonObject: JsonObject, completedAuthFlow: AuthEvent?) {
    if (!jsonObject.containsKey("client")) return

    when (val clientJson = jsonObject["client"]) {
      is JsonNull -> {
        if (jsonObject.containsKey("response")) {
          ClerkLog.d("Client sync skipped null piggyback client")
        } else {
          ClerkLog.d("Client sync cleared by explicit null client")
          Clerk.updateClient(Client())
        }
      }
      null -> Unit
      else -> {
        val client = json.decodeFromJsonElement<Client>(clientJson)
        ClerkLog.d("Client synced: ${client.id}")
        Clerk.updateClient(client, completedAuthFlow = completedAuthFlow)
      }
    }
  }

  private fun decodeAuthEvents(request: Request, jsonObject: JsonObject): List<AuthEvent> {
    val responseElement = jsonObject["response"]
    if (responseElement == null) {
      return emptyList()
    }
    val requestPath = request.url.encodedPath
    val responseObject =
      ((responseElement as? JsonObject)?.get("object") as? JsonPrimitive)?.contentOrNull

    return when {
      responseObject == "sign_in_attempt" ||
        requestPath.contains("/${ApiPaths.Client.SignIn.BASE}") ->
        decodeSignInEvents(
          requestPath = requestPath,
          method = request.method,
          responseElement = responseElement,
        )
      responseObject == "sign_up_attempt" ||
        requestPath.contains("/${ApiPaths.Client.SignUp.BASE}") ->
        decodeSignUpEvents(
          requestPath = requestPath,
          method = request.method,
          responseElement = responseElement,
        )
      else -> emptyList()
    }
  }

  private fun decodeSignInEvents(
    requestPath: String,
    method: String,
    responseElement: JsonElement,
  ): List<AuthEvent> {
    val signIn = decodeSignIn(responseElement) ?: return emptyList()

    return if (
      isSignInCreationRequest(path = requestPath, method = method) &&
        signIn.status != SignIn.Status.COMPLETE
    ) {
      listOf(AuthEvent.SignInStarted(signIn))
    } else if (signIn.status == SignIn.Status.COMPLETE) {
      listOf(AuthEvent.SignInCompleted(signIn))
    } else {
      emptyList()
    }
  }

  private fun decodeSignUpEvents(
    requestPath: String,
    method: String,
    responseElement: JsonElement,
  ): List<AuthEvent> {
    val signUp = decodeSignUp(responseElement) ?: return emptyList()

    return if (
      isSignUpCreationRequest(path = requestPath, method = method) &&
        signUp.status != SignUp.Status.COMPLETE
    ) {
      listOf(AuthEvent.SignUpStarted(signUp))
    } else if (signUp.status == SignUp.Status.COMPLETE) {
      listOf(AuthEvent.SignUpCompleted(signUp))
    } else {
      emptyList()
    }
  }

  private fun decodeSignIn(responseElement: JsonElement): SignIn? {
    return runCatching { json.decodeFromJsonElement<SignIn>(responseElement) }.getOrNull()
  }

  private fun decodeSignUp(responseElement: JsonElement): SignUp? {
    return runCatching { json.decodeFromJsonElement<SignUp>(responseElement) }.getOrNull()
  }

  private fun isSignInCreationRequest(path: String, method: String): Boolean {
    return method == "POST" && path.endsWith("/${ApiPaths.Client.SignIn.BASE}")
  }

  private fun isSignUpCreationRequest(path: String, method: String): Boolean {
    return method == "POST" && path.endsWith("/${ApiPaths.Client.SignUp.BASE}")
  }
}
