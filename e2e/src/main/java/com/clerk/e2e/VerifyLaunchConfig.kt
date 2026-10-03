package com.clerk.e2e

import com.clerk.ui.auth.AuthMode
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

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
  UserProfile("userProfile"),
  OrgSwitcher("orgSwitcher"),
  OrgList("orgList"),
  OrgProfile("orgProfile"),
}

data class VerifyLaunchConfig(
  val publishableKey: String,
  val runId: String?,
  val storageScope: String?,
  val launchId: String?,
  val screen: VerifyScreen,
  val authMode: AuthMode,
  val signInTicket: String?,
  val debugLogging: Boolean,
  val screenFailure: VerifyFailure?,
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
    fun parse(extra: (String) -> String?, fallbackPublishableKey: String): VerifyLaunchConfig {
      val argument = { key: String -> extra(key)?.trim()?.takeIf(String::isNotEmpty) }
      val requestedScreen = argument("verifyScreen")
      val screen = requestedScreen?.let { name ->
        VerifyScreen.entries.find { it.wireName == name }
      }

      return VerifyLaunchConfig(
        publishableKey = argument("verifyPublishableKey") ?: fallbackPublishableKey,
        runId = argument("verifyRunId"),
        storageScope = argument("verifyStorageScope"),
        launchId = argument("verifyLaunchId"),
        screen = screen ?: VerifyScreen.Home,
        authMode = authMode(argument("verifyAuthMode")),
        signInTicket = argument("verifySignInTicket"),
        debugLogging = argument("verifyLogLevel") == "debug",
        screenFailure =
          if (requestedScreen != null && screen == null) {
            VerifyFailure(
              code = "unknown-screen",
              message = "Unknown verifyScreen \"$requestedScreen\". Showing home instead.",
            )
          } else {
            null
          },
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
        listOf("pk_test_", "pk_live_")
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

    private const val BASE64_ALPHABET =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    private const val BASE64_BITS_PER_CHAR = 6
    private const val BASE64_CHARS_PER_GROUP = 4
    private const val BITS_PER_BYTE = 8
    private const val BYTE_MASK = 0xFF

    private fun decodeBase64(value: String): ByteArray? {
      val digits = value.trimEnd('=').replace('-', '+').replace('_', '/')
      if (digits.length % BASE64_CHARS_PER_GROUP == 1 || digits.any { it !in BASE64_ALPHABET }) {
        return null
      }
      val bytes = ArrayList<Byte>(digits.length * BASE64_BITS_PER_CHAR / BITS_PER_BYTE)
      var buffer = 0
      var bufferedBits = 0
      for (char in digits) {
        buffer = (buffer shl BASE64_BITS_PER_CHAR) or BASE64_ALPHABET.indexOf(char)
        bufferedBits += BASE64_BITS_PER_CHAR
        if (bufferedBits >= BITS_PER_BYTE) {
          bufferedBits -= BITS_PER_BYTE
          bytes.add(((buffer shr bufferedBits) and BYTE_MASK).toByte())
        }
      }
      return bytes.toByteArray()
    }
  }
}
