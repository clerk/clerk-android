package com.clerk.sdk.model.environment

import kotlinx.serialization.Serializable

@Serializable data class AuthConfig(val singleSessionMode: Boolean)
