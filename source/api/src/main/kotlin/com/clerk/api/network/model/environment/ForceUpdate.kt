package com.clerk.api.network.model.environment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ForceUpdate(
  @SerialName("ios") val iosPolicies: List<IOSPolicy> = emptyList(),
  @SerialName("android") val androidPolicies: List<AndroidPolicy> = emptyList(),
) {
  @Serializable
  internal data class IOSPolicy(
    @SerialName("bundle_id") val bundleId: String,
    @SerialName("minimum_version") val minimumVersion: String,
    @SerialName("update_url") val updateUrl: String? = null,
  )

  @Serializable
  internal data class AndroidPolicy(
    @SerialName("package_name") val packageName: String,
    @SerialName("minimum_version") val minimumVersion: String,
    @SerialName("update_url") val updateUrl: String? = null,
  )
}
