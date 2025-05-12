package com.clerk.sdk

import com.clerk.sdk.middleware.incoming.DeviceTokenSavingMiddleware
import com.clerk.sdk.middleware.outgoing.HeaderMiddleware
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object ClerkService {

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

  fun initializeApi(baseUrl: String): Retrofit {
    return Retrofit.Builder().baseUrl(baseUrl).client(okHttpClient).build()
  }
}
