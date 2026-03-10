package com.clerk.api.sso

import com.clerk.api.Clerk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class RedirectConfigurationTest {
  private val originalApplicationId = Clerk.applicationId

  @After
  fun tearDown() {
    Clerk.applicationId = originalApplicationId
  }

  @Test
  fun defaultRedirectUrl_usesCallbackHost() {
    Clerk.applicationId = "com.example.app"

    assertEquals("clerk://com.example.app.callback", RedirectConfiguration.DEFAULT_REDIRECT_URL)
  }

  @Test
  fun legacyRedirectUrl_keepsOauthHost() {
    Clerk.applicationId = "com.example.app"

    assertEquals("clerk://com.example.app.oauth", RedirectConfiguration.LEGACY_REDIRECT_URL)
  }

  @Test
  fun redirectUrlsReflectTheCurrentApplicationId() {
    Clerk.applicationId = "com.example.first"
    assertEquals("clerk://com.example.first.callback", RedirectConfiguration.DEFAULT_REDIRECT_URL)

    Clerk.applicationId = "com.example.second"
    assertEquals("clerk://com.example.second.callback", RedirectConfiguration.DEFAULT_REDIRECT_URL)
  }

  @Test
  fun defaultRedirectUrl_doesNotThrowWhenApplicationIdIsUnset() {
    Clerk.applicationId = null

    assertEquals("clerk://null.callback", RedirectConfiguration.DEFAULT_REDIRECT_URL)
  }
}
