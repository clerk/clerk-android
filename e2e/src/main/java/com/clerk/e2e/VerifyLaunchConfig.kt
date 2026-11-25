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
  ResetPasswordReverification("resetPasswordReverification"),
  NativeToWeb("nativeToWeb"),
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
) {
  private val decodedPublishableKey: String? = decodePublishableKey(publishableKey)

  val publishableKeyFailure: VerifyFailure? =
    if (decodedPublishableKey != null) {
      null
    } else {
      VerifyFailure(
        code = "invalid_publishable_key",
        message = "The publishable key is missing or is not a pk_test_ or pk_live_ key.",
      )
    }

  val frontendApiUrl: String? = decodedPublishableKey?.let { "https://${it.removeSuffix("$")}" }

  companion object {
    fun parse(extra: (String) -> String?, fallbackPublishableKey: String): VerifyLaunchConfig {
      val argument = { key: String -> extra(key)?.trim()?.takeIf(String::isNotEmpty) }
      return VerifyLaunchConfig(
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

    private fun decodePublishableKey(key: String): String? =
      listOf(Constants.Prefixes.TOKEN_PREFIX_TEST, Constants.Prefixes.TOKEN_PREFIX_LIVE)
        .firstOrNull(key::startsWith)
        ?.let(key::removePrefix)
        ?.takeIf(String::isNotEmpty)
        ?.let(::decodeBase64)
        ?.let(::decodeUtf8)

    private fun decodeUtf8(bytes: ByteArray): String? =
      try {
        Charsets.UTF_8.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(bytes))
          .toString()
      } catch (_: CharacterCodingException) {
        null
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
