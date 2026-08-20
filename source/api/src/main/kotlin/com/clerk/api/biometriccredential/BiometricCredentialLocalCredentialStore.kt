package com.clerk.api.biometriccredential

import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Local metadata that links a Clerk biometric credential to its on-device private key.
 *
 * @property id The server-side biometric credential ID.
 * @property localKeyId The Android Keystore alias suffix of the private key.
 * @property userId The ID of the user the credential belongs to.
 * @property appIdentifier The application ID the credential is bound to.
 * @property identifierHint A normalized, local-only user identifier hint.
 * @property policy The local authentication policy protecting the private key.
 * @property createdAt The time the credential was created, in milliseconds since epoch.
 * @property updatedAt The time the credential was last updated, in milliseconds since epoch.
 */
@Serializable
internal data class BiometricCredentialLocalCredential(
  val id: String,
  @SerialName("localKeyId") val localKeyId: String,
  @SerialName("userId") val userId: String,
  @SerialName("appIdentifier") val appIdentifier: String,
  @SerialName("identifierHint") val identifierHint: String? = null,
  val policy: BiometricCredentialPolicy = BiometricCredentialPolicy.BIOMETRY_OR_DEVICE_PASSCODE,
  @SerialName("createdAt") val createdAt: Long,
  @SerialName("updatedAt") val updatedAt: Long,
) {

  /** Returns whether this credential matches the given user identifier hint. */
  fun matches(identifierHint: String?): Boolean {
    val normalized = normalizedIdentifierHint(identifierHint) ?: return true
    return this.identifierHint == normalized
  }

  companion object {
    fun normalizedIdentifierHint(identifierHint: String?): String? {
      val normalized = identifierHint?.trim()?.lowercase()
      return normalized?.takeIf { it.isNotEmpty() }
    }
  }
}

/** Store for local biometric credential metadata. */
internal interface BiometricCredentialLocalCredentialStore {
  fun all(): List<BiometricCredentialLocalCredential>

  fun all(appIdentifier: String): List<BiometricCredentialLocalCredential> =
    all().filter { it.appIdentifier == appIdentifier }

  fun credential(id: String): BiometricCredentialLocalCredential? =
    all().firstOrNull { it.id == id }

  fun save(credential: BiometricCredentialLocalCredential)

  fun delete(id: String)

  fun deleteAll()
}

/**
 * Default [BiometricCredentialLocalCredentialStore] persisting credential metadata as encrypted
 * JSON via [StorageHelper].
 *
 * Malformed records are dropped on read instead of failing the whole list, so a single corrupt
 * entry can't lock out biometric-credential sign-in.
 */
internal object DefaultBiometricCredentialLocalCredentialStore :
  BiometricCredentialLocalCredentialStore {

  @Suppress("ReturnCount")
  override fun all(): List<BiometricCredentialLocalCredential> {
    val stored =
      StorageHelper.loadValue(StorageKey.TRUSTED_DEVICE_CREDENTIALS) ?: return emptyList()
    val elements =
      runCatching { ClerkApi.json.parseToJsonElement(stored).jsonArray }
        .getOrElse {
          ClerkLog.w("Biometric-credential credential metadata is malformed, clearing it.")
          deleteAll()
          return emptyList()
        }
    return elements.mapNotNull { element -> decodeCredential(element) }
  }

  override fun save(credential: BiometricCredentialLocalCredential) {
    persist(all().filterNot { it.id == credential.id } + credential)
  }

  override fun delete(id: String) {
    persist(all().filterNot { it.id == id })
  }

  override fun deleteAll() {
    StorageHelper.deleteValue(StorageKey.TRUSTED_DEVICE_CREDENTIALS)
  }

  private fun persist(credentials: List<BiometricCredentialLocalCredential>) {
    if (credentials.isEmpty()) {
      deleteAll()
      return
    }

    val elements = credentials.map {
      ClerkApi.json.encodeToJsonElement(BiometricCredentialLocalCredential.serializer(), it)
    }
    StorageHelper.saveValue(
      StorageKey.TRUSTED_DEVICE_CREDENTIALS,
      ClerkApi.json.encodeToString(JsonArray.serializer(), JsonArray(elements)),
    )
  }

  private fun decodeCredential(element: JsonElement): BiometricCredentialLocalCredential? {
    return runCatching {
        ClerkApi.json.decodeFromJsonElement(
          BiometricCredentialLocalCredential.serializer(),
          element,
        )
      }
      .onFailure { ClerkLog.w("Dropping malformed biometric credential record.") }
      .getOrNull()
  }
}

/** Persistent queue of user-scoped local credential cleanup work. */
internal object BiometricCredentialPendingCleanupStore {

  fun all(): Set<String> {
    val stored =
      StorageHelper.loadValue(StorageKey.PENDING_TRUSTED_DEVICE_CREDENTIAL_CLEANUP)
        ?: return emptySet()
    return runCatching {
        ClerkApi.json
          .parseToJsonElement(stored)
          .jsonArray
          .mapNotNull { it.jsonPrimitive.contentOrNull }
          .filterTo(mutableSetOf()) { it.isNotBlank() }
      }
      .getOrElse {
        ClerkLog.w("Biometric-credential pending cleanup metadata is malformed, clearing it.")
        StorageHelper.deleteValue(StorageKey.PENDING_TRUSTED_DEVICE_CREDENTIAL_CLEANUP)
        emptySet()
      }
  }

  fun add(userId: String) {
    persist(all() + userId)
  }

  fun remove(userId: String) {
    persist(all() - userId)
  }

  private fun persist(userIds: Set<String>) {
    if (userIds.isEmpty()) {
      StorageHelper.deleteValue(StorageKey.PENDING_TRUSTED_DEVICE_CREDENTIAL_CLEANUP)
      return
    }
    val encoded = JsonArray(userIds.sorted().map(::JsonPrimitive))
    StorageHelper.saveValue(
      StorageKey.PENDING_TRUSTED_DEVICE_CREDENTIAL_CLEANUP,
      ClerkApi.json.encodeToString(JsonArray.serializer(), encoded),
    )
  }
}
