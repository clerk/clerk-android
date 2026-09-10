package com.clerk.api

import android.os.Build
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import kotlinx.serialization.json.*

/** Converts bridge binary values to the WebAuthn JSON accepted by Credential Manager. */
internal object AndroidPasskeyRequests {
  fun create(arguments: JsonElement): CreatePublicKeyCredentialRequest {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) throw CoreException("capability_unavailable")
    return CreatePublicKeyCredentialRequest(webAuthnJSON(arguments))
  }

  fun get(arguments: JsonElement): GetCredentialRequest {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) throw CoreException("capability_unavailable")
    val options = arguments.jsonObject
    if (options["conditionalUI"]?.jsonPrimitive?.booleanOrNull == true) throw CoreException("capability_unavailable")
    return GetCredentialRequest(
      credentialOptions = listOf(GetPublicKeyCredentialOption(webAuthnJSON(arguments))),
      preferImmediatelyAvailableCredentials = options["preferImmediatelyAvailableCredentials"]?.jsonPrimitive?.booleanOrNull ?: false,
    )
  }

  private fun webAuthnJSON(arguments: JsonElement): String =
    binaryStrings(JsonObject(arguments.jsonObject - setOf("conditionalUI", "preferImmediatelyAvailableCredentials"))).toString()

  private fun binaryStrings(value: JsonElement): JsonElement = when (value) {
    is JsonArray -> JsonArray(value.map(::binaryStrings))
    is JsonObject -> {
      if (value.keys == setOf("base64url")) {
        val encoded = value.getValue("base64url").requireString()
        if (!encoded.matches(Regex("[A-Za-z0-9_-]+"))) throw CoreException("invalid_credential_options")
        JsonPrimitive(encoded)
      } else JsonObject(value.mapValues { (_, item) -> binaryStrings(item) })
    }
    else -> value
  }
}
