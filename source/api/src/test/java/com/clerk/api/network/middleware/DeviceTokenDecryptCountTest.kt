package com.clerk.api.network.middleware

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import com.clerk.api.Clerk
import com.clerk.api.Constants.Http.AUTHORIZATION_HEADER
import com.clerk.api.Constants.Storage.CLERK_PREFERENCES_FILE_NAME
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.middleware.incoming.ClientSyncingMiddleware
import com.clerk.api.network.middleware.incoming.DeviceTokenSavingMiddleware
import com.clerk.api.network.middleware.outgoing.VersioningUserAgentMiddleware
import com.clerk.api.storage.StorageCipher
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import java.util.concurrent.ConcurrentLinkedQueue
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Pins how often a request decrypts the device token across the real interceptor chain. */
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
    // Persisted by an earlier process, so nothing is cached in memory yet.
    context.getSharedPreferences(CLERK_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE).edit(
      commit = true
    ) {
      putString(StorageKey.DEVICE_TOKEN.name, "clerk:v1:${encode("token_1")}")
    }
    val client = client(responseToken = { requestToken -> requestToken })

    repeat(3) { assertEquals("token_1", client.execute()) }

    assertEquals(1, tokenDecrypts("token_1"))
  }

  @Test
  fun `rotated, cleared, and reset tokens are served without stale reads`() {
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "token_1")
    val client = client(responseToken = { "token_2" })

    assertEquals("token_1", client.execute())
    assertEquals("token_2", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))

    StorageHelper.deleteValue(StorageKey.DEVICE_TOKEN)
    assertEquals(null, StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))

    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "token_3")
    StorageHelper.reset(context)
    assertEquals(null, StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))

    // Every value above came from the in-memory copy kept in step with each write.
    assertEquals(0, decryptedValues.count { it.startsWith("token_") })
  }

  @Test
  fun `compareAndSetDeviceToken replaces the cached token`() {
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "token_1")
    assertEquals("token_1", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))

    // The path Clerk.setDeviceToken() and DeviceTokenSavingMiddleware write through.
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
    context.getSharedPreferences(CLERK_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE).edit(
      commit = true
    ) {
      putString(StorageKey.DEVICE_TOKEN.name, "clerk:v1:${encode("token_1")}")
    }
    var cipherAvailable = false
    StorageHelper.storageCipherFactoryOverride = {
      check(cipherAvailable) { "keystore not ready" }
      CountingCipher(decryptedValues)
    }

    // Preferences are open but the cipher failed to initialize, so the token can't be read yet.
    StorageHelper.initialize(context)
    assertEquals(null, StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))

    cipherAvailable = true
    StorageHelper.initialize(context)
    assertEquals("token_1", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
  }

  private fun tokenDecrypts(token: String): Int = decryptedValues.count { it == token }

  private fun client(responseToken: (requestToken: String?) -> String?): OkHttpClient =
    OkHttpClient.Builder()
      .addInterceptor(ClientSyncingMiddleware(json = ClerkApi.json))
      .addInterceptor(VersioningUserAgentMiddleware())
      .addInterceptor(DeviceTokenSavingMiddleware())
      .addInterceptor(TerminalInterceptor(responseToken))
      .build()

  /** Returns the Authorization header the request was sent with. */
  private fun OkHttpClient.execute(): String? =
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
