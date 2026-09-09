package com.clerk.api

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ClerkConfigurationTest {
  private fun key(value: String, mode: String = "test") =
    "pk_${mode}_" + Base64.getEncoder().encodeToString(value.toByteArray())

  private fun rejects(code: String, operation: () -> Unit) {
    try {
      operation()
      fail("Expected $code")
    } catch (error: CoreException) {
      assertEquals(code, error.code)
    }
  }

  @Test fun validKeysNormalizeWhitespaceAndPreserveTheConfiguredCallback() {
    for (mode in listOf("test", "live")) {
      val publishableKey = key("clerk.example.com$", mode)
      val config = ClerkConfiguration("  $publishableKey\n", "app.clerk://oauth/callback?source=app")
      assertEquals(publishableKey, config.publishableKey)
      assertEquals("https://clerk.example.com", config.frontendAPI)
      assertEquals("app.clerk://oauth/callback?source=app", config.callbackUrl)
    }
  }

  @Test fun invalidPublishableKeysAlwaysProduceStructuredErrors() {
    val malformed = listOf("", "   ", "invalid", "pk_invalid_something", "pk_test_!!!", Base64.getEncoder().encodeToString("clerk.example.com$".toByteArray())) + listOf(
      "", "x", "clerk.example.com", "clerk.example.comx", "user@clerk.example.com$", "clerk.example.com/path$",
      "clerk.example.com?query$", "clerk.example.com#fragment$", "bad host$", "[broken$",
    ).map { key(it) }
    for (value in malformed) rejects("invalid_publishable_key") {
      ClerkConfiguration(value, "app.clerk://oauth/callback")
    }
  }

  @Test fun invalidCallbackRoutesAlwaysProduceStructuredErrors() {
    for (callback in listOf(
      "relative", "http://example.com/callback", "HTTP://example.com/callback",
      "file://example.com/callback", "javascript://example.com/callback",
      "app.clerk://user@example.com/callback", "app.clerk://oauth/callback#fragment",
      "app.clerk://bad host/callback", "app.clerk://[broken/callback",
    )) rejects("invalid_callback_url") {
      ClerkConfiguration(key("clerk.example.com$"), callback)
    }
  }

  @Test fun httpsCallbacksAreAcceptedForPlatformsSupportingVerifiedLinks() {
    val config = ClerkConfiguration(key("clerk.example.com$"), "https://example.com/callback")
    assertEquals("https://example.com/callback", config.callbackUrl)
  }
}
