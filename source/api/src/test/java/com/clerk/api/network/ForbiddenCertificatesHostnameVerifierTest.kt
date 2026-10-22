package com.clerk.api.network

import javax.net.ssl.SSLPeerUnverifiedException
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.CertificatePinner.Companion.sha256Hash
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import okio.ByteString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ForbiddenCertificatesHostnameVerifierTest {

  private val root = HeldCertificate.Builder().certificateAuthority(1).build()
  private val intermediate =
    HeldCertificate.Builder().certificateAuthority(0).signedBy(root).build()
  private val leaf =
    HeldCertificate.Builder().addSubjectAlternativeName("127.0.0.1").signedBy(intermediate).build()

  private val server = MockWebServer()

  @Before
  fun setUp() {
    server.useHttps(
      HandshakeCertificates.Builder()
        .heldCertificate(leaf, intermediate.certificate)
        .build()
        .sslSocketFactory()
    )
    server.start()
  }

  @After
  fun tearDown() {
    server.close()
  }

  @Test
  fun `server presenting a forbidden intermediate is refused before the request is sent`() {
    val client = trustingRoot().forbiddingCertificates(setOf(intermediate.certificate.sha256Hash()))

    assertThrows(SSLPeerUnverifiedException::class.java) { get(client) }
    assertEquals(0, server.requestCount)
  }

  @Test
  fun `server presenting no forbidden certificate is reached`() {
    server.enqueue(MockResponse(body = "ok"))
    val client = trustingRoot().forbiddingCertificates(setOf(root.certificate.sha256Hash()))

    assertEquals("ok", get(client))
    assertEquals(1, server.requestCount)
  }

  @Test
  fun `the platform hostname check still applies`() {
    val otherLeaf =
      HeldCertificate.Builder()
        .addSubjectAlternativeName("clerk.com")
        .signedBy(intermediate)
        .build()
    server.useHttps(
      HandshakeCertificates.Builder()
        .heldCertificate(otherLeaf, intermediate.certificate)
        .build()
        .sslSocketFactory()
    )
    val client = trustingRoot().forbiddingCertificates(emptySet())

    assertThrows(SSLPeerUnverifiedException::class.java) { get(client) }
  }

  @Test
  fun `default denylist holds the eight compromised eMudhra intermediates`() {
    val denylist: Set<ByteString> = ForbiddenCertificatesHostnameVerifier.CVE_2026_111943
    assertEquals(8, denylist.size)
    assertTrue(denylist.all { it.size == 32 })
  }

  private fun trustingRoot(): OkHttpClient {
    val trusted = HandshakeCertificates.Builder().addTrustedCertificate(root.certificate).build()
    return OkHttpClient.Builder()
      .sslSocketFactory(trusted.sslSocketFactory(), trusted.trustManager)
      .build()
  }

  private fun get(client: OkHttpClient): String =
    client.newCall(Request(server.url("/").newBuilder().host("127.0.0.1").build())).execute().use {
      it.body.string()
    }
}
