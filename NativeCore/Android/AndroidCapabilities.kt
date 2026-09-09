package com.clerk.api

import android.app.Activity
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCancellationException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Base64
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

public class AndroidCapabilities(
  private val publishableKey: String,
  frontendAPI: String,
  private val storage: CredentialStorage,
  private val activity: (() -> Activity?)? = null,
  private val browser: BrowserAuthentication? = null,
) : NativeCapabilities {
  private val origin = frontendAPI.toHttpUrl().also { if (it.scheme != "https" || it.username.isNotEmpty() || it.password.isNotEmpty()) throw CoreException("invalid_frontend_api") }
  private val http = OkHttpClient.Builder().cookieJar(CookieJar.NO_COOKIES).cache(null)
    .followRedirects(false).followSslRedirects(false).build()
  override val supported: Set<String> get() = setOf("http", "storage", "timer", "random") +
    (if (browser != null) setOf("browser") else emptySet()) + (if (activity != null) setOf("passkeys") else emptySet())

  override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
    val args = arguments.jsonObject
    if (capability == "timer") { delay(args.getValue("milliseconds").jsonPrimitive.long.coerceIn(0, Int.MAX_VALUE.toLong())); return JsonNull }
    if (capability == "browser") return browser?.open(args.getValue("url").requireString(), args.getValue("callbackUrl").requireString()) ?: throw CoreException("capability_unavailable")
    if (capability.startsWith("passkeys.")) return passkey(capability, arguments)
    if (capability.startsWith("storage.")) {
      if (args["scope"] != JsonPrimitive(publishableKey) || args["key"] != JsonPrimitive("client")) throw CoreException("invalid_storage_scope")
      return when (capability) {
        "storage.read" -> storage.read()?.let(::JsonPrimitive) ?: JsonNull
        "storage.write" -> { storage.write(args.getValue("value").requireString()); JsonNull }
        "storage.remove" -> { storage.remove(); JsonNull }
        else -> throw CoreException("capability_unavailable")
      }
    }
    if (capability != "http") throw CoreException("capability_unavailable")
    return request(args)
  }

  private suspend fun passkey(capability: String, arguments: JsonElement): JsonElement = withContext(Dispatchers.Main.immediate) {
    val context = activity?.invoke()?.takeUnless { it.isFinishing || it.isDestroyed } ?: throw CoreException("presentation_unavailable")
    val manager = CredentialManager.create(context)
    try {
      val json = if (capability == "passkeys.create") {
        val response = manager.createCredential(context, CreatePublicKeyCredentialRequest(arguments.toString())) as? CreatePublicKeyCredentialResponse ?: throw CoreException("invalid_credential_response")
        response.registrationResponseJson
      } else if (capability == "passkeys.get") {
        val response = manager.getCredential(context, GetCredentialRequest(listOf(GetPublicKeyCredentialOption(arguments.toString()))))
        (response.credential as? PublicKeyCredential)?.authenticationResponseJson ?: throw CoreException("invalid_credential_response")
      } else throw CoreException("capability_unavailable")
      Json.parseToJsonElement(json)
    } catch (_: CreateCredentialCancellationException) { throw CoreException("user_cancelled") }
    catch (_: GetCredentialCancellationException) { throw CoreException("user_cancelled") }
  }

  private fun sameOrigin(url: HttpUrl): Boolean = url.scheme == "https" && url.host == origin.host && url.port == origin.port && url.username.isEmpty() && url.password.isEmpty()

  private suspend fun request(args: JsonObject): JsonElement = withContext(Dispatchers.IO) {
    val url = args.getValue("url").requireString().toHttpUrl()
    if (!sameOrigin(url)) throw CoreException("invalid_http_origin")
    val headers = args.getValue("headers").jsonObject
    val builder = Request.Builder().url(url)
    headers.forEach { (name, value) -> if (!name.equals("cookie", true) && !name.equals("host", true)) builder.header(name, value.requireString()) }
    val method = args.getValue("method").requireString()
    val bodyValue = args["body"]
    val body = if (bodyValue is JsonObject && bodyValue["multipart"] != null) {
      builder.removeHeader("content-type")
      MultipartBody.Builder().setType(MultipartBody.FORM).apply {
        bodyValue.getValue("multipart").jsonArray.forEach { value ->
          val part = value.jsonObject
          val name = part.getValue("name").requireString()
          if (part["value"] != null) addFormDataPart(name, part.getValue("value").requireString())
          else addFormDataPart(name, part.getValue("filename").requireString(), Base64.getDecoder().decode(part.getValue("base64").requireString()).toRequestBody(part.getValue("contentType").requireString().toMediaType()))
        }
      }.build()
    } else if (bodyValue != null && bodyValue != JsonNull) bodyValue.requireString().toRequestBody(null)
    else if (method !in setOf("GET", "HEAD")) ByteArray(0).toRequestBody(null) else null
    var request = builder.method(method, body).build()
    repeat(6) { redirects ->
      val response = http.newCall(request).await()
      response.use {
        val destination = response.header("location")?.let(request.url::resolve)
        if (response.code in setOf(301, 302, 303, 307, 308) && destination != null) {
          if (!sameOrigin(destination) || redirects == 5) throw CoreException("invalid_http_redirect")
          val next = request.newBuilder().url(destination)
          if (response.code == 303 || response.code in setOf(301, 302) && request.method == "POST") next.get().removeHeader("content-type")
          request = next.build()
        } else {
          val output = ByteArrayOutputStream()
          response.body.byteStream().use { input ->
            val chunk = ByteArray(8192)
            while (true) {
              val count = input.read(chunk)
              if (count < 0) break
              if (output.size() + count > 16 * 1024 * 1024) throw CoreException("response_too_large")
              output.write(chunk, 0, count)
            }
          }
          return@withContext buildJsonObject {
            put("status", response.code)
            put("headers", buildJsonObject { response.headers.names().forEach { name -> if (!name.equals("set-cookie", true)) put(name.lowercase(), response.header(name).orEmpty()) } })
            put("body", output.toString(Charsets.UTF_8.name()))
          }
        }
      }
    }
    throw CoreException("invalid_http_redirect")
  }

  private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
      override fun onFailure(call: Call, e: IOException) { if (continuation.isActive) continuation.resumeWithException(CoreException("network_error")) }
      override fun onResponse(call: Call, response: Response) {
        continuation.resume(response) { _, value, _ -> value.close() }
      }
    })
  }
}
