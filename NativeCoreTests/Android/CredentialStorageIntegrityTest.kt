package com.clerk.api

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.util.AtomicFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.security.KeyStore
import java.util.Base64
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CredentialStorageIntegrityTest {
  private suspend fun withStorage(
    block: suspend (Context, String, SharedPreferences, File, String) -> Unit
  ) {
    val base = InstrumentationRegistry.getInstrumentation().targetContext
    val fixtureId = UUID.randomUUID().toString()
    val preferenceName = "credential-integrity-$fixtureId"
    val context =
      object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this

        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
          super.getSharedPreferences(
            if (name == "clerk_preferences") preferenceName else name,
            mode,
          )
      }
    val key = "fixture-integrity-$fixtureId"
    val preferences = context.getSharedPreferences("clerk_preferences", Context.MODE_PRIVATE)
    val hash = instanceHash(key)
    val file = File(context.noBackupFilesDir, "clerk-core/$hash.credential")
    val alias = "${context.packageName}.clerk.core.v2.$hash"
    try {
      block(context, key, preferences, file, alias)
    } finally {
      AtomicFile(file).delete()
      KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
        deleteEntry(alias)
      }
      base.deleteSharedPreferences(preferenceName)
    }
  }

  @Test
  fun plaintextLegacyImportUpdateAndClearPreserveTheOriginalPreferences() = runBlocking {
    withStorage { context, key, preferences, file, _ ->
      val original =
        mapOf(
          "CACHED_CLERK_STATE" to buildJsonObject { put("publishable_key", key) }.toString(),
          "DEVICE_TOKEN" to "legacy-credential-é-🔑",
          "DEVICE_ID" to "unrelated-old-installation-id",
        )
      assertTrue(
        preferences
          .edit()
          .apply { original.forEach { (name, value) -> putString(name, value) } }
          .commit()
      )
      assertEquals(original.getValue("DEVICE_TOKEN"), AndroidCredentialStorage(context, key).read())
      assertTrue(file.exists())
      assertFalse(file.readText().contains(original.getValue("DEVICE_TOKEN")))
      assertEquals(original.getValue("DEVICE_TOKEN"), AndroidCredentialStorage(context, key).read())
      AndroidCredentialStorage(context, key).write("replacement-credential-🔐")
      assertEquals("replacement-credential-🔐", AndroidCredentialStorage(context, key).read())
      AndroidCredentialStorage(context, key).remove()
      assertTrue(file.exists())
      assertNull(AndroidCredentialStorage(context, key).read())
      assertEquals(original, preferences.all)
    }
  }

  @Test
  fun atomicBackupPreservesTheLastCommittedCredential() = verifyAtomicBackup("current-credential")

  @Test fun atomicBackupPreservesTheLastCommittedClearMarker() = verifyAtomicBackup(null)

  private fun verifyAtomicBackup(credential: String?) = runBlocking {
    withStorage { context, key, preferences, file, _ ->
      assertTrue(preferences.edit().putString("DEVICE_TOKEN", "must-not-reimport").commit())
      val storage = AndroidCredentialStorage(context, key, key)
      if (credential == null) storage.remove() else storage.write(credential)
      val committed = file.readText()
      val backup = File(file.path + ".bak")
      assertTrue(file.renameTo(backup))
      assertFalse(file.exists())
      assertEquals(credential, AndroidCredentialStorage(context, key, key).read())
      assertEquals(committed, file.readText())
      assertFalse(backup.exists())
      assertEquals(credential, AndroidCredentialStorage(context, key, key).read())
      assertEquals("must-not-reimport", preferences.getString("DEVICE_TOKEN", null))
    }
  }

  @Test
  fun corruptedCiphertextDoesNotClearOrReimportTheCredential() = runBlocking {
    withStorage { context, key, preferences, file, _ ->
      assertTrue(preferences.edit().putString("DEVICE_TOKEN", "must-not-reimport").commit())
      AndroidCredentialStorage(context, key, key).write("current-credential")
      val original = file.readText()
      val (iv, encoded) = original.split(':', limit = 2)
      val ciphertext = Base64.getDecoder().decode(encoded)
      ciphertext[ciphertext.lastIndex] = (ciphertext.last().toInt() xor 1).toByte()
      val corrupted = iv + ":" + Base64.getEncoder().encodeToString(ciphertext)
      file.writeText(corrupted)
      repeat(2) {
        assertNotNull(
          runCatching { AndroidCredentialStorage(context, key, key).read() }.exceptionOrNull()
        )
        assertEquals(corrupted, file.readText())
        assertEquals("must-not-reimport", preferences.getString("DEVICE_TOKEN", null))
      }
      file.writeText(original)
      assertEquals("current-credential", AndroidCredentialStorage(context, key, key).read())
    }
  }

  @Test
  fun missingKeystoreKeyDoesNotReplaceTheEncryptedRecord() = runBlocking {
    withStorage { context, key, preferences, file, alias ->
      assertTrue(preferences.edit().putString("DEVICE_TOKEN", "must-not-reimport").commit())
      AndroidCredentialStorage(context, key, key).write("current-credential")
      val original = file.readText()
      val keys =
        KeyStore.getInstance("AndroidKeyStore").apply {
          load(null)
          deleteEntry(alias)
        }
      repeat(2) {
        val error =
          runCatching { AndroidCredentialStorage(context, key, key).read() }.exceptionOrNull()
        assertTrue(error is CoreException)
        assertEquals("legacy_credential_key_unavailable", (error as CoreException).code)
        assertFalse(keys.containsAlias(alias))
        assertEquals(original, file.readText())
        assertEquals("must-not-reimport", preferences.getString("DEVICE_TOKEN", null))
      }
    }
  }

  @Test
  fun malformedLegacyEnvelopeRemainsIntactWithoutCreatingANewRecord() = runBlocking {
    withStorage { context, key, preferences, file, alias ->
      val malformed = "clerk:v1:malformed"
      assertTrue(preferences.edit().putString("DEVICE_TOKEN", malformed).commit())
      repeat(2) {
        val error =
          runCatching { AndroidCredentialStorage(context, key, key).read() }.exceptionOrNull()
        assertTrue(error is CoreException)
        assertEquals("invalid_credential_record", (error as CoreException).code)
        assertEquals(malformed, preferences.getString("DEVICE_TOKEN", null))
        assertFalse(file.exists())
        assertFalse(
          KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.containsAlias(alias)
        )
      }
    }
  }
}
