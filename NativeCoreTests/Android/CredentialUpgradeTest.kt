package com.clerk.api

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.security.KeyStore
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CredentialUpgradeTest {
  private val context = InstrumentationRegistry.getInstrumentation().targetContext
  private val preferences get() = context.getSharedPreferences("clerk_preferences", Context.MODE_PRIVATE)
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

  @Test fun scopedClearNeverFallsBackToLegacyToken() = runBlocking {
    val key = "fixture-" + UUID.randomUUID()
    val snapshot = buildJsonObject {
      put("schemaVersion", 1); put("instanceId", instanceHash(key))
      put("deviceToken", buildJsonObject { put("state", "cleared") })
      put("auth", buildJsonObject { put("state", "cleared") })
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
