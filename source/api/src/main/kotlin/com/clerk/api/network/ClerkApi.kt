package com.clerk.api.network

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.clerk.api.Clerk
import com.clerk.api.network.api.ClientApi
import com.clerk.api.network.api.DeviceAttestationApi
import com.clerk.api.network.api.EnvironmentApi
import com.clerk.api.network.api.OrganizationApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.api.SignInApi
import com.clerk.api.network.api.SignUpApi
import com.clerk.api.network.api.UserApi
import com.clerk.api.network.middleware.incoming.ClientSyncingMiddleware
import com.clerk.api.network.middleware.incoming.DeviceAssertionInterceptor
import com.clerk.api.network.middleware.incoming.DeviceTokenSavingMiddleware
import com.clerk.api.network.middleware.outgoing.UrlAppendingMiddleware
import com.clerk.api.network.middleware.outgoing.VersioningUserAgentMiddleware
import com.clerk.api.network.serialization.ClerkApiResultCallAdapterFactory
import com.clerk.api.network.serialization.ClerkApiResultConverterFactory
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
  internal val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = true
    namingStrategy = JsonNamingStrategy.SnakeCase
  }

  private var _client: ClientApi? = null
  val client: ClientApi
    get() = _client ?: error("ClerkApi is not configured.")

  private var _environment: EnvironmentApi? = null
  val environment: EnvironmentApi
    get() = _environment ?: error("ClerkApi is not configured.")

  private var _session: SessionApi? = null
  val session: SessionApi
    get() = _session ?: error("ClerkApi is not configured.")

  private var _signIn: SignInApi? = null
  val signIn: SignInApi
    get() = _signIn ?: error("ClerkApi is not configured.")

  private var _signUp: SignUpApi? = null
  val signUp: SignUpApi
    get() = _signUp ?: error("ClerkApi is not configured.")

  private var _user: UserApi? = null
  val user: UserApi
    get() = _user ?: error("ClerkApi is not configured.")

  private var _deviceAttestation: DeviceAttestationApi? = null
  val deviceAttestation: DeviceAttestationApi
    get() = _deviceAttestation ?: error("ClerkApi is not configured.")

  private var _organization: OrganizationApi? = null
  val organization: OrganizationApi
    get() = _organization ?: error("ClerkApi is not configured.")

  /** Initializes the API client with the given [baseUrl]. */
  fun configure(baseUrl: String, context: Context) {
    val retrofit = buildRetrofit(baseUrl, context)
    _client = retrofit.create(ClientApi::class.java)
    _environment = retrofit.create(EnvironmentApi::class.java)
    _session = retrofit.create(SessionApi::class.java)
    _signIn = retrofit.create(SignInApi::class.java)
    _signUp = retrofit.create(SignUpApi::class.java)
    _user = retrofit.create(UserApi::class.java)
    _deviceAttestation = retrofit.create(DeviceAttestationApi::class.java)
    _organization = retrofit.create(OrganizationApi::class.java)
  }

  /** Builds and configures the Retrofit instance. */
  private fun buildRetrofit(baseUrl: String, context: Context): Retrofit {
    val urlWithVersion = "$baseUrl/v1/"

    val client =
      OkHttpClient.Builder()
        .apply {
          addInterceptor(ClientSyncingMiddleware(json = json))
          addInterceptor(VersioningUserAgentMiddleware())
          addInterceptor(DeviceTokenSavingMiddleware())
          addInterceptor(UrlAppendingMiddleware())
          addInterceptor(DeviceAssertionInterceptor())

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
