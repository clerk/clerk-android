package com.clerk.api.network.model.environment

import com.clerk.api.sso.OAuthProvider

internal val Environment.allSocialProviders: List<OAuthProvider>
  get() {
    val social = this.userSettings.social
    val enabledProviders = social.filter { it.value.enabled }
    return enabledProviders.map { OAuthProvider.fromStrategy(it.value.strategy) }
  }
