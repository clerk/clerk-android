package com.clerk.api.biometriccredential

import android.system.Os
import android.system.OsConstants
import com.clerk.api.log.ClerkLog
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

@Suppress("TooManyFunctions")
internal class BiometricCredentialFileStore(
  private val directory: File,
  private val legacyStore: BiometricCredentialLegacyStore? = null,
  private val processGuard: ReentrantLock = PROCESS_GUARD,
  private val lockTimeoutMillis: Long = LOCK_TIMEOUT_MILLIS,
) {
  val dataFile = File(directory, DATA_FILE_NAME)
  val lockFile = File(directory, LOCK_FILE_NAME)
  private val tempFile = File(directory, TEMP_FILE_NAME)

  @Volatile private var lockChannel: FileChannel? = null
  @Volatile private var migrated = legacyStore == null

  fun credentials(): List<BiometricCredentialLocalRecord> {
    ensureMigrated()
    return readDocumentOrNull()?.records().orEmpty()
  }

  fun pendingCleanupUserIds(): Set<String> {
    ensureMigrated()
    return readDocumentOrNull()?.pendingCleanupUserIds().orEmpty()
  }

  fun saveCredential(credential: BiometricCredentialLocalRecord) = update {
    it.replacingCredential(credential)
  }

  fun deleteCredential(id: String) = update { document ->
    document.withCredentialElements(document.credentialElements().filterNot { it.recordId == id })
  }

  fun addPendingCleanupUserId(userId: String) = update {
    it.withPendingCleanupUserIds(it.pendingCleanupUserIds() + userId)
  }

  fun removePendingCleanupUserId(userId: String) = update {
    it.withPendingCleanupUserIds(it.pendingCleanupUserIds() - userId)
  }

  private fun update(transform: (StoreDocument) -> StoreDocument) {
    withExclusiveLock {
      migrateLegacyLocked()
      val current = readDocument()
      if (!current.writable) {
        throw IOException("Biometric credential store has an unsupported version.")
      }
      val updated = transform(current).normalized()
      if (updated.root != current.root) {
        writeDocument(updated.root)
      }
    }
  }

  private fun ensureMigrated() {
    if (migrated) return
    runCatching { withExclusiveLock { migrateLegacyLocked() } }
      .onFailure { ClerkLog.w("Failed to migrate biometric credential metadata: ${it.message}") }
  }

  private fun migrateLegacyLocked() {
    if (migrated) return
    val legacy = legacyStore?.load()
    if (legacy != null) {
      val current = readDocument()
      if (!current.writable) {
        throw IOException("Biometric credential store has an unsupported version.")
      }
      val existingIds = current.credentialElements().mapNotNullTo(mutableSetOf()) { it.recordId }
      val migratedElements =
        legacy.credentials
          .filter { existingIds.add(it.id) }
          .map { BiometricCredentialRecordJson.encode(it, preserving = null) }
      val updated =
        current
          .withCredentialElements(current.credentialElements() + migratedElements)
          .withPendingCleanupUserIds(current.pendingCleanupUserIds() + legacy.pendingCleanupUserIds)
          .normalized()
      writeDocument(updated.root)
      legacyStore.clear()
    }
    migrated = true
  }

  private fun readDocumentOrNull(): StoreDocument? = runCatching {
    readDocument()
  }
    .onFailure { ClerkLog.w("Failed to read biometric credential metadata: ${it.message}") }
    .getOrNull()
    ?.takeIf { it.writable }

  private fun readDocument(): StoreDocument {
    if (!dataFile.exists()) return StoreDocument.EMPTY
    val text = dataFile.readText(Charsets.UTF_8)
    val root = runCatching { JSON.parseToJsonElement(text) as? JsonObject }.getOrNull()
    val version = (root?.get(KEY_VERSION) as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
    return if (root == null) {
      ClerkLog.w("Biometric credential store is malformed, treating it as empty.")
      StoreDocument.EMPTY
    } else {
      StoreDocument(root, writable = version == STORE_VERSION)
    }
  }

  private fun writeDocument(root: JsonObject) {
    ensureDirectory()
    val bytes = JSON.encodeToString(JsonObject.serializer(), root).toByteArray(Charsets.UTF_8)
    FileOutputStream(tempFile).use { output ->
      output.write(bytes)
      output.flush()
      output.fd.sync()
    }
    if (!tempFile.renameTo(dataFile)) {
      tempFile.delete()
      throw IOException("Failed to replace the biometric credential store.")
    }
    syncDirectory()
  }

  private fun syncDirectory() {
    runCatching {
      val fd = Os.open(directory.path, OsConstants.O_RDONLY, 0)
      try {
        Os.fsync(fd)
      } finally {
        Os.close(fd)
      }
    }
  }

  private fun ensureDirectory() {
    if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
      throw IOException("Failed to create the biometric credential store directory.")
    }
  }

  private fun <T> withExclusiveLock(block: () -> T): T {
    if (!processGuard.tryLock(lockTimeoutMillis, TimeUnit.MILLISECONDS)) {
      throw IOException("Timed out waiting for the biometric credential store lock.")
    }
    try {
      val fileLock = acquireFileLock()
      try {
        return block()
      } finally {
        fileLock.release()
      }
    } finally {
      processGuard.unlock()
    }
  }

  /**
   * Another SDK in this process holding the lock surfaces as [OverlappingFileLockException] rather
   * than blocking, and another process as a null [FileChannel.tryLock], so both are retried.
   */
  private fun acquireFileLock(): FileLock {
    val channel = lockChannel()
    val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(lockTimeoutMillis)
    var backoffMillis = INITIAL_LOCK_BACKOFF_MILLIS
    while (true) {
      val lock =
        try {
          channel.tryLock()
        } catch (_: OverlappingFileLockException) {
          null
        }
      if (lock != null) return lock
      if (System.nanoTime() >= deadline) {
        throw IOException("Timed out waiting for the biometric credential store lock.")
      }
      Thread.sleep(backoffMillis)
      backoffMillis = (backoffMillis * 2).coerceAtMost(MAX_LOCK_BACKOFF_MILLIS)
    }
  }

  // Closing any channel on the lock file can drop this process's POSIX lock, so it stays open.
  private fun lockChannel(): FileChannel =
    lockChannel
      ?: synchronized(this) {
        lockChannel
          ?: run {
            ensureDirectory()
            RandomAccessFile(lockFile, "rw").channel.also { lockChannel = it }
          }
      }

  private class StoreDocument(val root: JsonObject, val writable: Boolean = true) {

    fun credentialElements(): List<JsonElement> = (root[KEY_CREDENTIALS] as? JsonArray).orEmpty()

    fun records(): List<BiometricCredentialLocalRecord> =
      credentialElements().mapNotNull(BiometricCredentialRecordJson::decode)

    fun pendingCleanupUserIds(): Set<String> =
      (root[KEY_PENDING_CLEANUP_USER_IDS] as? JsonArray)
        .orEmpty()
        .mapNotNull { (it as? JsonPrimitive)?.takeIf { value -> value.isString }?.content }
        .filterTo(mutableSetOf()) { it.isNotBlank() }

    fun replacingCredential(credential: BiometricCredentialLocalRecord): StoreDocument {
      val elements = credentialElements()
      val existing = elements.firstOrNull { it.recordId == credential.id } as? JsonObject
      val encoded = BiometricCredentialRecordJson.encode(credential, preserving = existing)
      var replaced = false
      val updated = elements.mapNotNull { element ->
        when {
          element.recordId != credential.id -> element
          replaced -> null
          else -> encoded.also { replaced = true }
        }
      }
      return withCredentialElements(if (replaced) updated else updated + encoded)
    }

    fun withCredentialElements(elements: List<JsonElement>): StoreDocument =
      StoreDocument(JsonObject(root + (KEY_CREDENTIALS to JsonArray(elements))), writable)

    fun withPendingCleanupUserIds(userIds: Set<String>): StoreDocument =
      StoreDocument(
        JsonObject(
          root +
            (KEY_PENDING_CLEANUP_USER_IDS to
              JsonArray(userIds.filter { it.isNotBlank() }.sorted().map(::JsonPrimitive)))
        ),
        writable,
      )

    fun normalized(): StoreDocument {
      val known =
        mapOf(
          KEY_VERSION to JsonPrimitive(STORE_VERSION),
          KEY_CREDENTIALS to JsonArray(credentialElements()),
          KEY_PENDING_CLEANUP_USER_IDS to
            JsonArray(pendingCleanupUserIds().sorted().map(::JsonPrimitive)),
        )
      return StoreDocument(JsonObject(known + root.filterKeys { it !in known }), writable)
    }

    companion object {
      val EMPTY = StoreDocument(JsonObject(emptyMap()))
    }
  }

  companion object {
    const val DIRECTORY_NAME = "clerk"
    const val DATA_FILE_NAME = "biometric_credentials.v2.json"
    const val TEMP_FILE_NAME = "biometric_credentials.v2.json.tmp"
    const val LOCK_FILE_NAME = "biometric_credentials.lock"
    const val STORE_VERSION = 2

    const val KEY_VERSION = "version"
    const val KEY_CREDENTIALS = "credentials"
    const val KEY_PENDING_CLEANUP_USER_IDS = "pending_cleanup_user_ids"

    private const val LOCK_TIMEOUT_MILLIS = 5_000L
    private const val INITIAL_LOCK_BACKOFF_MILLIS = 2L
    private const val MAX_LOCK_BACKOFF_MILLIS = 50L

    private val PROCESS_GUARD = ReentrantLock()
    private val JSON = Json

    private val JsonElement.recordId: String?
      get() = ((this as? JsonObject)?.get("id") as? JsonPrimitive)?.takeIf { it.isString }?.content
  }
}

internal object BiometricCredentialRecordJson {
  const val ID = "id"
  const val LOCAL_KEY_ID = "local_key_id"
  const val USER_ID = "user_id"
  const val APP_IDENTIFIER = "app_identifier"
  const val IDENTIFIER_HINT_SHA256 = "identifier_hint_sha256"
  const val POLICY = "policy"
  const val CREATED_AT = "created_at"
  const val UPDATED_AT = "updated_at"

  private val policiesByWireValue =
    BiometricCredentialPolicy.entries.associateBy {
      Json.encodeToJsonElement(BiometricCredentialPolicy.serializer(), it).jsonPrimitive.content
    }

  private val wireValuesByPolicy = policiesByWireValue.entries.associate { it.value to it.key }

  fun policy(wireValue: String?): BiometricCredentialPolicy? =
    wireValue?.let(policiesByWireValue::get)

  @Suppress("ReturnCount")
  fun decode(element: JsonElement): BiometricCredentialLocalRecord? {
    val record = element as? JsonObject ?: return null
    val hintElement = record[IDENTIFIER_HINT_SHA256]
    val hint =
      if (hintElement == null || hintElement is JsonNull) null
      else record.string(IDENTIFIER_HINT_SHA256) ?: return null
    return BiometricCredentialLocalRecord(
      id = record.string(ID) ?: return null,
      localKeyId = record.string(LOCAL_KEY_ID) ?: return null,
      userId = record.string(USER_ID) ?: return null,
      appIdentifier = record.string(APP_IDENTIFIER) ?: return null,
      identifierHintSha256 = hint,
      policy = policy(record.string(POLICY)) ?: return null,
      createdAt = record.long(CREATED_AT) ?: return null,
      updatedAt = record.long(UPDATED_AT) ?: return null,
    )
  }

  fun encode(record: BiometricCredentialLocalRecord, preserving: JsonObject?): JsonObject {
    val known = buildMap {
      put(ID, JsonPrimitive(record.id))
      put(LOCAL_KEY_ID, JsonPrimitive(record.localKeyId))
      put(USER_ID, JsonPrimitive(record.userId))
      put(APP_IDENTIFIER, JsonPrimitive(record.appIdentifier))
      record.identifierHintSha256?.let { put(IDENTIFIER_HINT_SHA256, JsonPrimitive(it)) }
      put(POLICY, JsonPrimitive(wireValuesByPolicy.getValue(record.policy)))
      put(CREATED_AT, JsonPrimitive(record.createdAt))
      put(UPDATED_AT, JsonPrimitive(record.updatedAt))
    }
    val unknown = preserving.orEmpty().filterKeys { it !in KNOWN_FIELDS }
    return JsonObject(known + unknown)
  }

  private val KNOWN_FIELDS =
    setOf(
      ID,
      LOCAL_KEY_ID,
      USER_ID,
      APP_IDENTIFIER,
      IDENTIFIER_HINT_SHA256,
      POLICY,
      CREATED_AT,
      UPDATED_AT,
    )

  private fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

  private fun JsonObject.long(key: String): Long? =
    (this[key] as? JsonPrimitive)?.takeUnless { it.isString }?.longOrNull
}
