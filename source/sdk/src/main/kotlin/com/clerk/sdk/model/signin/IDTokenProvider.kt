package com.clerk.sdk.model.signin

import kotlinx.serialization.Serializable

@Serializable
enum class IDTokenProvider {
  APPLE,
  GOOGLE,
  MICROSOFT,
}
