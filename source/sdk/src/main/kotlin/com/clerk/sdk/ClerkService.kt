package com.clerk.sdk

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
        }
      }
      .build()
  }

  fun createRetrofitService(): Retrofit {
    return Retrofit.Builder().client(okHttpClient).build()
  }
}
