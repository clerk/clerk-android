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
        "verifyAuthMode" to "signUp",
        "verifyInitialIdentifier" to "+12015550100",
        "verifySignInTicket" to "ticket-1",
        "verifyLogLevel" to "debug",
      )

    assertEquals(
      VerifyLaunchConfig(
        publishableKey = EXTRA_KEY,
        runId = "run-1",
        storageScope = "scope-1",
        launchId = "launch-1",
        authMode = AuthMode.SignUp,
        initialIdentifier = "+12015550100",
        signInTicket = "ticket-1",
        debugLogging = true,
        hasVerifyInputs = true,
      ),
      config,
    )
    assertNull(config.publishableKeyFailure)
  }

  @Test
  fun `no extras keeps the BuildConfig key and is not a verify launch`() {
    assertEquals(
      VerifyLaunchConfig(
        publishableKey = FALLBACK_KEY,
        runId = null,
        storageScope = null,
        launchId = null,
        authMode = AuthMode.SignInOrUp,
        initialIdentifier = null,
        signInTicket = null,
        debugLogging = false,
        hasVerifyInputs = false,
      ),
      parse(),
    )
  }

  @Test
  fun `any single verify input marks a verify launch`() {
    listOf(
        "verifyPublishableKey" to EXTRA_KEY,
        "verifyRunId" to "run",
        "verifyStorageScope" to "scope",
        "verifyLaunchId" to "launch",
        "verifyAuthMode" to "signIn",
        "verifyInitialIdentifier" to "user@example.com",
        "verifySignInTicket" to "ticket",
        "verifyLogLevel" to "debug",
      )
      .forEach { assertEquals(true, parse(it).hasVerifyInputs, it.first) }
    assertEquals(false, parse("verifyRunId" to " ", "unrelated" to "x").hasVerifyInputs)
  }

  @Test
  fun `blank extras count as missing`() {
    val config =
      parse(
        "verifyPublishableKey" to "  ",
        "verifySignInTicket" to " ",
        "verifyInitialIdentifier" to " ",
      )

    assertEquals(FALLBACK_KEY, config.publishableKey)
    assertNull(config.signInTicket)
    assertNull(config.initialIdentifier)
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
