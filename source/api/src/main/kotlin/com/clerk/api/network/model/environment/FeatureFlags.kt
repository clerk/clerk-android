package com.clerk.api.network.model.environment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FeatureFlags(
  @SerialName("android_test_flag") val androidTestFlag: Boolean = false
)
