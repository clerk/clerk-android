package com.clerk.sdk.network

import com.clerk.clerkserializer.ClerkApiResultCallAdapterFactory
import com.clerk.clerkserializer.ClerkApiResultConverterFactory
import com.clerk.sdk.Clerk
import com.clerk.sdk.error.ClerkClientError
import com.clerk.sdk.network.middleware.incoming.ClientSyncingMiddleware
import com.clerk.sdk.network.middleware.incoming.DeviceTokenSavingMiddleware
import com.clerk.sdk.network.middleware.outgoing.HeaderMiddleware
import com.clerk.sdk.network.middleware.outgoing.UrlAppendingMiddleware
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Singleton responsible for configuring and exposing the Clerk API service. */
internal object ClerkApi {

  private val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
  }

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
          addInterceptor(ClientSyncingMiddleware(json = json))
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

    return Retrofit.Builder()
      .baseUrl(urlWithVersion)
      .client(client)
      .addCallAdapterFactory(ClerkApiResultCallAdapterFactory)
      .addConverterFactory(ClerkApiResultConverterFactory)
      .addConverterFactory(json.asConverterFactory("application/json; charset=utf-8".toMediaType()))
      .build()
  }
}
