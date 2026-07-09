package com.clerk.api.trusteddevice

import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray

/**
 * Local metadata that links a Clerk trusted-device credential to its on-device private key.
 *
 * @property id The server-side trusted-device credential ID.
 * @property localKeyId The Android Keystore alias suffix of the private key.
 * @property userId The ID of the user the credential belongs to.
 * @property appIdentifier The application ID the credential is bound to.
 * @property identifierHint A normalized, local-only user identifier hint.
 * @property policy The local authentication policy protecting the private key.
 * @property createdAt The time the credential was created, in milliseconds since epoch.
 * @property updatedAt The time the credential was last updated, in milliseconds since epoch.
 */
@Serializable
internal data class TrustedDeviceLocalCredential(
  val id: String,
  @SerialName("localKeyId") val localKeyId: String,
  @SerialName("userId") val userId: String,
  @SerialName("appIdentifier") val appIdentifier: String,
  @SerialName("identifierHint") val identifierHint: String? = null,
  val policy: TrustedDevicePolicy = TrustedDevicePolicy.BIOMETRY_OR_DEVICE_PASSCODE,
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

/** Store for local trusted-device credential metadata. */
internal interface TrustedDeviceLocalCredentialStore {
  fun all(): List<TrustedDeviceLocalCredential>

  fun all(appIdentifier: String): List<TrustedDeviceLocalCredential> =
    all().filter { it.appIdentifier == appIdentifier }

  fun credential(id: String): TrustedDeviceLocalCredential? = all().firstOrNull { it.id == id }

  fun save(credential: TrustedDeviceLocalCredential)

  fun delete(id: String)

  fun deleteAll()
}

/**
 * Default [TrustedDeviceLocalCredentialStore] persisting credential metadata as encrypted JSON via
 * [StorageHelper].
 *
 * Malformed records are dropped on read instead of failing the whole list, so a single corrupt
 * entry can't lock out trusted-device sign-in.
 */
internal object DefaultTrustedDeviceLocalCredentialStore : TrustedDeviceLocalCredentialStore {

  @Suppress("ReturnCount")
  override fun all(): List<TrustedDeviceLocalCredential> {
    val stored =
      StorageHelper.loadValue(StorageKey.TRUSTED_DEVICE_CREDENTIALS) ?: return emptyList()
    val elements =
      runCatching { ClerkApi.json.parseToJsonElement(stored).jsonArray }
        .getOrElse {
          ClerkLog.w("Trusted-device credential metadata is malformed, clearing it.")
          deleteAll()
          return emptyList()
        }
    return elements.mapNotNull { element -> decodeCredential(element) }
  }

  override fun save(credential: TrustedDeviceLocalCredential) {
    persist(all().filterNot { it.id == credential.id } + credential)
  }

  override fun delete(id: String) {
    persist(all().filterNot { it.id == id })
  }

  override fun deleteAll() {
    StorageHelper.deleteValue(StorageKey.TRUSTED_DEVICE_CREDENTIALS)
  }

  private fun persist(credentials: List<TrustedDeviceLocalCredential>) {
    if (credentials.isEmpty()) {
      deleteAll()
      return
    }

    val elements =
      credentials.map {
        ClerkApi.json.encodeToJsonElement(TrustedDeviceLocalCredential.serializer(), it)
      }
    StorageHelper.saveValue(
      StorageKey.TRUSTED_DEVICE_CREDENTIALS,
      ClerkApi.json.encodeToString(JsonArray.serializer(), JsonArray(elements)),
    )
  }

  private fun decodeCredential(element: JsonElement): TrustedDeviceLocalCredential? {
    return runCatching {
        ClerkApi.json.decodeFromJsonElement(TrustedDeviceLocalCredential.serializer(), element)
      }
      .onFailure { ClerkLog.w("Dropping malformed trusted-device credential record.") }
      .getOrNull()
  }
}
