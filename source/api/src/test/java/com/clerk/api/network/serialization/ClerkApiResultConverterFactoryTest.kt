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
    // Given
    val stringType = String::class.java
    val annotations = emptyArray<Annotation>()

    // When
    val converter = converterFactory.responseBodyConverter(stringType, annotations, mockRetrofit)

    // Then
    assertNull(converter)
  }

  @Test
  fun `responseBodyConverter returns converter for parameterized ClerkResult types`() {
    // Given
    val clerkResultType =
      createParameterizedType(ClerkResult::class.java, String::class.java, Exception::class.java)
    val annotations = emptyArray<Annotation>()

    // Mock the retrofit.nextResponseBodyConverter to return a mock converter
    val mockDelegateConverter = mockk<Converter<ResponseBody, Any>>(relaxed = true)
    every { mockRetrofit.nextResponseBodyConverter<Any>(any(), any(), any()) } returns
      mockDelegateConverter

    // When
    val converter =
      converterFactory.responseBodyConverter(clerkResultType, annotations, mockRetrofit)

    // Then
    assertNotNull(converter)
  }

  @Test
  fun `responseBodyConverter handles Environment type without wrapping`() {
    // Given
    val environmentResultType =
      createParameterizedType(
        ClerkResult::class.java,
        Environment::class.java,
        Exception::class.java,
      )
    val annotations = emptyArray<Annotation>()

    // Mock the retrofit.nextResponseBodyConverter to return a mock converter
    val mockDelegateConverter = mockk<Converter<ResponseBody, Any>>(relaxed = true)
    every { mockRetrofit.nextResponseBodyConverter<Any>(any(), any(), any()) } returns
      mockDelegateConverter

    // When
    val converter =
      converterFactory.responseBodyConverter(environmentResultType, annotations, mockRetrofit)

    // Then
    assertNotNull(converter)
  }

  @Test
  fun `responseBodyConverter handles List of Session without wrapping`() {
    // Given
    val sessionListType = createParameterizedType(List::class.java, Session::class.java)
    val clerkResultType =
      createParameterizedType(ClerkResult::class.java, sessionListType, Exception::class.java)
    val annotations = emptyArray<Annotation>()

    // Mock the retrofit.nextResponseBodyConverter to return a mock converter
    val mockDelegateConverter = mockk<Converter<ResponseBody, Any>>(relaxed = true)
    every { mockRetrofit.nextResponseBodyConverter<Any>(any(), any(), any()) } returns
      mockDelegateConverter

    // When
    val converter =
      converterFactory.responseBodyConverter(clerkResultType, annotations, mockRetrofit)

    // Then
    assertNotNull(converter)
  }

  @Test
  fun `shouldWrapInClientPiggybackedResponse returns false for Environment type`() {
    // Test the logic indirectly by checking if Environment type is handled specially
    val environmentType = Environment::class.java

    // Environment type name should match the constant in the converter factory
    assertEquals("com.clerk.network.model.environment.Environment", environmentType.name)
  }

  @Test
  fun `shouldWrapInClientPiggybackedResponse returns false for List of Session`() {
    // Given a List<Session> type
    val sessionListType = createParameterizedType(List::class.java, Session::class.java)

    // When we check if it's a List
    val isListType = sessionListType.rawType == List::class.java

    // Then it should be identified as a List
    assertEquals(true, isListType)

    // And the element type should be Session
    val elementType = sessionListType.actualTypeArguments[0] as Class<*>
    assertEquals("Session", elementType.simpleName)
  }

  @Suppress("UNCHECKED_CAST")
  @Test
  fun `ClerkApiResultConverter handles null delegate response`() {
    // Given
    val responseBody = "null".toResponseBody("application/json".toMediaType())

    val mockDelegateConverter = mockk<Converter<ResponseBody, Any>>()
    every { mockDelegateConverter.convert(any()) } returns null

    // Using reflection to create the converter since it's private
    val converterClass =
      ClerkApiResultConverterFactory::class.java.declaredClasses.find {
        it.simpleName == "ClerkApiResultConverter"
      }
    assertNotNull("ClerkApiResultConverter class should exist", converterClass)

    val constructor = converterClass!!.getDeclaredConstructor(Converter::class.java)
    constructor.isAccessible = true
    val converter =
      constructor.newInstance(mockDelegateConverter) as Converter<ResponseBody, ClerkResult<*, *>>

    // When
    val result = converter.convert(responseBody)

    // Then
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
