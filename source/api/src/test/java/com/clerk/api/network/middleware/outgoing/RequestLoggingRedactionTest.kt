package com.clerk.api.network.middleware.outgoing

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestLoggingRedactionTest {
  private val secrets =
    listOf(
      "device_token_secret",
      "rotated_device_token_secret",
      "hunter2",
      "old_hunter2",
      "new_hunter2",
      "424242",
      "ticket_secret",
      "eyJhbGciOiJSUzI1NiJ9.session.jwt",
      "JBSWY3DPEHPK3PXP",
      "backup-one",
      "backup-two",
    )

  private fun logFor(request: Request, responseBody: String): String {
    val logs = mutableListOf<String>()
    val client =
      OkHttpClient.Builder()
        .addInterceptor(RequestLoggingMiddleware.create { logs += it })
        .addInterceptor { chain ->
          Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .header("Authorization", "rotated_device_token_secret")
            .body(responseBody.toResponseBody())
            .build()
        }
        .build()
    client.newCall(request).execute().close()
    return logs.joinToString("\n")
  }

  @Test
  fun `debug logging never prints credentials, codes or tokens`() {
    val request =
      Request.Builder()
        .url("https://example.com/v1/client/sign_ins")
        .header("Authorization", "device_token_secret")
        .post(
          FormBody.Builder()
            .add("identifier", "user@example.com")
            .add("password", "hunter2")
            .add("current_password", "old_hunter2")
            .add("new_password", "new_hunter2")
            .add("code", "424242")
            .add("ticket", "ticket_secret")
            .build()
        )
        .build()
    val responseBody =
      """
      {"response":{"object":"token","jwt":"eyJhbGciOiJSUzI1NiJ9.session.jwt",
      "secret":"JBSWY3DPEHPK3PXP","uri":"otpauth://totp/Clerk?secret=JBSWY3DPEHPK3PXP",
      "backup_codes":["backup-one","backup-two"]},
      "errors":[{"code":"form_password_incorrect"}]}
      """
        .trimIndent()

    val log = logFor(request, responseBody)

    secrets.forEach { secret -> assertFalse("log leaked $secret:\n$log", log.contains(secret)) }
    assertTrue(
      "identifier should stay visible:\n$log",
      log.contains("identifier=user%40example.com"),
    )
    assertTrue("error codes should stay visible:\n$log", log.contains("form_password_incorrect"))
  }

  @Test
  fun `debug logging never prints a rotating token nonce in the url`() {
    val request =
      Request.Builder()
        .url(
          "https://example.com/v1/client/sign_ins/sia_1" +
            "?rotating_token_nonce=nonce_secret&_is_native=true"
        )
        .build()

    val log = logFor(request, """{"response":{"object":"sign_in_attempt","id":"sia_1"}}""")

    assertFalse("log leaked the nonce:\n$log", log.contains("nonce_secret"))
    assertTrue("other query params should stay visible:\n$log", log.contains("_is_native=true"))
  }

  @Test
  fun `debug logging never prints regenerated backup codes`() {
    val request =
      Request.Builder()
        .url("https://example.com/v1/me/backup_codes")
        .post(FormBody.Builder().build())
        .build()
    val responseBody =
      """
      {"response":{"object":"backup_code","id":"bc_1","codes":["backup-one","backup-two"]},
      "errors":[{"code":"form_code_incorrect"}]}
      """
        .trimIndent()

    val log = logFor(request, responseBody)

    assertFalse("log leaked a backup code:\n$log", log.contains("backup-one"))
    assertFalse("log leaked a backup code:\n$log", log.contains("backup-two"))
    assertTrue("error codes should stay visible:\n$log", log.contains("form_code_incorrect"))
  }
}
