package com.clerk.api.network

import okhttp3.ConnectionSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ClerkApiHttpClientTest {

  @Test
  fun `http client only allows modern TLS and refuses cleartext`() {
    val client = ClerkApi.buildHttpClient(customHeaders = emptyMap())

    assertEquals(listOf(ConnectionSpec.MODERN_TLS), client.connectionSpecs)
  }

  @Test
  fun `http client refuses the compromised eMudhra intermediates`() {
    val client = ClerkApi.buildHttpClient(customHeaders = emptyMap())

    assertTrue(client.hostnameVerifier is ForbiddenCertificatesHostnameVerifier)
    assertEquals(listOf(ConnectionSpec.MODERN_TLS), client.connectionSpecs)
  }
}
