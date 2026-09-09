package com.clerk.api

import android.app.Activity
import android.content.Context
import kotlinx.serialization.json.*

public class AndroidBiometricCapabilities(
  context: Context,
  private val publishableKey: String,
  private val credentials: CredentialStorage,
  private val cleanup: CredentialStorage,
  activity: (() -> Activity?)? = null,
) {
  private val appIdentifier = context.applicationContext.packageName
  private val keys = AndroidBiometricKeyManager(context.applicationContext, activity)

  internal suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
    val args = arguments.jsonObject
    fun string(key: String): String = args.getValue(key).requireString()
    fun policy(): BiometricCredentialPolicy = when (string("policy")) {
      "biometry_current_set" -> BiometricCredentialPolicy.BiometryCurrentSet
      "biometry_any" -> BiometricCredentialPolicy.BiometryAny
      "biometry_or_device_passcode" -> BiometricCredentialPolicy.BiometryOrDevicePasscode
      else -> throw CoreException("unsupported_biometric_policy")
    }
    try {
      return when (capability) {
        "biometrics.appIdentifier" -> JsonPrimitive(appIdentifier)
        "biometrics.storage.read", "biometrics.storage.write" -> {
          if (args["scope"] != JsonPrimitive(publishableKey)) throw CoreException("invalid_storage_scope")
          val storage = when (string("key")) {
            "credentials" -> credentials
            "cleanup" -> cleanup
            else -> throw CoreException("invalid_storage_scope")
          }
          if (capability == "biometrics.storage.read") storage.read()?.let(::JsonPrimitive) ?: JsonNull
          else { storage.write(string("value")); JsonNull }
        }
        "biometrics.supports" -> JsonPrimitive(keys.isSupported(policy()))
        "biometrics.hasKey" -> JsonPrimitive(keys.hasKey(string("localKeyId")))
        "biometrics.createKey" -> {
          val key = keys.createKey(policy())
          buildJsonObject { put("localKeyId", key.localKeyId); put("publicKeyJwk", key.publicKeyJwk) }
        }
        "biometrics.sign" -> {
          val signature = keys.sign(string("clientData"), string("localKeyId"), policy(), string("reason"), args["promptSubtitle"]?.takeUnless { it == JsonNull }?.requireString())
          buildJsonObject { put("clientData", signature.clientData); put("signature", signature.signature); put("algorithm", signature.algorithm) }
        }
        "biometrics.deleteKey" -> { keys.deleteKey(string("localKeyId")); JsonNull }
        else -> throw CoreException("capability_unavailable")
      }
    } catch (error: BiometricCredentialKeyManagerException) {
      val code = when (error.code) {
        BiometricCredentialKeyManagerException.Code.BIOMETRIC_AUTHENTICATION_CANCELED -> "user_cancelled"
        BiometricCredentialKeyManagerException.Code.KEY_NOT_FOUND -> "key_not_found"
        BiometricCredentialKeyManagerException.Code.KEY_INVALIDATED -> "key_invalidated"
        BiometricCredentialKeyManagerException.Code.UNSUPPORTED_PLATFORM -> "capability_unavailable:biometrics"
        else -> error.code.name.lowercase()
      }
      throw CoreException(code)
    }
  }
}
