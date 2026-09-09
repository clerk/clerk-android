package com.clerk.api

import java.io.File
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.Executors
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

private class FixtureCapabilities(path: String) : NativeCapabilities {
  override val supported = setOf("http", "storage", "timer", "random", "browser")
  private val fixtures = Json.parseToJsonElement(File(path).readText()).jsonObject
  var credential: String? = null
  val requests = mutableListOf<JsonObject>()
  var browserCount = 0
  override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
    val args = arguments.jsonObject
    when (capability) {
      "storage.read" -> return credential?.let(::JsonPrimitive) ?: JsonNull
      "storage.write" -> { credential = args.getValue("value").requireString(); return JsonNull }
      "storage.remove" -> { credential = null; return JsonNull }
      "timer" -> { delay(args.getValue("milliseconds").jsonPrimitive.long); return JsonNull }
      "browser" -> {
        browserCount++
        check(args["url"] == JsonPrimitive("https://provider.example/authorize"))
        return buildJsonObject { put("callbackUrl", "clerk-test://sso-callback?rotating_token_nonce=native_nonce") }
      }
    }
    check(capability == "http")
    requests += args
    val url = URI(args.getValue("url").requireString())
    val response = when {
      url.path.endsWith("/environment") -> fixtures.getValue("environment")
      url.path.endsWith("/client") -> fixtures.getValue("client")
      else -> {
        val resource = fixtures.getValue(if (url.path.contains("sign_ins")) "signIn" else "signUp").jsonObject.toMutableMap()
        if (args["method"] == JsonPrimitive("GET")) {
          check(url.rawQuery.contains("rotating_token_nonce=native_nonce"))
          resource["status"] = JsonPrimitive("complete")
          resource["created_session_id"] = JsonPrimitive("session_native")
        }
        JsonObject(resource)
      }
    }
    return buildJsonObject {
      put("status", 200)
      put("headers", buildJsonObject { put("authorization", "fixture-client-credential") })
      put("body", buildJsonObject { put("response", response) }.toString())
    }
  }
}

fun main(args: Array<String>) = runBlocking {
  val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
  try {
    withContext(dispatcher) {
      withTimeout(30000) {
        val started = System.nanoTime()
        val bundle = File(args[0]).readBytes()
        val capabilities = FixtureCapabilities(args[1])
        val transport = QuickJSTransport(capabilities, dispatcher)
        val runtime = CoreRuntime(transport, dispatcher)
        val deliver = transport.receive
        transport.receive = { message ->
          if (message.jsonObject["kind"] in listOf(JsonPrimitive("runtimeError"), JsonPrimitive("initializationFailed"))) println("Engine error: $message")
          deliver?.invoke(message)
        }
        val hash = MessageDigest.getInstance("SHA-256").digest(bundle).joinToString("") { "%02x".format(it) }
        transport.start(bundle, hash)
        val key = "pk_test_" + java.util.Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
        runtime.initialize(key, "clerk-test://sso-callback", "android", capabilities.supported)
        val clerk = runtime.resource(runtime.roots.getValue("clerk")) as Clerk
        val signIn = clerk.signIn
        check(signIn === runtime.resource(runtime.roots.getValue("signIn")))
        signIn.sso(SignInSSOParams(SignInSSOParamsStrategy.OauthGoogle))
        check(signIn.status.rawValue == "complete" && signIn.createdSessionId == "session_native")
        check(clerk.session == null)
        clerk.signUp.sso(SignUpSSOParams("oauth_google"))
        check(clerk.signUp.status.rawValue == "complete" && clerk.session == null)
        check(capabilities.browserCount == 2)
        check(capabilities.requests.none { it.getValue("url").requireString().contains("/sessions") })
        val group = signIn.emailCode
        val count = capabilities.requests.size
        signIn.reset()
        check(capabilities.requests.size == count && group.isInvalidated)
        try { group.verifyCode(SignInEmailCodeVerifyParams("123456")); error("Stale group accepted") }
        catch (error: CoreException) { check(error.code == "stale_resource") }
        println("PASS: generated Kotlin API -> JNI QuickJS -> real future SSO; state before completion; no implicit activation; local reset; stale nested group. Elapsed ${(System.nanoTime() - started) / 1_000_000} ms")
        runtime.close()
        delay(100)
      }
    }
  } finally { dispatcher.close() }
}
