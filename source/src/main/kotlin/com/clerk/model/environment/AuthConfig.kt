package com.clerk.model.environment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthConfig(@SerialName("single_session_mode") val singleSessionMode: Boolean)
