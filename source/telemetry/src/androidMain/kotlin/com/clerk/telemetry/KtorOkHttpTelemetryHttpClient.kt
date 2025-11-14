package com.clerk.telemetry

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

/** Android implementation of TelemetryHttpClient using Ktor with the OkHttp engine. */
class KtorOkHttpTelemetryHttpClient(okHttpClient: OkHttpClient = OkHttpClient()) :
  TelemetryHttpClient {

  private val client =
    HttpClient(OkHttp) {
      engine { preconfigured = okHttpClient }
      install(ContentNegotiation) {
        json(
          Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
          }
        )
      }
      // We don't care about HTTP status here – just fire & forget
      expectSuccess = false
    }

  override suspend fun postEvents(
    url: String,
    batch: TelemetryEventBatch,
    headers: Map<String, String>,
  ) {
    client
      .post(url) {
        contentType(ContentType.Application.Json)
        this.headers { headers.forEach { (k, v) -> append(k, v) } }
        setBody(batch)
      }
      .bodyAsText()
  }
}
