package com.clerk.api.biometriccredential

import android.content.Context
import java.io.File
import java.security.MessageDigest

/**
 * Local metadata that links a Clerk biometric credential to its on-device private key.
 *
 * @property id The server-side biometric credential ID.
 * @property localKeyId The Android Keystore alias suffix of the private key.
 * @property userId The ID of the user the credential belongs to.
 * @property appIdentifier The application ID the credential is bound to.
 * @property policy The local authentication policy protecting the private key.
 * @property createdAt The time the credential was created, in milliseconds since epoch.
 * @property updatedAt The time the credential was last updated, in milliseconds since epoch.
 */
internal data class BiometricCredentialLocalRecord(
  val id: String,
  val localKeyId: String,
  val userId: String,
  val appIdentifier: String,
  val identifierHintSha256: String? = null,
  val policy: BiometricCredentialPolicy = BiometricCredentialPolicy.BIOMETRY_OR_DEVICE_PASSCODE,
  val createdAt: Long,
  val updatedAt: Long,
) {
  fun matches(identifierHint: String?): Boolean {
    val hash = identifierHintSha256(identifierHint) ?: return true
    return identifierHintSha256 == hash
  }

  companion object {
    fun normalizedIdentifierHint(identifierHint: String?): String? {
      val normalized = identifierHint?.trim()?.lowercase()
      return normalized?.takeIf { it.isNotEmpty() }
    }

    fun identifierHintSha256(identifierHint: String?): String? {
      val normalized = normalizedIdentifierHint(identifierHint) ?: return null
      return MessageDigest.getInstance("SHA-256")
        .digest(normalized.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
    }
  }
}

internal interface BiometricCredentialLocalStore {
  fun all(): List<BiometricCredentialLocalRecord>

  fun all(appIdentifier: String): List<BiometricCredentialLocalRecord> =
    all().filter { it.appIdentifier == appIdentifier }

  fun credential(id: String): BiometricCredentialLocalRecord? = all().firstOrNull { it.id == id }

  fun save(credential: BiometricCredentialLocalRecord)

  fun delete(id: String)
}

internal object BiometricCredentialStorage {
  @Volatile var fileStore: BiometricCredentialFileStore? = null

  @Synchronized
  fun initialize(context: Context) {
    if (fileStore == null) {
      fileStore =
        BiometricCredentialFileStore(
          directory = directory(context),
          legacyStore = StorageHelperLegacyBiometricCredentialStore,
        )
    }
  }

  fun directory(context: Context): File =
    File(context.applicationContext.noBackupFilesDir, BiometricCredentialFileStore.DIRECTORY_NAME)

  fun requireFileStore(): BiometricCredentialFileStore =
    checkNotNull(fileStore) { "Biometric credential storage is not initialized." }
}

internal object DefaultBiometricCredentialLocalStore : BiometricCredentialLocalStore {

  override fun all(): List<BiometricCredentialLocalRecord> =
    BiometricCredentialStorage.fileStore?.credentials().orEmpty()

  override fun save(credential: BiometricCredentialLocalRecord) {
    BiometricCredentialStorage.requireFileStore().saveCredential(credential)
  }

  override fun delete(id: String) {
    BiometricCredentialStorage.requireFileStore().deleteCredential(id)
  }
}

internal object BiometricCredentialPendingCleanupStore {

  fun all(): Set<String> = BiometricCredentialStorage.fileStore?.pendingCleanupUserIds().orEmpty()

  fun add(userId: String) {
    BiometricCredentialStorage.requireFileStore().addPendingCleanupUserId(userId)
  }

  fun remove(userId: String) {
    BiometricCredentialStorage.requireFileStore().removePendingCleanupUserId(userId)
  }
}
