package com.clerk.sdk

import android.util.Base64
import com.clerk.configuration.PublishableKeyHelper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PublishableKeyHelperTest {

  private lateinit var publishableKeyHelper: PublishableKeyHelper

  @Before
  fun setUp() {
    publishableKeyHelper = PublishableKeyHelper()
  }

  @Test
  fun `extractApiUrl with valid test key returns correct URL`() {
    // Given
    val domain = "clerk.example.com"
    val encodedDomain = Base64.encodeToString("${domain}x".toByteArray(), Base64.DEFAULT)
    val testKey = "${TokenConstants.TOKEN_PREFIX_TEST}$encodedDomain"

    // When
    val result = publishableKeyHelper.extractApiUrl(testKey)

    // Then
    assertEquals("https://$domain", result)
  }

  @Test
  fun `extractApiUrl with valid live key returns correct URL`() {
    // Given
    val domain = "clerk.example.com"
    val encodedDomain = Base64.encodeToString("${domain}x".toByteArray(), Base64.DEFAULT)
    val liveKey = "${TokenConstants.TOKEN_PREFIX_LIVE}$encodedDomain"

    // When
    val result = publishableKeyHelper.extractApiUrl(liveKey)

    // Then
    assertEquals("https://$domain", result)
  }

  @Test(expected = IllegalStateException::class)
  fun `extractApiUrl with empty decoded string throws ClerkClientError`() {
    // Given
    val emptyString = ""
    val encodedEmptyString = Base64.encodeToString(emptyString.toByteArray(), Base64.DEFAULT)
    val testKey = "${TokenConstants.TOKEN_PREFIX_TEST}$encodedEmptyString"

    // When
    publishableKeyHelper.extractApiUrl(testKey)

    // Then
    // ClerkClientError is expected to be thrown
  }

  @Test
  fun `extractApiUrl with no prefix still works correctly`() {
    // Given
    val domain = "clerk.example.com"
    val encodedDomain = Base64.encodeToString("${domain}x".toByteArray(), Base64.DEFAULT)

    // When
    val result = publishableKeyHelper.extractApiUrl(encodedDomain)

    // Then
    assertEquals("https://$domain", result)
  }

  @Test
  fun `extractApiUrl with single character domain returns https prefix only`() {
    // Given
    val singleCharDomain = "x"
    val encodedSingleChar = Base64.encodeToString(singleCharDomain.toByteArray(), Base64.DEFAULT)
    val testKey = "${TokenConstants.TOKEN_PREFIX_TEST}$encodedSingleChar"

    // When
    val result = publishableKeyHelper.extractApiUrl(testKey)

    // Then
    // After dropping the last character, we'd have an empty host
    assertEquals("https://", result)
  }
}
