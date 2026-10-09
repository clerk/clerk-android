package com.clerk.e2e

import com.clerk.api.Constants
import com.clerk.ui.auth.AuthMode
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import kotlin.io.encoding.Base64

sealed interface HostScreen {
  val wireName: String

  data object Launching : HostScreen {
    override val wireName = "launching"
  }

  data object Error : HostScreen {
    override val wireName = "error"
  }
}

enum class VerifyScreen(override val wireName: String) : HostScreen {
  Home("home"),
  Auth("auth"),
  CustomSignIn("customSignIn"),
}

data class VerifyLaunchConfig(
  val publishableKey: String,
  val runId: String?,
  val storageScope: String?,
  val launchId: String?,
  val authMode: AuthMode,
  val initialIdentifier: String?,
  val signInTicket: String?,
  val debugLogging: Boolean,
  val hasVerifyInputs: Boolean,
) {
  val publishableKeyFailure: VerifyFailure? =
    if (isWellFormedPublishableKey(publishableKey)) {
      null
    } else {
      VerifyFailure(
        code = "invalid_publishable_key",
        message = "The publishable key is missing or is not a pk_test_ or pk_live_ key.",
      )
    }

  companion object {
    private val VERIFY_INPUTS =
      listOf(
        "verifyPublishableKey",
        "verifyRunId",
        "verifyStorageScope",
        "verifyLaunchId",
        "verifyAuthMode",
        "verifyInitialIdentifier",
        "verifySignInTicket",
        "verifyLogLevel",
      )

    fun parse(extra: (String) -> String?, fallbackPublishableKey: String): VerifyLaunchConfig {
      val argument = { key: String -> extra(key)?.trim()?.takeIf(String::isNotEmpty) }
      return VerifyLaunchConfig(
        hasVerifyInputs = VERIFY_INPUTS.any { argument(it) != null },
        publishableKey = argument("verifyPublishableKey") ?: fallbackPublishableKey,
        runId = argument("verifyRunId"),
        storageScope = argument("verifyStorageScope"),
        launchId = argument("verifyLaunchId"),
        authMode = authMode(argument("verifyAuthMode")),
        initialIdentifier = argument("verifyInitialIdentifier"),
        signInTicket = argument("verifySignInTicket"),
        debugLogging = argument("verifyLogLevel") == "debug",
      )
    }

    private fun authMode(value: String?): AuthMode =
      when (value) {
        "signIn" -> AuthMode.SignIn
        "signUp" -> AuthMode.SignUp
        else -> AuthMode.SignInOrUp
      }

    private fun isWellFormedPublishableKey(key: String): Boolean {
      val bytes =
        listOf(Constants.Prefixes.TOKEN_PREFIX_TEST, Constants.Prefixes.TOKEN_PREFIX_LIVE)
          .firstOrNull(key::startsWith)
          ?.let(key::removePrefix)
          ?.takeIf(String::isNotEmpty)
          ?.let(::decodeBase64)
      return bytes != null && isUtf8(bytes)
    }

    private fun isUtf8(bytes: ByteArray): Boolean =
      try {
        Charsets.UTF_8.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(bytes))
        true
      } catch (_: CharacterCodingException) {
        false
      }

    private fun decodeBase64(value: String): ByteArray? =
      try {
        Base64.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
          .decode(value.replace('-', '+').replace('_', '/'))
      } catch (_: IllegalArgumentException) {
        null
      }
  }
}
