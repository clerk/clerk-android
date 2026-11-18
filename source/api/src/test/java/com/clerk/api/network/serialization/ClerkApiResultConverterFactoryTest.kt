package com.clerk.api.network.serialization

import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.response.ClientPiggybackedResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Converter
import retrofit2.Retrofit

@RunWith(RobolectricTestRunner::class)
class ClerkApiResultConverterFactoryTest {

  private lateinit var converterFactory: ClerkApiResultConverterFactory
  private lateinit var mockRetrofit: Retrofit

  @Before
  fun setup() {
    converterFactory = ClerkApiResultConverterFactory
    mockRetrofit = mockk(relaxed = true)
  }

  @Test
  fun `responseBodyConverter returns null for non-ClerkResult types`() {
    val stringType = String::class.java
    val annotations = emptyArray<Annotation>()

    val converter = converterFactory.responseBodyConverter(stringType, annotations, mockRetrofit)

    assertNull(converter)
  }

  @Suppress("UNCHECKED_CAST")
  @Test
  fun `responseBodyConverter wraps success type in piggyback response and unwraps on convert`() {
    val clerkResultType =
      createParameterizedType(ClerkResult::class.java, String::class.java, Exception::class.java)
    val requestedType = slot<Type>()
    val delegateConverter = mockk<Converter<ResponseBody, Any>>()
    every { delegateConverter.convert(any()) } returns
      ClientPiggybackedResponse(response = "unwrapped", client = null)
    every {
      mockRetrofit.nextResponseBodyConverter<Any>(any(), capture(requestedType), any())
    } returns delegateConverter

    val converter =
      converterFactory.responseBodyConverter(clerkResultType, emptyArray(), mockRetrofit)
        as Converter<ResponseBody, ClerkResult<*, *>>

    val delegateType = requestedType.captured as ParameterizedType
    assertEquals(ClientPiggybackedResponse::class.java, delegateType.rawType)
    assertEquals(listOf<Type>(String::class.java), delegateType.actualTypeArguments.toList())

    val result = converter.convert("{}".toResponseBody("application/json".toMediaType()))
    assertTrue(result is ClerkResult.Success)
    assertEquals("unwrapped", (result as ClerkResult.Success).value)
  }

  @Test
  fun `responseBodyConverter handles Environment type without wrapping`() {
    val environmentResultType =
      createParameterizedType(
        ClerkResult::class.java,
        Environment::class.java,
        Exception::class.java,
      )

    assertEquals(Environment::class.java, requestedDelegateType(environmentResultType))
  }

  @Test
  fun `responseBodyConverter passes the caller's annotations to the delegate unchanged`() {
    val clerkResultType =
      createParameterizedType(
        ClerkResult::class.java,
        Environment::class.java,
        Exception::class.java,
      )
    val annotations = arrayOf<Annotation>(Deprecated("marker"))
    val delegateAnnotations = slot<Array<Annotation>>()
    every {
      mockRetrofit.nextResponseBodyConverter<Any>(any(), any(), capture(delegateAnnotations))
    } returns mockk(relaxed = true)

    converterFactory.responseBodyConverter(clerkResultType, annotations, mockRetrofit)

    assertEquals(annotations.toList(), delegateAnnotations.captured.toList())
  }

  private fun requestedDelegateType(clerkResultType: Type): Type {
    val requestedType = slot<Type>()
    every {
      mockRetrofit.nextResponseBodyConverter<Any>(any(), capture(requestedType), any())
    } returns mockk(relaxed = true)

    assertNotNull(
      converterFactory.responseBodyConverter(clerkResultType, emptyArray(), mockRetrofit)
    )

    return requestedType.captured
  }

  private fun createParameterizedType(
    rawType: Class<*>,
    vararg typeArguments: Type,
  ): ParameterizedType {
    return object : ParameterizedType {
      override fun getRawType(): Type = rawType

      override fun getActualTypeArguments(): Array<Type> = typeArguments.toList().toTypedArray()

      override fun getOwnerType(): Type? = null
    }
  }
}
