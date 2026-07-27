package com.clerk.api.network.middleware.outgoing

import com.clerk.api.network.middleware.SensitiveRequest
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestLoggingMiddlewareTest {
  @Test
  fun `sensitive request bypasses body logging`() {
    val logs = mutableListOf<String>()
    val loggingInterceptor =
      HttpLoggingInterceptor { message -> logs += message }
        .apply { level = HttpLoggingInterceptor.Level.BODY }
    val client =
      OkHttpClient.Builder()
        .addInterceptor(RequestLoggingMiddleware(loggingInterceptor))
        .addInterceptor { chain ->
          Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("{}".toResponseBody())
            .build()
        }
        .build()
    val request =
      Request.Builder()
        .url("https://example.com/v1/client")
        .post(FormBody.Builder().add("code_verifier", "secret_verifier").build())
        .tag(SensitiveRequest::class.java, SensitiveRequest)
        .build()

    client.newCall(request).execute().close()

    assertTrue(logs.isEmpty())
  }

  @Test
  fun `ordinary request uses body logging`() {
    val logs = mutableListOf<String>()
    val loggingInterceptor =
      HttpLoggingInterceptor { message -> logs += message }
        .apply { level = HttpLoggingInterceptor.Level.BODY }
    val client =
      OkHttpClient.Builder()
        .addInterceptor(RequestLoggingMiddleware(loggingInterceptor))
        .addInterceptor { chain ->
          Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("{}".toResponseBody())
            .build()
        }
        .build()
    val request =
      Request.Builder()
        .url("https://example.com/v1/client")
        .post(FormBody.Builder().add("ordinary_field", "ordinary_value").build())
        .build()

    client.newCall(request).execute().close()

    assertTrue(logs.isNotEmpty())
  }
}
