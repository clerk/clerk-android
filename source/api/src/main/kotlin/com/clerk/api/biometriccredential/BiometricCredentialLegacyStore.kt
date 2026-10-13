package com.clerk.api.biometriccredential

import com.clerk.api.log.ClerkLog
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

internal data class LegacyBiometricCredentialContents(
  val credentials: List<BiometricCredentialLocalRecord>,
  val pendingCleanupUserIds: Set<String>,
)

internal interface BiometricCredentialLegacyStore {
  fun load(): LegacyBiometricCredentialContents?

  fun clear()
}

internal object StorageHelperLegacyBiometricCredentialStore : BiometricCredentialLegacyStore {
  private val v1Json = Json {
    isLenient = true
    ignoreUnknownKeys = true
  }

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
    val elements = runCatching {
      v1Json.parseToJsonElement(json).jsonArray
    }
      .getOrElse {
        ClerkLog.w("Legacy biometric credential metadata is malformed, dropping it.")
        return emptyList()
      }
    return elements.mapNotNull { element ->
      runCatching { v1Json.decodeFromJsonElement(LegacyRecord.serializer(), element) }
        .onFailure { ClerkLog.w("Dropping malformed legacy biometric credential record.") }
        .getOrNull()
        ?.toRecord()
    }
  }

  fun decodePendingCleanup(json: String): Set<String> = runCatching {
    v1Json
      .parseToJsonElement(json)
      .jsonArray
      .mapNotNull { it.jsonPrimitive.contentOrNull }
      .filterTo(mutableSetOf()) { it.isNotBlank() }
  }
    .getOrElse {
      ClerkLog.w("Legacy biometric credential cleanup metadata is malformed, dropping it.")
      emptySet()
    }

  @Serializable
  private data class LegacyRecord(
    val id: String,
    @SerialName("local_key_id") val localKeyId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("app_identifier") val appIdentifier: String,
    @SerialName("identifier_hint") val identifierHint: String? = null,
    val policy: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
  ) {
    fun toRecord() =
      BiometricCredentialLocalRecord(
        id = id,
        localKeyId = localKeyId,
        userId = userId,
        appIdentifier = appIdentifier,
        identifierHintSha256 = BiometricCredentialLocalRecord.identifierHintSha256(identifierHint),
        policy =
          BiometricCredentialRecordJson.policy(policy)
            ?: BiometricCredentialPolicy.BIOMETRY_OR_DEVICE_PASSCODE,
        createdAt = createdAt,
        updatedAt = updatedAt,
      )
  }
}
