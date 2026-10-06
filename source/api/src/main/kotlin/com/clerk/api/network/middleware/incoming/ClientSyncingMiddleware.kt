package com.clerk.api.network.middleware.incoming

import com.clerk.api.Clerk
import com.clerk.api.Constants.Http.AUTHORIZATION_HEADER
import com.clerk.api.auth.AuthEvent
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ApiPaths
import com.clerk.api.network.middleware.ManualClientSyncRequest
import com.clerk.api.network.middleware.ResponseGuard
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.clerk.api.state.ClientStateStore
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
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

private const val SERVER_DATE_HEADER = "Date"
private const val SERVER_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz"

internal class ClientSyncingMiddleware(private val json: Json) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val response = chain.proceed(request)
    val orderAtArrival = Clerk.stateStore.observeResponse(response.serverDateMillis())
    val manualClientSyncRequest = request.tag(ManualClientSyncRequest::class.java)
    manualClientSyncRequest?.recordResponse(
      requestDeviceToken = response.request.header(AUTHORIZATION_HEADER),
      responseDeviceToken = response.header(AUTHORIZATION_HEADER),
    )

    return when {
      !Clerk.isClientResponseCurrent(
        requestDeviceToken = response.request.header(AUTHORIZATION_HEADER),
        responseDeviceToken = response.header(AUTHORIZATION_HEADER),
      ) -> {
        ClerkLog.d("Client sync skipped for a response using a stale shared device token")
        response
      }
      manualClientSyncRequest != null -> response.withResponseOrder(orderAtArrival)
      else ->
        syncResponse(request = request, response = response, order = orderAtArrival)
          .withResponseOrder(orderAtArrival)
    }
  }

  @Suppress("NestedBlockDepth")
  private fun syncResponse(
    request: Request,
    response: Response,
    order: ClientStateStore.ResponseOrder?,
  ): Response {
    val body = response.body
    if (response.isSuccessful && body.contentType()?.subtype == "json") {
      val responseBody = body.string()
      responseBody.let {
        try {
          val jsonElement = json.parseToJsonElement(it)
          val authEvents =
            if (jsonElement is JsonObject) {
              decodeAuthEvents(request = request, jsonObject = jsonElement)
            } else {
              emptyList()
            }
          val completedAuthFlow = authEvents.firstOrNull { event ->
            event is AuthEvent.SignInCompleted || event is AuthEvent.SignUpCompleted
          }
          syncClientFromResponse(
            request = request,
            response = response,
            jsonElement = jsonElement,
            order = order,
            completedAuthFlow = completedAuthFlow,
          )

          authEvents.forEach(Clerk.auth::send)
        } catch (e: SerializationException) {
          ClerkLog.e("Error deserializing client: ${e.message}")
        } catch (e: IOException) {
          ClerkLog.e("IO error while processing response: ${e.message}")
        } catch (e: IllegalArgumentException) {
          ClerkLog.e("Error parsing JSON: ${e.message}")
        }

        val newBody = it.toResponseBody(body.contentType())
        return response.newBuilder().body(newBody).build()
      }
    }

    return response
  }

  private fun syncClientFromResponse(
    request: Request,
    response: Response,
    jsonElement: JsonElement,
    order: ClientStateStore.ResponseOrder?,
    completedAuthFlow: AuthEvent?,
  ) {
    if (jsonElement !is JsonObject) return

    if (jsonElement.containsKey("client")) {
      val clientJson = jsonElement["client"]
      when (clientJson) {
        is JsonNull -> {
          if (jsonElement.containsKey("response")) {
            ClerkLog.d("Client sync skipped null piggyback client")
          } else {
            ClerkLog.d("Client sync cleared by explicit null client")
            request.syncClient(response) { Clerk.applyClientResponse(Client(), order) }
          }
        }
        null -> Unit
        else ->
          request.syncClient(response) {
            syncClerkClient(
              client = json.decodeFromJsonElement(clientJson),
              order = order,
              completedAuthFlow = completedAuthFlow,
            )
          }
      }
      return
    }

    if (request.isClientFetch()) {
      request.syncClient(response) {
        syncClerkClient(
          client = json.decodeFromJsonElement(jsonElement),
          order = order,
          completedAuthFlow = completedAuthFlow,
        )
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
    return try {
      json.decodeFromJsonElement<SignIn>(responseElement)
    } catch (_: Exception) {
      null
    }
  }

  private fun decodeSignUp(responseElement: JsonElement): SignUp? {
    return try {
      json.decodeFromJsonElement<SignUp>(responseElement)
    } catch (_: Exception) {
      null
    }
  }

  private fun isSignInCreationRequest(path: String, method: String): Boolean {
    return method == "POST" && path.endsWith("/${ApiPaths.Client.SignIn.BASE}")
  }

  private fun isSignUpCreationRequest(path: String, method: String): Boolean {
    return method == "POST" && path.endsWith("/${ApiPaths.Client.SignUp.BASE}")
  }
}

/**
 * Re-checks that the response's device token is still current immediately before applying its
 * client, since the device token may change while the body is read and decoded. This narrows but
 * does not close the window: the check and apply are not atomic because applying the client
 * notifies shared-session sync, which takes its own lock and then storage's device-token lock.
 */
private fun Request.syncClient(response: Response, sync: () -> Unit) {
  val guardedSync: () -> Unit = {
    if (
      Clerk.isClientResponseCurrent(
        requestDeviceToken = response.request.header(AUTHORIZATION_HEADER),
        responseDeviceToken = response.header(AUTHORIZATION_HEADER),
      )
    ) {
      sync()
    } else {
      ClerkLog.d("Client sync skipped: device token changed while reading the response")
    }
  }
  tag(ResponseGuard::class.java)?.runIfAllowed(guardedSync) ?: guardedSync()
}

private fun Request.isClientFetch(): Boolean =
  method == "GET" && url.encodedPath.endsWith("/${ApiPaths.Client.BASE}")

private fun syncClerkClient(
  client: Client,
  order: ClientStateStore.ResponseOrder?,
  completedAuthFlow: AuthEvent?,
) {
  ClerkLog.d("Client synced: ${client.id}")
  Clerk.applyClientResponse(
    client = client,
    order = order,
    completedAuthFlow = completedAuthFlow,
  )
}

internal fun ClerkResult.Success<*>.responseOrder(): ClientStateStore.ResponseOrder? =
  (tags[Response::class] as? Response)?.request?.tag(ClientStateStore.ResponseOrder::class.java)

private fun Response.withResponseOrder(order: ClientStateStore.ResponseOrder?): Response {
  order ?: return this
  val taggedRequest = request.newBuilder().tag(ClientStateStore.ResponseOrder::class.java, order)
  return newBuilder().request(taggedRequest.build()).build()
}

private fun Response.serverDateMillis(): Long? {
  val serverDate = header(SERVER_DATE_HEADER) ?: return null
  return try {
    SimpleDateFormat(SERVER_DATE_FORMAT, Locale.US)
      .apply { timeZone = TimeZone.getTimeZone("GMT") }
      .parse(serverDate)
      ?.time
  } catch (_: Exception) {
    null
  }
}
