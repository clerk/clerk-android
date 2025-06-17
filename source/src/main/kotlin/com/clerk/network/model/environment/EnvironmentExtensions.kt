package com.clerk.network.model.environment

import com.clerk.sso.OAuthProvider

internal val Environment.allSocialProviders: List<OAuthProvider>
  get() {
    val social = this.userSettings?.social.orEmpty()
    val enabledProviders = social.filter { it.value.enabled }
    return enabledProviders.map { OAuthProvider.fromStrategy(it.value.strategy) }
  }
