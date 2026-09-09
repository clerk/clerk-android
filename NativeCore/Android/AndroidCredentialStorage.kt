package com.clerk.api

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

public interface CredentialStorage {
  public suspend fun read(): String?
  public suspend fun write(value: String)
  public suspend fun remove()
}

internal fun instanceHash(value: String): String = MessageDigest.getInstance("SHA-256")
  .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

/** The optional legacy key authorizes migration of older, unscoped device credentials. */
public class AndroidCredentialStorage(
  context: Context,
  private val publishableKey: String,
  private val legacyPublishableKey: String? = null,
  private val purpose: Purpose = Purpose.CLIENT,
) : CredentialStorage {
  public enum class Purpose(internal val suffix: String) { CLIENT("credential"), MAGIC_LINK("magicLink") }
  private val application = context.applicationContext
  private val hash = instanceHash(publishableKey)
  private val alias = "${application.packageName}.clerk.core.v2.$hash"
  private val file = AtomicFile(File(application.noBackupFilesDir, "clerk-core/$hash.${purpose.suffix}"))
  private val mutex = Mutex()

  override suspend fun read(): String? = withContext(Dispatchers.IO) {
    mutex.withLock {
      if (file.baseFile.exists()) {
        val plaintext = decrypt(file.openRead().use { it.readBytes().toString(Charsets.UTF_8) }, alias)
        val record = Json.parseToJsonElement(plaintext).jsonObject
        if (record["schemaVersion"] != JsonPrimitive(1)) throw CoreException("unsupported_credential_record")
        return@withLock record["credential"]?.takeUnless { it == JsonNull }?.requireString()
      }
      val legacy = migrateLegacy()
      save(legacy)
      legacy
    }
  }

  override suspend fun write(value: String): Unit = withContext(Dispatchers.IO) {
    mutex.withLock { save(value) }
  }

  override suspend fun remove(): Unit = withContext(Dispatchers.IO) {
    // A durable empty record prevents a restart from reimporting a legacy credential.
    mutex.withLock { save(null) }
  }

  private fun save(value: String?) {
    file.baseFile.parentFile?.let { if (!it.exists() && !it.mkdirs()) throw CoreException("secure_storage_write_failed") }
    val record = buildJsonObject { put("schemaVersion", 1); put("credential", value?.let(::JsonPrimitive) ?: JsonNull) }
    val encrypted = encrypt(record.toString(), key(alias, create = true)).toByteArray(Charsets.UTF_8)
    val stream = file.startWrite()
    try { stream.write(encrypted); file.finishWrite(stream) }
    catch (error: Exception) { file.failWrite(stream); throw CoreException("secure_storage_write_failed") }
  }

  private fun migrateLegacy(): String? {
    val preferences = application.getSharedPreferences("clerk_preferences", Context.MODE_PRIVATE)
    fun read(name: String): String? {
      val stored = preferences.getString(name, null) ?: return null
      return if (stored.startsWith("clerk:v1:")) decrypt(stored.removePrefix("clerk:v1:"), "clerk_preferences.master_key") else stored
    }
    if (purpose == Purpose.MAGIC_LINK) {
      val cached = read("CACHED_CLERK_STATE")?.let { Json.parseToJsonElement(it).jsonObject }
      if (cached != null && cached["publishable_key"] != JsonPrimitive(publishableKey)) return null
      if (cached == null && legacyPublishableKey != publishableKey) return null
      return read("PENDING_NATIVE_MAGIC_LINK_FLOW")
    }
    val snapshot = read("SHARED_SESSION_SYNC_SNAPSHOT")
    if (snapshot != null) {
      val value = Json.parseToJsonElement(snapshot).jsonObject
      if (value["schemaVersion"] != JsonPrimitive(1) || value["instanceId"] != JsonPrimitive(hash)) return null
      val auth = value["auth"]?.takeUnless { it == JsonNull }?.jsonObject
      val device = value["deviceToken"]?.takeUnless { it == JsonNull }?.jsonObject
      if (auth?.get("state") == JsonPrimitive("cleared") || device?.get("state") == JsonPrimitive("cleared")) return null
      if (device?.get("state") == JsonPrimitive("set")) return device["value"]?.takeUnless { it == JsonNull }?.requireString()
      return null
    }
    val cached = read("CACHED_CLERK_STATE")?.let { Json.parseToJsonElement(it).jsonObject }
    if (cached != null && cached["publishable_key"] != JsonPrimitive(publishableKey)) return null
    if (cached == null && legacyPublishableKey != publishableKey) return null
    return read("DEVICE_TOKEN")?.takeIf { it.isNotEmpty() }
  }

  private fun key(name: String, create: Boolean): SecretKey {
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (store.getKey(name, null) as? SecretKey)?.let { return it }
    if (!create) throw CoreException("legacy_credential_key_unavailable")
    return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
      init(KeyGenParameterSpec.Builder(name, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(true).setUserAuthenticationRequired(false).build())
      generateKey()
    }
  }

  private fun encrypt(value: String, key: SecretKey): String {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key)
    return Base64.getEncoder().encodeToString(cipher.iv) + ":" + Base64.getEncoder().encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)))
  }

  private fun decrypt(value: String, alias: String): String {
    val parts = value.split(':', limit = 2)
    if (parts.size != 2) throw CoreException("invalid_credential_record")
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, key(alias, create = false), GCMParameterSpec(128, Base64.getDecoder().decode(parts[0])))
    return cipher.doFinal(Base64.getDecoder().decode(parts[1])).toString(Charsets.UTF_8)
  }
}
