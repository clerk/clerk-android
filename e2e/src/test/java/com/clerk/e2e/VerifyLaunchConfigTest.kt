package com.clerk.e2e

import com.clerk.ui.auth.AuthMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val FALLBACK_KEY = "pk_test_ZXhhbXBsZS5jbGVyay5hY2NvdW50cy5kZXYk"
private const val EXTRA_KEY = "pk_live_c3Vifj4/LmRldiQ="

class VerifyLaunchConfigTest {
  private fun parse(vararg extras: Pair<String, String>) =
    VerifyLaunchConfig.parse(extra = extras.toMap()::get, fallbackPublishableKey = FALLBACK_KEY)

  @Test
  fun `reads every input from its extra`() {
    val config =
      parse(
        "verifyPublishableKey" to EXTRA_KEY,
        "verifyRunId" to "run-1",
        "verifyStorageScope" to "scope-1",
        "verifyLaunchId" to "launch-1",
        "verifyScreen" to "orgProfile",
        "verifyAuthMode" to "signUp",
        "verifySignInTicket" to "ticket-1",
        "verifyLogLevel" to "debug",
      )

    assertEquals(
      VerifyLaunchConfig(
        publishableKey = EXTRA_KEY,
        runId = "run-1",
        storageScope = "scope-1",
        launchId = "launch-1",
        screen = VerifyScreen.OrgProfile,
        authMode = AuthMode.SignUp,
        signInTicket = "ticket-1",
        debugLogging = true,
        screenFailure = null,
      ),
      config,
    )
    assertNull(config.publishableKeyFailure)
  }

  @Test
  fun `no extras keeps the BuildConfig key and the legacy home screen`() {
    assertEquals(
      VerifyLaunchConfig(
        publishableKey = FALLBACK_KEY,
        runId = null,
        storageScope = null,
        launchId = null,
        screen = VerifyScreen.Home,
        authMode = AuthMode.SignInOrUp,
        signInTicket = null,
        debugLogging = false,
        screenFailure = null,
      ),
      parse(),
    )
  }

  @Test
  fun `blank extras count as missing`() {
    val config =
      parse("verifyPublishableKey" to "  ", "verifyScreen" to "", "verifySignInTicket" to " ")

    assertEquals(FALLBACK_KEY, config.publishableKey)
    assertEquals(VerifyScreen.Home, config.screen)
    assertNull(config.screenFailure)
    assertNull(config.signInTicket)
  }

  @Test
  fun `every screen wire name routes to its screen`() {
    val screens =
      listOf("home", "auth", "userProfile", "orgSwitcher", "orgList", "orgProfile").map {
        parse("verifyScreen" to it).screen
      }

    assertEquals(VerifyScreen.entries.toList(), screens)
  }

  @Test
  fun `an unknown screen routes home and reports unknown-screen`() {
    val config = parse("verifyScreen" to "settings")

    assertEquals(VerifyScreen.Home, config.screen)
    assertEquals("unknown-screen", config.screenFailure?.code)
  }

  @Test
  fun `auth modes parse and unknown modes fall back to sign in or up`() {
    assertEquals(AuthMode.SignIn, parse("verifyAuthMode" to "signIn").authMode)
    assertEquals(AuthMode.SignUp, parse("verifyAuthMode" to "signUp").authMode)
    assertEquals(AuthMode.SignInOrUp, parse("verifyAuthMode" to "signInOrUp").authMode)
    assertEquals(AuthMode.SignInOrUp, parse("verifyAuthMode" to "register").authMode)
  }

  @Test
  fun `only the debug log level turns on debug logging`() {
    assertEquals(false, parse("verifyLogLevel" to "info").debugLogging)
    assertEquals(true, parse("verifyLogLevel" to "debug").debugLogging)
  }

  @Test
  fun `well formed keys pass in either base64 alphabet`() {
    listOf(FALLBACK_KEY, "pk_live_c3Vifj4/LmRldiQ=", "pk_test_c3Vifj4_LmRldiQ", "pk_test_eD4-PyQ")
      .forEach { assertNull(parse("verifyPublishableKey" to it).publishableKeyFailure, it) }
  }

  @Test
  fun `malformed keys report invalid_publishable_key`() {
    val keys =
      listOf(
        "pk_test_invalid",
        "pk_test_",
        "sk_test_ZXhhbXBsZS5jbGVyay5hY2NvdW50cy5kZXYk",
        "pk_test_placeholder_for_e2e",
        "pk_test_+/8tZGV2LmNsZXJrLmFjY291bnRzLmRldiQ=",
        "pk_test_ZXhh*mBsZQ",
        "pk_test_ZXhhb",
      )

    keys.forEach {
      val config = VerifyLaunchConfig.parse(extra = { null }, fallbackPublishableKey = it)
      assertEquals("invalid_publishable_key", config.publishableKeyFailure?.code, it)
    }
  }
}
