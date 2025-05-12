package com.clerk.sdk

import com.clerk.sdk.middleware.incoming.DeviceTokenSavingMiddleware
import com.clerk.sdk.middleware.outgoing.HeaderMiddleware
import com.clerk.sdk.network.ClerkApiVersion
import com.slack.eithernet.ApiResult
import com.slack.eithernet.integration.retrofit.ApiResultCallAdapterFactory
import com.slack.eithernet.integration.retrofit.ApiResultConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

internal object ClerkApiInitializationService {

  /**
   * Lazy initialization of OkHttpClient, attaches interceptors for logging and middleware. Current
   * middleware
   * - [HeaderMiddleware] - adds clerk specific headers to the request
   * - [DeviceTokenSavingMiddleware] - saves device token to the local storage
   * - [HttpLoggingInterceptor] - logs the request and response if debug mode is enabled
   */
  private val okHttpClient by lazy {
    OkHttpClient.Builder()
      .apply {
        if (Clerk.debugMode) {
          addInterceptor(
              HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
            .addInterceptor(HeaderMiddleware())
            .addInterceptor(DeviceTokenSavingMiddleware())
        }
      }
      .build()
  }

  /**
   * Initializes the [Retrofit] instance with the base URL and the OkHttpClient. Currently uses the
   * Clerk API version from the [ClerkApiVersion] object. This should be changed in the future to
   * tie in [Clerk.version]
   *
   * Client is broken down into: [com.clerk.sdk.network.client.ClientApi] [CurrentUserApi]
   * [EnvironmentApi] (internal only)
   *
   * Each API returns either an [ApiResult] or a [com.clerk.sdk.model.error.ClerkAPIError],
   * Eithernet (https://github.com/slackhq/EitherNet) is used to handle the result of the API call.
   *
   * Kotlinx serialization is used to handle the JSON serialization and deserialization.
   *
   * @param baseUrl The base URL for the API.
   * @return The initialized [Retrofit] instance.
   */
  internal fun initializeApi(baseUrl: String): Retrofit {
    val urlWithVersion = "$baseUrl/${ClerkApiVersion.VERSION}/"
    return Retrofit.Builder()
      .baseUrl(urlWithVersion)
      .client(okHttpClient)
      .addConverterFactory(ApiResultConverterFactory)
      .addCallAdapterFactory(ApiResultCallAdapterFactory)
      .addConverterFactory(Json.asConverterFactory("application/json; charset=utf-8".toMediaType()))
      .build()
  }
}
