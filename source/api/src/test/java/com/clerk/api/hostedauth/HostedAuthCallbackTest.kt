package com.clerk.api.hostedauth

import android.net.Uri
import com.clerk.api.network.model.hostedauth.HostedAuthResource
import com.clerk.api.network.serialization.ClerkResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HostedAuthCallbackTest {

  @Test
  fun validateAcceptsMatchingCallback() {
    val uri =
      Uri.parse(
        "$REDIRECT_URL?state=$STATE&rotating_token_nonce=nonce_123&created_session_id=sess_123"
      )

    val result = validate(uri)

    assertTrue(result is ClerkResult.Success)
    val callback = (result as ClerkResult.Success).value
    assertEquals("nonce_123", callback.rotatingTokenNonce)
    assertEquals("sess_123", callback.createdSessionId)
  }

  @Test
  fun validateRejectsStateMismatch() {
    val uri =
      Uri.parse(
        "$REDIRECT_URL?state=forged&rotating_token_nonce=nonce_123&created_session_id=sess_123"
      )

    assertFailureContains(validate(uri), "state did not match")
  }

  @Test
  fun validateRejectsDuplicateStateParameter() {
    val uri =
      Uri.parse(
        "$REDIRECT_URL?state=$STATE&state=$STATE" +
          "&rotating_token_nonce=nonce_123&created_session_id=sess_123"
      )

    assertFailureContains(validate(uri), "state did not match")
  }

  @Test
  fun validateRejectsDuplicateNonceParameter() {
    val uri =
      Uri.parse(
        "$REDIRECT_URL?state=$STATE&rotating_token_nonce=nonce_123" +
          "&rotating_token_nonce=nonce_456&created_session_id=sess_123"
      )

    assertFailureContains(validate(uri), "rotating token nonce")
  }

  @Test
  fun validateRejectsBlankNonce() {
    val uri =
      Uri.parse("$REDIRECT_URL?state=$STATE&rotating_token_nonce=&created_session_id=sess_123")

    assertFailureContains(validate(uri), "rotating token nonce")
  }

  @Test
  fun validateRejectsMissingCreatedSessionId() {
    val uri = Uri.parse("$REDIRECT_URL?state=$STATE&rotating_token_nonce=nonce_123")

    assertFailureContains(validate(uri), "created session")
  }

  @Test
  fun authenticationUriAcceptsHttpsUrl() {
    val resource = HostedAuthResource(objectType = "hosted_auth", url = "https://portal.dev/start")

    assertEquals(Uri.parse("https://portal.dev/start"), resource.authenticationUri())
  }

  @Test
  fun authenticationUriRejectsHttpUrl() {
    val resource = HostedAuthResource(objectType = "hosted_auth", url = "http://portal.dev/start")

    assertNull(resource.authenticationUri())
  }

  @Test
  fun authenticationUriRejectsUserInfo() {
    val resource =
      HostedAuthResource(objectType = "hosted_auth", url = "https://user@portal.dev/start")

    assertNull(resource.authenticationUri())
  }

  @Test
  fun authenticationUriRejectsOpaqueUri() {
    val resource = HostedAuthResource(objectType = "hosted_auth", url = "mailto:user@portal.dev")

    assertNull(resource.authenticationUri())
  }

  @Test
  fun authenticationUriRejectsWrongObjectType() {
    val resource = HostedAuthResource(objectType = "client", url = "https://portal.dev/start")

    assertNull(resource.authenticationUri())
  }

  private fun validate(uri: Uri) =
    validateHostedAuthCallback(uri = uri, redirectUrl = REDIRECT_URL, expectedState = STATE)

  private fun assertFailureContains(
    result: ClerkResult<HostedAuthCallback, *>,
    expectedFragment: String,
  ) {
    assertTrue(result is ClerkResult.Failure)
    val message = (result as ClerkResult.Failure).throwable?.message.orEmpty()
    assertTrue(
      "Expected \"$message\" to contain \"$expectedFragment\"",
      message.contains(expectedFragment),
    )
  }

  private companion object {
    const val REDIRECT_URL = "clerk://com.example.app.callback"
    const val STATE = "state_123"
  }
}
