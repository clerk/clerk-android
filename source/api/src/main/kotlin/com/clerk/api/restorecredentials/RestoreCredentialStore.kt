package com.clerk.api.restorecredentials

import androidx.annotation.VisibleForTesting
import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLogger
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class RestoreCredentialState(
  val signInAttempted: Boolean = false,
  val deviceKeyUserId: String? = null,
  val createdPasskey: RestorePasskey? = null,
  val pendingSignInId: String? = null,
  val restoredSessionId: String? = null,
)

@Serializable internal data class RestorePasskey(val userId: String, val passkeyId: String)

/**
 * Keeps [RestoreCredentialState] in `noBackupFilesDir`.
 *
 * Android backup and device-to-device transfer skip `noBackupFilesDir`, so a restored or
 * reinstalled app starts with an empty state while the restore key itself travels with Credential
 * Manager.
 */
internal object RestoreCredentialStore {
  private const val FILE_NAME = "clerk_restore_credential.json"
  private const val TEMP_FILE_NAME = "$FILE_NAME.tmp"
  private val json = Json { ignoreUnknownKeys = true }

  @VisibleForTesting
  internal var directory: () -> File? = { Clerk.applicationContext?.get()?.noBackupFilesDir }

  @Synchronized
  fun read(): RestoreCredentialState {
    val file = directory()?.let { File(it, FILE_NAME) }
    if (file == null || !file.exists()) return RestoreCredentialState()
    return runCatching { json.decodeFromString<RestoreCredentialState>(file.readText()) }
      .onFailure { ClerkLogger.w("Failed to read restore credential state: ${it.message}") }
      .getOrDefault(RestoreCredentialState())
  }

  @Synchronized
  fun update(transform: (RestoreCredentialState) -> RestoreCredentialState): Boolean = runCatching {
    val directory =
      directory() ?: throw IOException("Clerk must be initialized to save restore state.")
    write(directory, transform(read()))
  }
    .onFailure { ClerkLogger.w("Failed to save restore credential state: ${it.message}") }
    .isSuccess

  private fun write(directory: File, state: RestoreCredentialState) {
    if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
      throw IOException("Failed to create ${directory.path}.")
    }
    val tempFile = File(directory, TEMP_FILE_NAME)
    FileOutputStream(tempFile).use { output ->
      output.write(json.encodeToString(RestoreCredentialState.serializer(), state).toByteArray())
      output.fd.sync()
    }
    if (!tempFile.renameTo(File(directory, FILE_NAME))) {
      tempFile.delete()
      throw IOException("Failed to replace the restore credential state.")
    }
  }

  @VisibleForTesting
  internal fun reset() {
    directory()?.let { File(it, FILE_NAME).delete() }
  }
}
