package com.clerk.sdk.network

import com.clerk.sdk.Clerk
import com.clerk.sdk.error.ClerkClientError
import com.clerk.sdk.network.encoding.FormUrlEncodedConverterFactory
import com.clerk.sdk.network.middleware.incoming.DeviceTokenSavingMiddleware
import com.clerk.sdk.network.middleware.outgoing.HeaderMiddleware
import com.clerk.sdk.network.middleware.outgoing.UrlAppendingMiddleware
import com.slack.eithernet.integration.retrofit.ApiResultCallAdapterFactory
import com.slack.eithernet.integration.retrofit.ApiResultConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Singleton responsible for configuring and exposing the Clerk API service. */
internal object ClerkApi {

  private var _instance: ClerkApiService? = null

  /** Exposes the configured Clerk API service or throws if not initialized. */
  internal val instance: ClerkApiService
    get() =
      _instance
        ?: throw ClerkClientError(
          "ClerkApi is not configured. Call ClerkApi.configure(baseUrl) first."
        )

  /** Initializes the API client with the given [baseUrl]. */
  fun configure(baseUrl: String) {
    _instance = buildRetrofit(baseUrl).create(ClerkApiService::class.java)
  }

  /** Builds and configures the Retrofit instance. */
  private fun buildRetrofit(baseUrl: String): Retrofit {
    val urlWithVersion = "$baseUrl/${ClerkApiVersion.VERSION}/"

    val client =
      OkHttpClient.Builder()
        .apply {
          addInterceptor(HeaderMiddleware())
          addInterceptor(DeviceTokenSavingMiddleware())
          addInterceptor(UrlAppendingMiddleware())

          if (Clerk.debugMode) {
            addInterceptor(
              HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
          }
        }
        .build()

    val json = Json {
      isLenient = true
      ignoreUnknownKeys = true
      coerceInputValues = true
      explicitNulls = false
    }

    return Retrofit.Builder()
      .baseUrl(urlWithVersion)
      .client(client)
      .addConverterFactory(ApiResultConverterFactory)
      .addCallAdapterFactory(ApiResultCallAdapterFactory)
      .addConverterFactory(FormUrlEncodedConverterFactory())
      .addConverterFactory(json.asConverterFactory("application/json; charset=utf-8".toMediaType()))
      .build()
  }
}
