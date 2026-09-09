package com.clerk.ui.core.extensions

import androidx.compose.runtime.Composable
import com.clerk.api.OAuthProvider
import com.clerk.ui.core.composition.LocalClerk

internal val OAuthProvider.providerName: String
  @Composable
  get() =
    LocalClerk.currentOrNull
      ?.environment
      ?.userSettings
      ?.social
      ?.values
      ?.firstOrNull { it.strategy.rawValue == "oauth_$rawValue" }
      ?.name
      ?.takeIf { it.isNotBlank() }
      ?: when (rawValue) {
        "github" -> "GitHub"
        "linkedin" -> "LinkedIn"
        "tiktok" -> "TikTok"
        "huggingface" -> "Hugging Face"
        else -> rawValue.replace('_', ' ').replaceFirstChar { it.uppercase() }
      }

internal val OAuthProvider.logoUrl: String?
  @Composable
  get() =
    LocalClerk.currentOrNull
      ?.environment
      ?.userSettings
      ?.social
      ?.values
      ?.firstOrNull { it.strategy.rawValue == "oauth_$rawValue" }
      ?.logoUrl
