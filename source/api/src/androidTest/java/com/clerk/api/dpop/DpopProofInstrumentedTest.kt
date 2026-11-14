package com.clerk.api.dpop

import android.content.Context
import android.os.SystemClock
import android.util.Base64
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.clerk.api.devicekey.DeviceKey
import com.clerk.api.devicekey.DeviceKeyFactory
import java.io.File
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.Headers.Companion.headersOf
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assume.assumeTrue
import org.junit.Test

class DpopProofInstrumentedTest {
  private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
  private val arguments = InstrumentationRegistry.getArguments()
  private val key: DeviceKey = DeviceKeyFactory.create(context)
  private val clientUrl = "https://clerk.example.com/v1/client?_is_native=true".toHttpUrl()
  private val directory = File(context.getExternalFilesDir(null), "dpop-proofs").apply { mkdirs() }

  @Test
  fun writesProofWithoutToken() {
    write("without-token", listOf(proofCase(clientUrl, accessToken = null)))
  }

  @Test
  fun writesProofWithToken() {
    write("with-token", listOf(proofCase(clientUrl, accessToken = ACCESS_TOKEN)))
  }

  @Test
  fun writesProofAfterRecordingDate() {
    val serverDate = arguments.getString("serverDate")
    assumeTrue("pass a server Date header value as serverDate", serverDate != null)
    val serverTime = assertNotNull(headersOf("Date", serverDate!!).getDate("Date"))
    val serverClock =
      ServerClock(Clock.System).apply { record(Instant.fromEpochMilliseconds(serverTime.time)) }

    report("serverDate=$serverDate local=${Clock.System.now()} server=${serverClock.now()}")
    write("server-clock", listOf(proofCase(clientUrl, now = serverClock.now())))
    write("local-clock", listOf(proofCase(clientUrl, now = Clock.System.now())))
  }

  @Test
  fun writesTwoProofs() {
    val proofs = List(2) { proofCase(clientUrl) }

    assertNotEquals(proofs[0].claim("jti"), proofs[1].claim("jti"))
    write("two", proofs)
  }

  @Test
  fun writesProofForUrlWithQueryAndFragment() {
    val url = "https://clerk.example.com/v1/client/sessions?_is_native=true&a=b#section".toHttpUrl()
    val proof = proofCase(url)

    assertEquals("https://clerk.example.com/v1/client/sessions", proof.claim("htu"))
    write("query-and-fragment", listOf(proof))
  }

  @Test
  fun writesThousandProofs() {
    val proofs = List(PROOF_COUNT) { proofCase(clientUrl) }

    assertEquals(PROOF_COUNT, proofs.map { it.claim("jti") }.toSet().size)
    write("thousand", proofs)
  }

  @Test
  fun writesJwk() {
    File(directory, "jwk.json").writeText(key.publicJwk.toString())
    File(directory, "thumbprint.txt").writeText(key.thumbprint)
    report("thumbprint=${key.thumbprint}")
  }

  @Test
  fun timings() {
    val unsignedKey =
      object : DeviceKey by key {
        override fun sign(data: ByteArray): ByteArray = ByteArray(SIGNATURE_SIZE)
      }

    val withoutSignMs = List(PROOF_COUNT) { timed { build(unsignedKey, clientUrl) } }.sorted()
    val withSignMs = List(PROOF_COUNT) { timed { build(key, clientUrl) } }.sorted()

    report(
      "level=${key.securityLevel.wireName} " +
        "build_without_sign_p50_ms=${withoutSignMs.median()} " +
        "build_without_sign_p90_ms=${withoutSignMs.p90()} " +
        "build_with_sign_p50_ms=${withSignMs.median()} " +
        "build_with_sign_p90_ms=${withSignMs.p90()}"
    )
  }

  private fun proofCase(
    url: HttpUrl,
    accessToken: String? = null,
    now: Instant = Clock.System.now(),
  ): JsonObject = buildJsonObject {
    put("method", "GET")
    put("url", url.toString())
    accessToken?.let { put("token", it) }
    put("proof", build(key, url, accessToken, now))
    put("thumbprint", key.thumbprint)
  }

  private fun build(
    key: DeviceKey,
    url: HttpUrl,
    accessToken: String? = null,
    now: Instant = Clock.System.now(),
  ): String =
    DpopProof.build(
      key = key,
      request = BoundRequest(method = "GET", url = url, accessToken = accessToken),
      now = now,
      jti = UUID.randomUUID().toString(),
    )

  private fun JsonObject.claim(name: String): String {
    val payload = getValue("proof").jsonPrimitive.content.split(".")[1]
    val claims = Json.parseToJsonElement(String(Base64.decode(payload, Base64.URL_SAFE)))
    return claims.jsonObject.getValue(name).jsonPrimitive.content
  }

  private fun write(name: String, proofs: List<JsonObject>) {
    val file = File(directory, "$name.jsonl")
    file.writeText(proofs.joinToString(separator = "\n", postfix = "\n"))
    report("wrote ${proofs.size} proofs to ${file.absolutePath}")
  }

  private fun timed(block: () -> Unit): Double {
    val start = SystemClock.elapsedRealtimeNanos()
    block()
    return (SystemClock.elapsedRealtimeNanos() - start) / NANOS_PER_MILLI
  }

  private fun List<Double>.median(): Double = this[size / 2]

  private fun List<Double>.p90(): Double = this[size * 9 / 10]

  private fun report(line: String) {
    Log.i(TAG, line)
  }

  private companion object {
    const val TAG = "DpopProofTest"
    const val PROOF_COUNT = 1000
    const val SIGNATURE_SIZE = 64
    const val NANOS_PER_MILLI = 1_000_000.0
    const val ACCESS_TOKEN = "Kz~8mXK1EalYznwH-LC-1fBAo.4Ljp~zsPE_NeO.gxU"
  }
}
