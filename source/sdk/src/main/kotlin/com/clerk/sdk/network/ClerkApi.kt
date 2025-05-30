package com.clerk.sdk.network

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.clerk.sdk.Clerk
import com.clerk.sdk.network.middleware.incoming.ClientSyncingMiddleware
import com.clerk.sdk.network.middleware.incoming.DeviceTokenSavingMiddleware
import com.clerk.sdk.network.middleware.outgoing.HeaderMiddleware
import com.clerk.sdk.network.middleware.outgoing.UrlAppendingMiddleware
import com.clerk.sdk.network.serialization.ClerkApiResultCallAdapterFactory
import com.clerk.sdk.network.serialization.ClerkApiResultConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Singleton responsible for configuring and exposing the Clerk API service. */
internal object ClerkApi {

  @OptIn(ExperimentalSerializationApi::class)
  private val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
    namingStrategy = JsonNamingStrategy.SnakeCase
  }

  private var _instance: ClerkApiService? = null

  /** Exposes the configured Clerk API service or throws if not initialized. */
  internal val instance: ClerkApiService
    get() =
      _instance ?: error("ClerkApi is not configured. Call ClerkApi.configure(baseUrl) first.")

  /** Initializes the API client with the given [baseUrl]. */
  fun configure(baseUrl: String, context: Context) {
    _instance = buildRetrofit(baseUrl, context).create(ClerkApiService::class.java)
  }

  /** Builds and configures the Retrofit instance. */
  private fun buildRetrofit(baseUrl: String, context: Context): Retrofit {
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
            addInterceptor(ChuckerInterceptor(context))
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
