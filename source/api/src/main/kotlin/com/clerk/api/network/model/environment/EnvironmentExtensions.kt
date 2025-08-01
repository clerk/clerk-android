package com.clerk.api.network.model.environment

import com.clerk.api.sso.OAuthProvider

/**
 * Extension property that returns all enabled social OAuth providers for the environment.
 *
 * This property filters the configured social providers to return only those that are enabled for
 * the application. It converts the provider configurations to [OAuthProvider] instances that can be
 * used for authentication flows.
 *
 * @return List of enabled [OAuthProvider] instances configured for this environment
 */
internal val Environment.allSocialProviders: List<OAuthProvider>
  get() {
    val social = this.userSettings?.social.orEmpty()
    val enabledProviders = social.filter { it.value.enabled }
    return enabledProviders.map { OAuthProvider.fromStrategy(it.value.strategy) }
  }
