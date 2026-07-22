package com.clerk.api.hostedauth

import android.net.Uri
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HostedAuthRedirectUrlTest {
  @Test
  fun callbackUriMatchesCanonicalRedirectUrl() {
    val callbackUri = Uri.parse("clerk://com.example.app.callback?state=abc")

    assertTrue(callbackUri.matchesHostedAuthRedirectUrl("clerk://com.example.app.callback"))
  }

  @Test
  fun callbackUriMatchesCaseInsensitiveSchemeAndAuthority() {
    val callbackUri = Uri.parse("CLERK://Com.Example.App.Callback?state=abc")

    assertTrue(callbackUri.matchesHostedAuthRedirectUrl("clerk://com.example.app.callback"))
  }

  @Test
  fun callbackUriRejectsDifferentScheme() {
    val callbackUri = Uri.parse("other://com.example.app.callback?state=abc")

    assertFalse(callbackUri.matchesHostedAuthRedirectUrl("clerk://com.example.app.callback"))
  }

  @Test
  fun callbackUriRejectsDifferentHost() {
    val callbackUri = Uri.parse("clerk://other.example.app.callback?state=abc")

    assertFalse(callbackUri.matchesHostedAuthRedirectUrl("clerk://com.example.app.callback"))
  }

  @Test
  fun callbackUriRejectsDifferentAuthority() {
    val callbackUri = Uri.parse("clerk://user@com.example.app.callback?state=abc")

    assertFalse(callbackUri.matchesHostedAuthRedirectUrl("clerk://com.example.app.callback"))
  }

  @Test
  fun callbackUriRejectsDifferentPort() {
    val callbackUri = Uri.parse("clerk://com.example.app.callback:8444/callback?state=abc")

    assertFalse(
      callbackUri.matchesHostedAuthRedirectUrl("clerk://com.example.app.callback:8443/callback")
    )
  }

  @Test
  fun callbackUriRejectsDifferentPath() {
    val callbackUri = Uri.parse("myapp:///other?state=abc")

    assertFalse(callbackUri.matchesHostedAuthRedirectUrl("myapp:///hosted-auth-callback"))
  }

  @Test
  fun callbackUriRejectsHostWhenExpectedRedirectHasNoHost() {
    val callbackUri = Uri.parse("myapp://attacker/hosted-auth-callback?state=abc")

    assertFalse(callbackUri.matchesHostedAuthRedirectUrl("myapp:///hosted-auth-callback"))
  }

  @Test
  fun callbackUriMatchesTripleSlashRedirectUrl() {
    val callbackUri = Uri.parse("myapp:///hosted-auth-callback?state=abc")

    assertTrue(callbackUri.matchesHostedAuthRedirectUrl("myapp:///hosted-auth-callback"))
  }

  @Test
  fun callbackUriRejectsAddedSlash() {
    val callbackUri = Uri.parse("clerk://com.example.app.callback/?state=abc")

    assertFalse(callbackUri.matchesHostedAuthRedirectUrl("clerk://com.example.app.callback"))
  }
}
