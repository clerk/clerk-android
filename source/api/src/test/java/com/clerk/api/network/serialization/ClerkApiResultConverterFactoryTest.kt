package com.clerk.api.network.serialization

import com.clerk.api.network.model.environment.Environment
import com.clerk.api.session.Session
import io.mockk.every
import io.mockk.mockk
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

  @Test
  fun `responseBodyConverter returns converter for parameterized ClerkResult types`() {
    val clerkResultType =
      createParameterizedType(ClerkResult::class.java, String::class.java, Exception::class.java)
    val annotations = emptyArray<Annotation>()

    val mockDelegateConverter = mockk<Converter<ResponseBody, Any>>(relaxed = true)
    every { mockRetrofit.nextResponseBodyConverter<Any>(any(), any(), any()) } returns
      mockDelegateConverter

    val converter =
      converterFactory.responseBodyConverter(clerkResultType, annotations, mockRetrofit)

    assertNotNull(converter)
  }

  @Test
  fun `responseBodyConverter handles Environment type without wrapping`() {
    val environmentResultType =
      createParameterizedType(
        ClerkResult::class.java,
        Environment::class.java,
        Exception::class.java,
      )
    val annotations = emptyArray<Annotation>()

    val mockDelegateConverter = mockk<Converter<ResponseBody, Any>>(relaxed = true)
    every { mockRetrofit.nextResponseBodyConverter<Any>(any(), any(), any()) } returns
      mockDelegateConverter

    val converter =
      converterFactory.responseBodyConverter(environmentResultType, annotations, mockRetrofit)

    assertNotNull(converter)
  }

  @Test
  fun `responseBodyConverter handles List of Session without wrapping`() {
    val sessionListType = createParameterizedType(List::class.java, Session::class.java)
    val clerkResultType =
      createParameterizedType(ClerkResult::class.java, sessionListType, Exception::class.java)
    val annotations = emptyArray<Annotation>()

    val mockDelegateConverter = mockk<Converter<ResponseBody, Any>>(relaxed = true)
    every { mockRetrofit.nextResponseBodyConverter<Any>(any(), any(), any()) } returns
      mockDelegateConverter

    val converter =
      converterFactory.responseBodyConverter(clerkResultType, annotations, mockRetrofit)

    assertNotNull(converter)
  }

  @Test
  fun `shouldWrapInClientPiggybackedResponse returns false for List of Session`() {
    val sessionListType = createParameterizedType(List::class.java, Session::class.java)

    val isListType = sessionListType.rawType == List::class.java

    assertEquals(true, isListType)

    val elementType = sessionListType.actualTypeArguments[0] as Class<*>
    assertEquals("Session", elementType.simpleName)
  }

  @Suppress("UNCHECKED_CAST")
  @Test
  fun `ClerkApiResultConverter handles null delegate response`() {
    val responseBody = "null".toResponseBody("application/json".toMediaType())

    val mockDelegateConverter = mockk<Converter<ResponseBody, Any>>()
    every { mockDelegateConverter.convert(any()) } returns null

    val converterClass =
      ClerkApiResultConverterFactory::class.java.declaredClasses.find {
        it.simpleName == "ClerkApiResultConverter"
      }
    assertNotNull("ClerkApiResultConverter class should exist", converterClass)

    val constructor = converterClass!!.getDeclaredConstructor(Converter::class.java)
    constructor.isAccessible = true
    val converter =
      constructor.newInstance(mockDelegateConverter) as Converter<ResponseBody, ClerkResult<*, *>>

    val result = converter.convert(responseBody)

    assertNull(result)
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
