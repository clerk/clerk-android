package com.clerk.api.network.middleware

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import com.clerk.api.Clerk
import com.clerk.api.Constants.Http.AUTHORIZATION_HEADER
import com.clerk.api.Constants.Storage.CLERK_PREFERENCES_FILE_NAME
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.middleware.incoming.ClientSyncingMiddleware
import com.clerk.api.network.middleware.incoming.DeviceTokenSavingMiddleware
import com.clerk.api.network.middleware.outgoing.VersioningUserAgentMiddleware
import com.clerk.api.storage.StorageCipher
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import com.clerk.api.storage.failCommitsForTesting
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DeviceTokenDecryptCountTest {
  private lateinit var context: Context
  private val decryptedValues = ConcurrentLinkedQueue<String>()

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    Clerk.reset()
    StorageHelper.storageCipherFactoryOverride = { CountingCipher(decryptedValues) }
    StorageHelper.resetToUninitializedForTesting()
    StorageHelper.initialize(context)
  }

  @After
  fun tearDown() {
    StorageHelper.storageCipherFactoryOverride = null
    StorageHelper.reset(context)
    Clerk.reset()
  }

  @Test
  fun `requests decrypt a persisted device token at most once`() {
    persistTokenFromEarlierProcess("clerk:v1:${encode("token_1")}")
    val client = client(responseToken = { requestToken -> requestToken })

    repeat(3) { assertEquals("token_1", client.sendReturningAuthorizationHeader()) }

    assertEquals(1, tokenDecrypts("token_1"))
  }

  @Test
  fun `rotated, cleared, and reset tokens are served without stale reads`() {
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "token_1")
    val client = client(responseToken = { "token_2" })

    assertEquals("token_1", client.sendReturningAuthorizationHeader())
    assertEquals("token_2", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))

    StorageHelper.deleteValue(StorageKey.DEVICE_TOKEN)
    assertEquals(null, StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))

    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "token_3")
    StorageHelper.reset(context)
    assertEquals(null, StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))

    assertEquals(0, decryptedValues.count { it.startsWith("token_") })
  }

  @Test
  fun `compareAndSetDeviceToken replaces the cached token`() {
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "token_1")
    assertEquals("token_1", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))

    assertTrue(StorageHelper.compareAndSetDeviceToken(expected = "token_1", value = "token_2"))
    assertEquals("token_2", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))

    assertTrue(StorageHelper.compareAndSetDeviceToken(expected = "token_2", value = null))
    assertEquals(null, StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))

    assertFalse(StorageHelper.compareAndSetDeviceToken(expected = "token_2", value = "token_3"))
    assertEquals(null, StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }

  @Test
  fun `a read before the cipher is ready is not cached`() {
    StorageHelper.resetToUninitializedForTesting()
    persistTokenFromEarlierProcess("clerk:v1:${encode("token_1")}")
    var cipherAvailable = false
    StorageHelper.storageCipherFactoryOverride = {
      check(cipherAvailable) { "keystore not ready" }
      CountingCipher(decryptedValues)
    }

    StorageHelper.initialize(context)
    assertEquals(null, StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))

    cipherAvailable = true
    StorageHelper.initialize(context)
    assertEquals("token_1", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }

  @Test
  fun `a read that started before the cipher was ready is not cached after initialize finishes`() {
    StorageHelper.resetToUninitializedForTesting()
    persistTokenFromEarlierProcess("clerk:v1:${encode("token_1")}")
    val cipherAvailable = AtomicBoolean(false)
    StorageHelper.storageCipherFactoryOverride = {
      check(cipherAvailable.get()) { "keystore not ready" }
      CountingCipher(decryptedValues)
    }
    StorageHelper.initialize(context)
    val readSawNoCipher = CountDownLatch(1)
    val releaseRead = CountDownLatch(1)
    mockkObject(ClerkLog)
    try {
      every { ClerkLog.w(any()) } answers
        {
          if (firstArg<String>().startsWith("Encrypted storage is unavailable, returning null")) {
            readSawNoCipher.countDown()
            releaseRead.await(5, TimeUnit.SECONDS)
          }
          0
        }
      val readResult = AtomicReference<String?>("unset")
      val reader = Thread {
        readResult.set(StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
      }
        .apply { start() }
      assertTrue(readSawNoCipher.await(5, TimeUnit.SECONDS))

      cipherAvailable.set(true)
      StorageHelper.initialize(context)
      releaseRead.countDown()
      reader.join(5_000)

      assertNull(readResult.get())
      assertEquals("token_1", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
    } finally {
      releaseRead.countDown()
      unmockkObject(ClerkLog)
    }
  }

  @Test
  fun `migrating a plaintext token keeps a token written while it was encrypting`() {
    val firstEncryptStarted = CountDownLatch(1)
    val releaseFirstEncrypt = CountDownLatch(1)
    StorageHelper.storageCipherFactoryOverride = {
      object : StorageCipher {
        private val legacyEncryptCalls = AtomicInteger()

        override fun encrypt(plaintext: String): String {
          if (plaintext == "token_1" && legacyEncryptCalls.getAndIncrement() == 0) {
            firstEncryptStarted.countDown()
            releaseFirstEncrypt.await(5, TimeUnit.SECONDS)
          }
          return encode(plaintext)
        }

        override fun decrypt(ciphertext: String): String =
          CountingCipher(decryptedValues).decrypt(ciphertext)
      }
    }
    StorageHelper.resetToUninitializedForTesting()
    StorageHelper.initialize(context)
    persistTokenFromEarlierProcess("token_1")
    val reader = Thread { StorageHelper.loadValue(StorageKey.DEVICE_TOKEN) }.apply { start() }
    assertTrue(firstEncryptStarted.await(5, TimeUnit.SECONDS))

    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "token_2")
    releaseFirstEncrypt.countDown()
    reader.join(5_000)

    assertEquals("clerk:v1:${encode("token_2")}", storedDeviceToken())
    assertEquals("token_2", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }

  @Test
  fun `a failed commit drops the cached token`() {
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "token_1")
    val restoreCommits = StorageHelper.failCommitsForTesting()

    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "token_2")
    restoreCommits()

    assertEquals("token_1", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
    assertEquals(1, tokenDecrypts("token_1"))
  }

  @Test
  fun `removing an undecryptable token keeps a token written while it was decrypting`() {
    val undecryptable = "clerk:v1:undecryptable"
    val firstDecryptStarted = CountDownLatch(1)
    val releaseFirstDecrypt = CountDownLatch(1)
    StorageHelper.storageCipherFactoryOverride = {
      object : StorageCipher {
        private val decryptCalls = AtomicInteger()

        override fun encrypt(plaintext: String): String = encode(plaintext)

        override fun decrypt(ciphertext: String): String {
          if (ciphertext != "undecryptable") {
            return CountingCipher(decryptedValues).decrypt(ciphertext)
          }
          if (decryptCalls.getAndIncrement() == 0) {
            firstDecryptStarted.countDown()
            releaseFirstDecrypt.await(5, TimeUnit.SECONDS)
          }
          error("bad ciphertext")
        }
      }
    }
    StorageHelper.resetToUninitializedForTesting()
    StorageHelper.initialize(context)
    persistTokenFromEarlierProcess(undecryptable)
    val reader = Thread { StorageHelper.loadValue(StorageKey.DEVICE_TOKEN) }.apply { start() }
    assertTrue(firstDecryptStarted.await(5, TimeUnit.SECONDS))

    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "token_2")
    releaseFirstDecrypt.countDown()
    reader.join(5_000)

    assertEquals("clerk:v1:${encode("token_2")}", storedDeviceToken())
    assertEquals("token_2", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }

  private fun preferences() =
    context.getSharedPreferences(CLERK_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

  private fun persistTokenFromEarlierProcess(storedValue: String) {
    preferences().edit(commit = true) { putString(StorageKey.DEVICE_TOKEN.name, storedValue) }
  }

  private fun storedDeviceToken(): String? =
    preferences().getString(StorageKey.DEVICE_TOKEN.name, null)

  private fun tokenDecrypts(token: String): Int = decryptedValues.count { it == token }

  private fun client(responseToken: (requestToken: String?) -> String?): OkHttpClient =
    OkHttpClient.Builder()
      .addInterceptor(ClientSyncingMiddleware(json = ClerkApi.json))
      .addInterceptor(VersioningUserAgentMiddleware())
      .addInterceptor(DeviceTokenSavingMiddleware())
      .addInterceptor(TerminalInterceptor(responseToken))
      .build()

  private fun OkHttpClient.sendReturningAuthorizationHeader(): String? =
    newCall(Request.Builder().url("https://example.com/v1/client").build()).execute().use {
      it.request.header(AUTHORIZATION_HEADER)
    }

  private class TerminalInterceptor(private val responseToken: (String?) -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      val request = chain.request()
      val builder =
        Response.Builder()
          .request(request)
          .protocol(Protocol.HTTP_1_1)
          .code(200)
          .message("OK")
          .body("ok".toResponseBody("text/plain".toMediaType()))
      responseToken(request.header(AUTHORIZATION_HEADER))?.let {
        builder.header(AUTHORIZATION_HEADER, it)
      }
      return builder.build()
    }
  }

  private class CountingCipher(private val decryptedValues: MutableCollection<String>) :
    StorageCipher {
    override fun encrypt(plaintext: String): String = encode(plaintext)

    override fun decrypt(ciphertext: String): String =
      String(Base64.decode(ciphertext, Base64.NO_WRAP), Charsets.UTF_8).also {
        decryptedValues.add(it)
      }
  }

  private companion object {
    fun encode(plaintext: String): String =
      Base64.encodeToString(plaintext.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
  }
}
