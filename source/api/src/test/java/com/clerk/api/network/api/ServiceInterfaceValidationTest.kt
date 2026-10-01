package com.clerk.api.network.api

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.serialization.ClerkApiResultCallAdapterFactory
import com.clerk.api.network.serialization.ClerkApiResultConverterFactory
import kotlin.test.Test
import kotlin.test.assertTrue
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * The per-API tests mock these interfaces, so they never see Retrofit's annotation parsing. Eager
 * validation parses every method of every service [ClerkApi] creates, failing here instead of on a
 * user's first call.
 */
class ServiceInterfaceValidationTest {

  private val retrofit =
    Retrofit.Builder()
      .baseUrl("https://clerk.example.com/v1/")
      .addCallAdapterFactory(ClerkApiResultCallAdapterFactory)
      .addConverterFactory(ClerkApiResultConverterFactory)
      .addConverterFactory(
        ClerkApi.json.asConverterFactory("application/json; charset=utf-8".toMediaType())
      )
      .validateEagerly(true)
      .build()

  @Test
  fun `every service interface passes Retrofit validation`() {
    val services = serviceInterfaces()
    assertTrue(UserApi::class.java in services, "ClerkApi services found: $services")

    val failures = services.mapNotNull { service ->
      runCatching { retrofit.create(service) }
        .exceptionOrNull()
        ?.let { "${service.simpleName}: ${it.message}" }
    }

    assertTrue(failures.isEmpty(), failures.joinToString("\n"))
  }

  private fun serviceInterfaces(): List<Class<*>> {
    val servicePackage = UserApi::class.java.name.substringBeforeLast('.')
    return ClerkApi::class
      .java
      .declaredFields
      .map { it.type }
      .filter { it.isInterface && it.name.substringBeforeLast('.') == servicePackage }
      // getChallenge() returns void, which Retrofit rejects. Nothing has called this API since
      // device attestation was removed in #571; drop the exclusion when the API is deleted.
      .filterNot { it == DeviceAttestationApi::class.java }
      .distinct()
  }
}
