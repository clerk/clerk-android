package com.clerk.api

import android.app.Activity
import android.content.Context
import java.net.URI
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

public class ClerkConfiguration(
  publishableKey: String,
  public val callbackUrl: String,
  public val legacyPublishableKey: String? = null,
) {
  public val publishableKey: String = publishableKey.trim()
  public val frontendAPI: String
  init {
    if (!this.publishableKey.startsWith("pk_test_") && !this.publishableKey.startsWith("pk_live_")) throw CoreException("invalid_publishable_key")
    val decoded = try { Base64.getDecoder().decode(this.publishableKey.substring(8).replace('-', '+').replace('_', '/')).toString(Charsets.UTF_8) }
    catch (_: IllegalArgumentException) { throw CoreException("invalid_publishable_key") }
    if (!decoded.endsWith('$')) throw CoreException("invalid_publishable_key")
    val origin = URI("https://${decoded.dropLast(1)}")
    if (origin.host.isNullOrEmpty() || origin.userInfo != null || !origin.path.isNullOrEmpty() || origin.query != null || origin.fragment != null) throw CoreException("invalid_publishable_key")
    frontendAPI = origin.toString()
    val callback = URI(callbackUrl)
    if (callback.scheme in setOf(null, "http", "javascript", "data", "file", "about") || callback.host.isNullOrEmpty() || callback.userInfo != null || callback.fragment != null) throw CoreException("invalid_callback_url")
  }
}

public suspend fun Clerk.Companion.connect(
  context: Context,
  configuration: ClerkConfiguration,
  activity: (() -> Activity?)? = null,
  storage: CredentialStorage = AndroidCredentialStorage(context, configuration.publishableKey, configuration.legacyPublishableKey),
  authStorage: CredentialStorage = AndroidCredentialStorage(context, configuration.publishableKey, configuration.legacyPublishableKey, AndroidCredentialStorage.Purpose.MAGIC_LINK),
  magicLinkAttestation: (suspend () -> String?)? = null,
): Clerk {
  val capabilities = AndroidCapabilities(configuration.publishableKey, configuration.frontendAPI, storage, activity, activity?.let(::BrowserAuthentication), authStorage, magicLinkAttestation)
  return connect(context, configuration, capabilities)
}

public suspend fun Clerk.Companion.connect(context: Context, configuration: ClerkConfiguration, capabilities: NativeCapabilities): Clerk = withContext(Dispatchers.Main.immediate) {
  val bundle = withContext(Dispatchers.IO) { context.assets.open("clerk-core.js").use { it.readBytes() } }
  val transport = QuickJSTransport(capabilities)
  val runtime = CoreRuntime(transport)
  try {
    transport.start(bundle, BundledCore.sha256)
    runtime.initialize(configuration.publishableKey, configuration.callbackUrl, "android", capabilities.supported)
    observeApplicationLifecycle(runtime)
    runtime.resource(runtime.roots["clerk"] ?: throw CoreException("missing_clerk_root")) as Clerk
  } catch (error: Exception) { runtime.close(); throw error }
}

public fun Clerk.close() { context.requireRuntime().close() }
public const val CLERK_SDK_VERSION: String = "2.0.0-alpha.0"
