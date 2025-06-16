package com.clerk.network.model.environment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AuthConfig(@SerialName("single_session_mode") val singleSessionMode: Boolean)
