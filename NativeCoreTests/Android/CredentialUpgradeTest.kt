package com.clerk.api

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.security.KeyStore
import java.io.File
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CredentialUpgradeTest {
  private val context = InstrumentationRegistry.getInstrumentation().targetContext
  private val preferences get() = context.getSharedPreferences("clerk_preferences", Context.MODE_PRIVATE)
  private fun cleanup(key: String) {
    val hash = instanceHash(key)
    for (purpose in AndroidCredentialStorage.Purpose.entries) {
      AtomicFile(File(context.noBackupFilesDir, "clerk-core/$hash.${purpose.suffix}")).delete()
    }
    KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("${context.packageName}.clerk.core.v2.$hash") }
  }

  @OptIn(ExperimentalSerializationApi::class)
  @Test fun legacyBiometricMetadataReachesTheCoreWithItsOriginalPolicy() = runBlocking {
    // Serializer shape and configuration from clerk-android 1ea9f972.
    val legacyJson = Json {
      isLenient = true; ignoreUnknownKeys = true; coerceInputValues = true
      explicitNulls = true; namingStrategy = JsonNamingStrategy.SnakeCase
    }
    for (policy in listOf("biometry_or_device_passcode", "biometry_current_set")) {
      val key = "pk_test_" + Base64.getEncoder().encodeToString((UUID.randomUUID().toString() + ".clerk.accounts.dev$").toByteArray())
      try {
        val encoded = legacyJson.encodeToJsonElement(LegacyBiometricRecord.serializer(), LegacyBiometricRecord(id = "td_legacy", localKeyId = "tdlk_legacy", userId = "user_native", appIdentifier = "com.example.native", policy = policy, createdAt = 1700000000000, updatedAt = 1700000000001))
        check(encoded.jsonObject["local_key_id"] == JsonPrimitive("tdlk_legacy"))
        check(encoded.jsonObject.containsKey("policy") == (policy != "biometry_or_device_passcode"))
        val original = JsonArray(listOf(encoded)).toString()
        preferences.edit().clear().putString("CACHED_CLERK_STATE", encrypted(buildJsonObject { put("publishable_key", key) }.toString()))
          .putString("TRUSTED_DEVICE_CREDENTIALS", encrypted(original)).commit()
        val metadata = AndroidCredentialStorage(context, key, purpose = AndroidCredentialStorage.Purpose.BIOMETRIC_CREDENTIALS)
        check(metadata.read() == original)
        withContext(Dispatchers.Main.immediate) {
          val base = PackagedFixtures(InstrumentationRegistry.getInstrumentation().context)
          val checkedPolicies = mutableListOf<String>()
          val deleted = mutableListOf<String>()
          val capabilities = object : NativeCapabilities {
            override val supported = base.supported
            override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
              val args = arguments.jsonObject
              if (capability == "biometrics.storage.read" && args["key"] == JsonPrimitive("credentials"))
                return metadata.read()?.let(::JsonPrimitive) ?: JsonNull
              if (capability == "biometrics.storage.write" && args["key"] == JsonPrimitive("credentials")) {
                metadata.write(args.getValue("value").requireString()); return JsonNull
              }
              if (capability == "biometrics.supports") checkedPolicies += args.getValue("policy").requireString()
              if (capability == "biometrics.hasKey") check(args["localKeyId"] == JsonPrimitive("tdlk_legacy"))
              if (capability == "biometrics.deleteKey") deleted += args.getValue("localKeyId").requireString()
              return base.perform(capability, arguments)
            }
          }
          val clerk = Clerk.connect(context, ClerkConfiguration(key, "clerk-test://sso-callback"), capabilities)
          try {
            check(clerk.biometricCredentials.localAvailability().isAvailable)
            check(checkedPolicies == listOf(policy))
            check(clerk.biometricCredentials.forgetLocalCredentials(BiometricCredentialsForgetLocalCredentialsParams(userId = "user_native")) == 1.0)
            check(deleted == listOf("tdlk_legacy"))
          } finally { clerk.close() }
        }
        check(AndroidCredentialStorage(context, key, purpose = AndroidCredentialStorage.Purpose.BIOMETRIC_CREDENTIALS).read() == "[]")
        check(preferences.getString("TRUSTED_DEVICE_CREDENTIALS", null) != null)
      } finally { cleanup(key); preferences.edit().clear().commit() }
    }
    Unit
  }

  @Test fun concurrentFirstWritesPreserveEveryCredentialPurpose() = runBlocking {
    repeat(6) {
      val key = "fixture-concurrent-" + UUID.randomUUID()
      try {
        coroutineScope {
          val start = CompletableDeferred<Unit>()
          val writes = AndroidCredentialStorage.Purpose.entries.map { purpose ->
            async {
              start.await()
              AndroidCredentialStorage(context, key, purpose = purpose).write("fixture-${purpose.name}")
            }
          }
          start.complete(Unit)
          writes.awaitAll()
        }
        for (purpose in AndroidCredentialStorage.Purpose.entries) {
          check(AndroidCredentialStorage(context, key, purpose = purpose).read() == "fixture-${purpose.name}")
        }
      } finally { cleanup(key) }
    }
    Unit
  }

  @Test fun independentStorageObjectsSerializeWritesToTheSameAtomicFile() = runBlocking {
    val key = "fixture-shared-file-" + UUID.randomUUID()
    try {
      AndroidCredentialStorage(context, key).write("initial")
      repeat(4) {
        val values = (1..12).map { "fixture-value-$it" }
        coroutineScope {
          val start = CompletableDeferred<Unit>()
          val writes = values.map { value -> async {
            start.await()
            AndroidCredentialStorage(context, key).write(value)
          } }
          start.complete(Unit)
          writes.awaitAll()
        }
        check(AndroidCredentialStorage(context, key).read() in values)
      }
      AndroidCredentialStorage(context, key).remove()
      check(AndroidCredentialStorage(context, key).read() == null)
    } finally { cleanup(key) }
    Unit
  }

  @Test fun scopedSnapshotUsesThePreviousMajorsSnakeCaseAndOmittedSchemaDefault() = runBlocking {
    for (includeSchema in listOf(false, true)) {
      val key = "fixture-snapshot-" + UUID.randomUUID()
      try {
        val snapshot = buildJsonObject {
          if (includeSchema) put("schema_version", 1)
          put("instance_id", instanceHash(key))
          put("device_token", buildJsonObject {
            put("state", "set"); put("version", "fixture-v1"); put("changed_at_millis", 1700000000000)
            put("value", "snapshot-credential")
          })
        }
        preferences.edit().clear().putString("SHARED_SESSION_SYNC_SNAPSHOT", encrypted(snapshot.toString()))
          .putString("DEVICE_TOKEN", encrypted("older-credential")).commit()
        check(AndroidCredentialStorage(context, key).read() == "snapshot-credential")
        check(AndroidCredentialStorage(context, key).read() == "snapshot-credential")
      } finally { cleanup(key); preferences.edit().clear().commit() }
    }
    Unit
  }
  private fun encrypted(value: String): String {
    val alias = "clerk_preferences.master_key"
    val keys = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    val key = (keys.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance("AES", "AndroidKeyStore").run {
      init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
      generateKey()
    }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
    return "clerk:v1:" + Base64.getEncoder().encodeToString(cipher.iv) + ":" + Base64.getEncoder().encodeToString(cipher.doFinal(value.toByteArray()))
  }

  @Test fun encryptedLegacyCredentialMigratesAndTombstoneSurvivesReopen() = runBlocking {
    val key = "fixture-" + UUID.randomUUID()
    preferences.edit().clear().putString("CACHED_CLERK_STATE", encrypted(buildJsonObject { put("publishable_key", key) }.toString()))
      .putString("DEVICE_TOKEN", encrypted("upgrade-fixture-credential")).commit()
    check(AndroidCredentialStorage(context, key).read() == "upgrade-fixture-credential")
    check(AndroidCredentialStorage(context, key).read() == "upgrade-fixture-credential")
    AndroidCredentialStorage(context, key).remove()
    check(AndroidCredentialStorage(context, key).read() == null)
    check(AndroidCredentialStorage(context, "$key-other").read() == null)
    preferences.edit().clear().commit()
    Unit
  }

  @Test fun magicLinkUsesSeparateEncryptedRecordAndPreservesClientOnClear() = runBlocking {
    val key = "fixture-" + UUID.randomUUID()
    val record = "{\"codeVerifier\":\"fixture-pkce\"}"
    preferences.edit().clear().putString("CACHED_CLERK_STATE", encrypted(buildJsonObject { put("publishable_key", key) }.toString()))
      .putString("DEVICE_TOKEN", encrypted("client-fixture"))
      .putString("PENDING_NATIVE_MAGIC_LINK_FLOW", encrypted(record)).commit()
    val client = AndroidCredentialStorage(context, key)
    val magic = AndroidCredentialStorage(context, key, purpose = AndroidCredentialStorage.Purpose.MAGIC_LINK)
    check(client.read() == "client-fixture")
    check(magic.read() == record)
    check(AndroidCredentialStorage(context, key, purpose = AndroidCredentialStorage.Purpose.MAGIC_LINK).read() == record)
    check(AndroidCredentialStorage(context, "$key-other", purpose = AndroidCredentialStorage.Purpose.MAGIC_LINK).read() == null)
    magic.remove()
    check(AndroidCredentialStorage(context, key, purpose = AndroidCredentialStorage.Purpose.MAGIC_LINK).read() == null)
    check(client.read() == "client-fixture")
    preferences.edit().clear().commit()
    Unit
  }

  @Test fun biometricMetadataAndPendingCleanupMigrateSeparatelyFromTheClient() = runBlocking {
    val key = "fixture-" + UUID.randomUUID()
    preferences.edit().clear().putString("CACHED_CLERK_STATE", encrypted(buildJsonObject { put("publishable_key", key) }.toString()))
      .putString("DEVICE_TOKEN", encrypted("client-fixture"))
      .putString("TRUSTED_DEVICE_CREDENTIALS", encrypted("fixture-biometric-metadata"))
      .putString("PENDING_TRUSTED_DEVICE_CREDENTIAL_CLEANUP", encrypted("[\"deleted-user\"]")).commit()
    val biometric = AndroidCredentialStorage(context, key, purpose = AndroidCredentialStorage.Purpose.BIOMETRIC_CREDENTIALS)
    val cleanup = AndroidCredentialStorage(context, key, purpose = AndroidCredentialStorage.Purpose.BIOMETRIC_CLEANUP)
    check(biometric.read() == "fixture-biometric-metadata")
    check(cleanup.read() == "[\"deleted-user\"]")
    check(AndroidCredentialStorage(context, key).read() == "client-fixture")
    biometric.remove()
    check(AndroidCredentialStorage(context, key, purpose = AndroidCredentialStorage.Purpose.BIOMETRIC_CREDENTIALS).read() == null)
    check(AndroidCredentialStorage(context, "$key-other", purpose = AndroidCredentialStorage.Purpose.BIOMETRIC_CREDENTIALS).read() == null)
    preferences.edit().clear().commit()
    Unit
  }

  @Test fun scopedClearNeverFallsBackToLegacyToken() = runBlocking {
    val key = "fixture-" + UUID.randomUUID()
    val snapshot = buildJsonObject {
      put("instance_id", instanceHash(key))
      put("device_token", buildJsonObject { put("state", "cleared"); put("version", "device-clear"); put("changed_at_millis", 1700000000000) })
      put("auth", buildJsonObject { put("state", "cleared"); put("version", "auth-clear") })
    }
    preferences.edit().clear().putString("SHARED_SESSION_SYNC_SNAPSHOT", encrypted(snapshot.toString()))
      .putString("DEVICE_TOKEN", encrypted("must-not-restore")).commit()
    check(AndroidCredentialStorage(context, key, key).read() == null)
    preferences.edit().remove("SHARED_SESSION_SYNC_SNAPSHOT").commit()
    check(AndroidCredentialStorage(context, key, key).read() == null)
    preferences.edit().clear().commit()
    Unit
  }
}

@Serializable
private data class LegacyBiometricRecord(
  val id: String,
  @SerialName("localKeyId") val localKeyId: String,
  @SerialName("userId") val userId: String,
  @SerialName("appIdentifier") val appIdentifier: String,
  @SerialName("identifierHint") val identifierHint: String? = null,
  val policy: String = "biometry_or_device_passcode",
  @SerialName("createdAt") val createdAt: Long,
  @SerialName("updatedAt") val updatedAt: Long,
)
