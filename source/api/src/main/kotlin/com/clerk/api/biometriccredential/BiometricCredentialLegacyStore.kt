package com.clerk.api.biometriccredential

import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/** Contract v1 metadata that still needs migrating into the v2 file store. */
internal data class LegacyBiometricCredentialContents(
  val credentials: List<BiometricCredentialLocalRecord>,
  val pendingCleanupUserIds: Set<String>,
)

/** Read-only access to contract v1 metadata, kept only to migrate it into contract v2. */
internal interface BiometricCredentialLegacyStore {
  /** Returns the v1 metadata, or null when no v1 value is stored. */
  fun load(): LegacyBiometricCredentialContents?

  fun clear()
}

/** Contract v1 metadata stored encrypted in `clerk_preferences` through [StorageHelper]. */
internal object StorageHelperLegacyBiometricCredentialStore : BiometricCredentialLegacyStore {

  override fun load(): LegacyBiometricCredentialContents? {
    val credentials = StorageHelper.loadValue(StorageKey.TRUSTED_DEVICE_CREDENTIALS)
    val pendingCleanup =
      StorageHelper.loadValue(StorageKey.PENDING_TRUSTED_DEVICE_CREDENTIAL_CLEANUP)
    if (credentials == null && pendingCleanup == null) return null
    return LegacyBiometricCredentialContents(
      credentials = credentials?.let(::decodeCredentials).orEmpty(),
      pendingCleanupUserIds = pendingCleanup?.let(::decodePendingCleanup).orEmpty(),
    )
  }

  override fun clear() {
    StorageHelper.deleteValue(StorageKey.TRUSTED_DEVICE_CREDENTIALS)
    StorageHelper.deleteValue(StorageKey.PENDING_TRUSTED_DEVICE_CREDENTIAL_CLEANUP)
  }

  fun decodeCredentials(json: String): List<BiometricCredentialLocalRecord> {
    val elements =
      runCatching { ClerkApi.json.parseToJsonElement(json).jsonArray }
        .getOrElse {
          ClerkLog.w("Legacy biometric credential metadata is malformed, dropping it.")
          return emptyList()
        }
    return elements.mapNotNull { element ->
      runCatching { ClerkApi.json.decodeFromJsonElement(LegacyRecord.serializer(), element) }
        .onFailure { ClerkLog.w("Dropping malformed legacy biometric credential record.") }
        .getOrNull()
        ?.toRecord()
    }
  }

  fun decodePendingCleanup(json: String): Set<String> =
    runCatching {
        ClerkApi.json
          .parseToJsonElement(json)
          .jsonArray
          .mapNotNull { it.jsonPrimitive.contentOrNull }
          .filterTo(mutableSetOf()) { it.isNotBlank() }
      }
      .getOrElse {
        ClerkLog.w("Legacy biometric credential cleanup metadata is malformed, dropping it.")
        emptySet()
      }

  /** v1 record; `ClerkApi.json` maps these property names to the v1 snake_case keys. */
  @Serializable
  private data class LegacyRecord(
    val id: String,
    val localKeyId: String,
    val userId: String,
    val appIdentifier: String,
    val identifierHint: String? = null,
    // v1 reads a missing, null or unknown policy as PIN-capable.
    val policy: BiometricCredentialPolicy = BiometricCredentialPolicy.BIOMETRY_OR_DEVICE_PASSCODE,
    val createdAt: Long,
    val updatedAt: Long,
  ) {
    fun toRecord() =
      BiometricCredentialLocalRecord(
        id = id,
        localKeyId = localKeyId,
        userId = userId,
        appIdentifier = appIdentifier,
        identifierHintSha256 = BiometricCredentialLocalRecord.identifierHintSha256(identifierHint),
        policy = policy,
        createdAt = createdAt,
        updatedAt = updatedAt,
      )
  }
}
